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
import { Pencil, Trash2 } from 'lucide-react';

const BannerTable = ({ banners, handleEdit, deleteMutation }) => {
    return (
        <Table>
            <TableHeader>
                <TableRow className="bg-slate-50">
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Preview</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Title & Subtitle</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-center">Order</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-center">Status</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-right">Actions</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {banners.map((banner) => (
                    <TableRow key={banner.id} className="hover:bg-slate-50 transition-colors">
                        <TableCell className="px-6 py-4">
                            <div className="w-24 h-12 rounded-lg overflow-hidden border border-slate-100">
                                <img src={banner.imageUrl} alt="" className="w-full h-full object-cover" />
                            </div>
                        </TableCell>
                        <TableCell className="px-6 py-4">
                            <div className="flex flex-col">
                                <span className="font-bold text-slate-900">{banner.title}</span>
                                <span className="text-xs text-slate-500 line-clamp-1">{banner.subtitle}</span>
                            </div>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-center">
                            <span className="font-bold text-slate-400">{banner.displayOrder}</span>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-center">
                            <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest ${banner.active ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                                {banner.active ? 'Active' : 'Inactive'}
                            </span>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-right">
                            <div className="flex justify-end gap-2">
                                <Button variant="outline" size="icon" className="h-9 w-9 text-blue-600 hover:bg-blue-50" onClick={() => handleEdit(banner)}>
                                    <Pencil className="w-4 h-4" />
                                </Button>
                                <Button variant="outline" size="icon" className="h-9 w-9 text-red-600 hover:bg-red-50" onClick={() => {
                                    if(confirm("Are you sure you want to delete this banner?")) {
                                        deleteMutation.mutate(banner.id);
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

export default BannerTable;
