import React from 'react';
import { 
    Table, 
    TableBody, 
    TableCell, 
    TableHead, 
    TableHeader, 
    TableRow 
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Eye, Pencil, Trash2 } from 'lucide-react';
import { formatDate } from '@/utils';

const BlogTable = ({ posts, handleEdit, deleteMutation }) => {
    return (
        <Table>
            <TableHeader>
                <TableRow className="bg-slate-50">
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Title</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Author</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Date</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-center">Status</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-right">Actions</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {posts.map((post) => (
                    <TableRow key={post.id} className="hover:bg-slate-50 transition-colors">
                        <TableCell className="px-6 py-4">
                            <div className="flex flex-col">
                                <span className="font-bold text-slate-900 line-clamp-1">{post.title}</span>
                            </div>
                        </TableCell>
                        <TableCell className="px-6 py-4">
                            <span className="font-medium text-slate-600">{post.author || 'Admin'}</span>
                        </TableCell>
                        <TableCell className="px-6 py-4">
                            <span className="text-xs font-bold text-slate-500">{formatDate(post.publishedAt || post.createdAt)}</span>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-center">
                            <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest ${post.status == 'PUBLISHED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                                {post.status}
                            </span>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-right">
                            <div className="flex justify-end gap-2">
                                <Button variant="outline" size="icon" className="h-9 w-9 text-slate-600 hover:bg-slate-50" onClick={() => window.open(`/blogs/${post.title}`, '_blank')}>
                                    <Eye className="w-4 h-4" />
                                </Button>
                                <Button variant="outline" size="icon" className="h-9 w-9 text-blue-600 hover:bg-blue-50" onClick={() => handleEdit(post)}>
                                    <Pencil className="w-4 h-4" />
                                </Button>
                                <Button variant="outline" size="icon" className="h-9 w-9 text-red-600 hover:bg-red-50" onClick={() => {
                                    if(confirm("Are you sure you want to delete this post?")) {
                                        deleteMutation.mutate(post.id);
                                    }
                                }}>
                                    <Trash2 className="w-4 h-4" />
                                </Button>
                            </div>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
};

export default BlogTable;
