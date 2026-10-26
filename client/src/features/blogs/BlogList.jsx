import React, {useEffect, useMemo} from 'react';
import DOMPurify from 'dompurify';
import {useForm, Controller} from 'react-hook-form';
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import PaginationButton from '@/components/common/PaginationButton';
import {formatDate} from '@/utils/DateUtils';
import {FIRST_PAGE} from '@/utils/PaginationUtils';
import {Search, SortAsc} from 'lucide-react';
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import SEO from "@/common/SEO";
import {queryKeys} from "@/services/reactQuery/queryKeys";
import {useListPage} from "@/hooks/useListPage";
import {createListFetcher} from "@/services/api";

const getPublishedBlogs = createListFetcher("/blogs/published", {
    mapParams: (p) => ({
        search: p.search || undefined,
    }),
});

const ALLOWED_SORT_FIELDS = new Set(["createdAt", "title"]);

const BLOG_SORTBY = [
    {label: "Newest First", value: "createdAt,DESC"},
    {label: "Oldest First", value: "createdAt,ASC"},
    {label: "Title: A-Z", value: "title,ASC"},
    {label: "Title: Z-A", value: "title,DESC"},
];

const BlogList = () => {
    const {
        items: posts,
        totalPages,
        filters,
        query: {isPending: isLoading},
        updateQuery,
        navigate,
    } = useListPage({
        queryKey: (f) => queryKeys.blogs.list(f),
        queryFn: getPublishedBlogs,
        allowedSortFields: ALLOWED_SORT_FIELDS,
        extraFilters: (qp) => ({
            search: qp.search || "",
        }),
        errorMessage: "Failed to load articles",
    });

    const {search, sort} = filters;

    const {control, watch, reset} = useForm({
        defaultValues: {
            search: "",
            sort: BLOG_SORTBY[0].value,
        },
    });

    const watchedSearch = watch("search");

    // Sync form ← URL
    useEffect(() => {
        reset({search, sort});
    }, [search, sort, reset]);

    // Debounced search → URL
    useEffect(() => {
        const trimmed = watchedSearch.trim();
        if (trimmed == search) return;

        const timer = setTimeout(() => {
            updateQuery({
                search: trimmed || undefined,
                page: FIRST_PAGE,
            });
        }, 400);

        return () => clearTimeout(timer);
    }, [watchedSearch, search, updateQuery]);

    return (
        <div className="container mx-auto py-10 px-4 relative min-h-[600px]">
            <SEO
                title="News & Articles | Aspire Blog"
                description="Read our latest stories, shopping guides, announcements, and product insights from the Aspire team."
                canonicalUrl="/blogs"
                ogType="website"
            />
            {isLoading && !posts.length && <PageLoadingOverlay />}
            
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
                <h1 className="text-4xl font-bold text-slate-900 tracking-tighter uppercase">
                    Latest News & Articles
                </h1>
                
                <div className="flex flex-col sm:flex-row gap-4 w-full md:w-auto">
                    <div className="relative w-full sm:w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <Controller
                            name="search"
                            control={control}
                            render={({field}) => (
                                <Input
                                    placeholder="Search articles..."
                                    className="pl-10 font-bold"
                                    {...field}
                                />
                            )}
                        />
                    </div>

                    <Controller
                        name="sort"
                        control={control}
                        render={({field}) => (
                            <Select
                                value={field.value}
                                onValueChange={(val) => {
                                    field.onChange(val);
                                    updateQuery({sort: val});
                                }}
                            >
                                <SelectTrigger className="w-full sm:w-48 font-bold">
                                    <div className="flex items-center gap-2">
                                        <SortAsc className="w-4 h-4" />
                                        <SelectValue placeholder="Sort By" />
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
                        )}
                    />
                </div>
            </div>

            {!isLoading && posts.length == 0 ? (
                <div className="text-center py-20 bg-slate-50 rounded-xl border-2 border-dashed border-slate-200">
                    <p className="text-slate-500 font-bold">No articles found{search ? ` for "${search}"` : ''}.</p>
                </div>
            ) : (
                <>
                    <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 mb-10">
                        {posts.map((post) => (
                            <Card key={post.id} className="overflow-hidden border-slate-100 shadow-xl hover:shadow-2xl transition-all group flex flex-col">
                                {post.imageUrl && (
                                    <div className="h-48 overflow-hidden">
                                        <img 
                                            src={post.imageUrl} 
                                            alt={post.title} 
                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                        />
                                    </div>
                                )}
                                <CardHeader className="flex-none">
                                    <div className="text-xs font-bold text-blue-600 uppercase tracking-widest mb-2">
                                        {formatDate(post.publishedAt || post.createdAt)}
                                    </div>
                                    <CardTitle className="text-xl font-bold group-hover:text-blue-600 transition-colors line-clamp-2 leading-tight">
                                        {post.title}
                                    </CardTitle>
                                </CardHeader>
                                <CardContent className="flex-grow">
                                    <p className="text-slate-600 line-clamp-3 font-medium text-sm leading-relaxed">
                                        {DOMPurify.sanitize(post.content, { ALLOWED_TAGS: [] })}
                                    </p>
                                </CardContent>
                                <CardFooter className="mt-auto pt-0">
                                    <Button 
                                        variant="link" 
                                        className="p-0 font-bold text-blue-600 uppercase tracking-widest text-xs hover:no-underline flex items-center gap-2 group/btn"
                                        onClick={() => navigate(`/blogs/${post.title}`)}
                                    >
                                        Read More 
                                        <span className="group-hover/btn:translate-x-1 transition-transform">→</span>
                                    </Button>
                                </CardFooter>
                            </Card>
                        ))}
                    </div>
                    <PaginationButton totalPages={totalPages} />
                </>
            )}
        </div>
    );
};

export default BlogList;
