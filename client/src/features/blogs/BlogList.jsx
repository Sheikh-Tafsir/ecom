import React, { useMemo, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { AuthenticatedAxios } from "@/services/http/Axios";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import { 
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import PaginationButton from '@/components/common/PaginationButton';
import { formatDate } from '@/utils';
import { FIRST_PAGE } from '@/utils/PaginationUtils';
import { Search, SortAsc } from 'lucide-react';
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";

const getPublishedBlogs = async (params) => {
    const response = await AuthenticatedAxios.get("/blogs/published", { params });
    return response.data.data;
};

const BLOG_SORTBY = [
    { label: "Newest First", value: "createdAt,DESC" },
    { label: "Oldest First", value: "createdAt,ASC" },
    { label: "Title: A-Z", value: "title,ASC" },
    { label: "Title: Z-A", value: "title,DESC" },
];

const BlogList = () => {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const page = parseInt(searchParams.get('page') || FIRST_PAGE);
    const [searchTerm, setSearchTerm] = useState('');
    const [sortBy, setSortBy] = useState(BLOG_SORTBY[0].value);

    const { data, isLoading } = useQuery({
        queryKey: ['blogs', 'published', { page, searchTerm, sortBy }],
        queryFn: () => {
            const [sort, direction] = sortBy.split(',');
            return getPublishedBlogs({ 
                page: page - 1, 
                search: searchTerm,
                sort: `${sort},${direction}`
            });
        },
        placeholderData: keepPreviousData
    });

    const posts = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    const handleSortChange = (value) => {
        setSortBy(value);
        // Reset to first page on sort change
        searchParams.set('page', FIRST_PAGE);
        setSearchParams(searchParams);
    };

    return (
        <div className="container mx-auto py-10 px-4 relative min-h-[600px]">
            {isLoading && !posts.length && <PageLoadingOverlay />}
            
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
                <h1 className="text-4xl font-bold text-slate-900 tracking-tighter uppercase">
                    Latest News & Articles
                </h1>
                
                <div className="flex flex-col sm:flex-row gap-4 w-full md:w-auto">
                    <div className="relative w-full sm:w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <Input 
                            placeholder="Search articles..." 
                            className="pl-10 font-bold"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <Select value={sortBy} onValueChange={handleSortChange}>
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
                </div>
            </div>

            {!isLoading && posts.length == 0 ? (
                <div className="text-center py-20 bg-slate-50 rounded-xl border-2 border-dashed border-slate-200">
                    <p className="text-slate-500 font-bold">No articles found{searchTerm ? ` for "${searchTerm}"` : ''}.</p>
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
                                        {post.content.replace(/<[^>]*>/g, '')}
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
