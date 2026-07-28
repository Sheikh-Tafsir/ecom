import React, {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import * as z from 'zod';
import {Axios} from "@/services/http/Axios";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {toastify} from '@/common/toastify';
import {TOAST_TYPE} from "@/constants/app.constants";
import {FileText, Plus, ArrowLeft, SortAsc} from 'lucide-react';
import {handleErrors} from "@/utils/ErrorUtils";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import BlogForm from './BlogForm';
import BlogTable from './BlogTable';

const blogSchema = z.object({
    title: z.string().min(1, "Title is required").max(255),
    content: z.string().min(1, "Content is required"),
    author: z.string().optional(),
    imageUrl: z.string().url("Must be a valid URL").or(z.string().length(0)).optional(),
    status: z.enum(['DRAFT', 'PUBLISHED']).default('DRAFT')
});

const getAllBlogs = async (params) => {
    const response = await Axios.get("/blogs", {params});
    return response.data.data;
};

const createBlog = async (data) => {
    const response = await Axios.post("/blogs", data);
    return response.data.data;
};

const updateBlog = async (id, data) => {
    const response = await Axios.put(`/blogs/${id}`, data);
    return response.data.data;
};

const deleteBlog = async (id) => {
    const response = await Axios.delete(`/blogs/${id}`);
    return response.data;
};

const BLOG_SORTBY = [
    {label: "Newest First", value: "createdAt,DESC"},
    {label: "Oldest First", value: "createdAt,ASC"},
    {label: "Title: A-Z", value: "title,ASC"},
    {label: "Title: Z-A", value: "title,DESC"},
];

const BlogManager = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingPost, setEditingPost] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [sortBy, setSortBy] = useState(BLOG_SORTBY[0].value);

    const {register, handleSubmit, reset, control, setError, formState: {errors}} = useForm({
        resolver: zodResolver(blogSchema),
        defaultValues: {
            title: '',
            content: '',
            author: '',
            imageUrl: '',
            status: 'DRAFT'
        }
    });

    const {data, isLoading: isPageLoading} = useQuery({
        queryKey: ['blogs', 'all', searchTerm, sortBy],
        queryFn: () => {
            const [sort, direction] = sortBy.split(',');
            return getAllBlogs({
                size: 100,
                search: searchTerm,
                sort: `${sort},${direction}`
            });
        }
    });

    const posts = data?.content || [];

    const createMutation = useMutation({
        mutationFn: createBlog,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['blogs']});
            toastify(TOAST_TYPE.SUCCESS, "Blog post created successfully");
            setIsDialogOpen(false);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const updateMutation = useMutation({
        mutationFn: ({id, data}) => updateBlog(id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['blogs']});
            toastify(TOAST_TYPE.SUCCESS, "Blog post updated successfully");
            setIsDialogOpen(false);
            setEditingPost(null);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const deleteMutation = useMutation({
        mutationFn: deleteBlog,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['blogs']});
            toastify(TOAST_TYPE.SUCCESS, "Blog post deleted successfully");
        }
    });

    useEffect(() => {
        if (editingPost) {
            reset({
                title: editingPost.title,
                content: editingPost.content,
                author: editingPost.author || '',
                imageUrl: editingPost.imageUrl || '',
                status: editingPost.status
            });
        } else {
            reset({
                title: '',
                content: '',
                author: '',
                imageUrl: '',
                status: 'DRAFT'
            });
        }
    }, [editingPost, reset]);

    const onSubmit = (data) => {
        if (editingPost) {
            updateMutation.mutate({id: editingPost.id, data});
        } else {
            createMutation.mutate(data);
        }
    };

    const handleEdit = (post) => {
        setEditingPost(post);
        setIsDialogOpen(true);
    };

    return (
        <div className="container mx-auto py-10 px-4">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 uppercase tracking-tighter flex items-center gap-3">
                        <FileText className="w-8 h-8 text-emerald-600"/>
                        Blog Management
                    </h1>
                </div>

                <div className="flex flex-col md:flex-row gap-4 w-full md:w-auto">
                    <Input
                        placeholder="Search blogs..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="max-w-sm font-bold"
                    />

                    <Select value={sortBy} onValueChange={setSortBy}>
                        <SelectTrigger className="w-full md:w-60 font-semibold">
                            <div className="flex items-center gap-2">
                                <SortAsc className="w-4 h-4"/>
                                <SelectValue placeholder="Sort By"/>
                            </div>
                        </SelectTrigger>
                        <SelectContent>
                            {BLOG_SORTBY.map((option) => (
                                <SelectItem key={option.value} value={option.value} className="font-medium">
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    <Dialog open={isDialogOpen} onOpenChange={(open) => {
                        setIsDialogOpen(open);
                        if (!open) {
                            setEditingPost(null);
                        }
                    }}>
                        <DialogTrigger asChild>
                            <Button
                                className="bg-blue-600 hover:bg-blue-700 font-bold px-6 py-6 rounded-xl shadow-lg shadow-blue-200">
                                <Plus className="w-5 h-5 mr-2"/> Write New Post
                            </Button>
                        </DialogTrigger>
                        <DialogContent
                            className="sm:max-w-[800px] lg:max-w-[1300px] max-w-[2000px] max-h-[96vh] overflow-y-auto">
                            <DialogHeader>
                                <DialogTitle className="text-2xl font-bold uppercase tracking-tighter">
                                    {editingPost ? 'Edit Blog Post' : 'Create New Blog Post'}
                                </DialogTitle>
                            </DialogHeader>
                            <BlogForm
                                register={register}
                                control={control}
                                errors={errors}
                                isPending={createMutation.isPending || updateMutation.isPending}
                                editingPost={editingPost}
                                onSubmit={handleSubmit(onSubmit)}
                            />
                        </DialogContent>
                    </Dialog>
                </div>
            </div>

            <div
                className="bg-white rounded-2xl shadow-xl shadow-slate-200 border border-slate-100 overflow-hidden relative">
                {!isPageLoading && posts.length == 0 ? (
                    <div className="text-center py-10 font-bold text-slate-400">No blog posts found</div>
                ) : (
                    <BlogTable
                        posts={posts}
                        handleEdit={handleEdit}
                        deleteMutation={deleteMutation}
                    />
                )}
            </div>
        </div>
    );
};

export default BlogManager;
