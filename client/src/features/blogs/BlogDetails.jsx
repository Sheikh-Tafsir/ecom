import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import DOMPurify from 'dompurify';
import { AuthenticatedAxios } from "@/services/http/Axios";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { ArrowLeft, User, Calendar } from "lucide-react";
import { formatDate } from '@/utils';
import NotFound from '@/features/NotFound';

const getBlogByTitle = async (title) => {
    const response = await AuthenticatedAxios.get(`/blogs/${title}`);
    return response.data.data;
};

const BlogDetails = () => {
    const { title } = useParams();
    const navigate = useNavigate();

    const { data: post, isLoading, isError } = useQuery({
        queryKey: ['blogs', title],
        queryFn: () => getBlogByTitle(title),
        retry: false
    });

    if (isLoading) {
        return (
            <div className="container mx-auto py-10 px-4 max-w-4xl">
                <Skeleton className="h-8 w-32 mb-8" />
                <Skeleton className="h-12 w-3/4 mb-4" />
                <Skeleton className="h-6 w-48 mb-10" />
                <Skeleton className="h-[400px] w-full mb-10 rounded-xl" />
                <div className="space-y-4">
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-5/6" />
                </div>
            </div>
        );
    }

    if (isError || !post) {
        return <NotFound />;
    }

    return (
        <article className="container mx-auto py-10 px-4 max-w-4xl">
            <Button 
                variant="ghost" 
                className="mb-8 font-bold text-slate-500 hover:text-blue-600 p-0 hover:bg-transparent flex items-center gap-2 group"
                onClick={() => navigate('/blogs')}
            >
                <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" /> 
                Back to News
            </Button>

            <header className="mb-10">
                <div className="flex items-center gap-3 mb-4">
                    <h1 className="text-4xl md:text-5xl font-bold text-slate-900 tracking-tighter leading-tight">
                        {post.title}
                    </h1>
                    {post.status === 'DRAFT' && (
                        <span className="bg-amber-100 text-amber-700 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest h-fit mt-2">
                            Draft Preview
                        </span>
                    )}
                </div>
                
                <div className="flex flex-wrap items-center gap-6 text-sm font-bold text-slate-400 uppercase tracking-widest">
                    <div className="flex items-center gap-2">
                        <User className="w-4 h-4 text-blue-600" />
                        {post.author || 'Admin'}
                    </div>
                    <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-blue-600" />
                        {formatDate(post.publishedAt || post.createdAt)}
                    </div>
                </div>
            </header>

            {post.imageUrl && (
                <div className="mb-10 rounded-2xl overflow-hidden shadow-2xl shadow-slate-200">
                    <img 
                        src={post.imageUrl} 
                        alt={post.title} 
                        className="w-full h-auto object-cover max-h-[500px]"
                    />
                </div>
            )}

            <div 
                className="prose prose-slate max-w-none 
                prose-headings:font-bold prose-headings:tracking-tight prose-a:text-blue-600 
                font-medium text-slate-700 leading-relaxed text-lg
                [&_ul]:list-disc [&_ul]:ml-6 [&_ul]:mb-4 [&_ul]:space-y-2
                [&_ol]:list-decimal [&_ol]:ml-6 [&_ol]:mb-4 [&_ol]:space-y-2
                [&_li]:pl-1
                [&_h2]:text-2xl [&_h2]:mt-8 [&_h2]:mb-4
                [&_h3]:text-xl [&_h3]:mt-6 [&_h3]:mb-3
                [&_p]:mb-4"
                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.content) }}
            />
        </article>
    );
};

export default BlogDetails;
