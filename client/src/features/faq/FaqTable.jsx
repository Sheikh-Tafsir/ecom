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

const FaqTable = ({ faqs, handleEdit, deleteMutation }) => {
    return (
        <Table>
            <TableHeader>
                <TableRow className="bg-slate-50">
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 w-16 text-center">Order</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6">Question</TableHead>
                    <TableHead className="font-bold uppercase tracking-widest text-xs py-4 px-6 text-right">Actions</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {faqs?.map((faq) => (
                    <TableRow key={faq.id} className="hover:bg-slate-50 transition-colors">
                        <TableCell className="px-6 py-4 text-center">
                            <span className="font-bold text-slate-400">{faq.displayOrder}</span>
                        </TableCell>
                        <TableCell className="px-6 py-4">
                            <span className="font-bold text-slate-900">{faq.question}</span>
                        </TableCell>
                        <TableCell className="px-6 py-4 text-right">
                            <div className="flex justify-end gap-2">
                                <Button variant="outline" size="icon" className="h-9 w-9 text-blue-600 hover:bg-blue-50" onClick={() => handleEdit(faq)}>
                                    <Pencil className="w-4 h-4" />
                                </Button>
                                <Button variant="outline" size="icon" className="h-9 w-9 text-red-600 hover:bg-red-50" onClick={() => {
                                    if(confirm("Are you sure you want to delete this FAQ?")) {
                                        deleteMutation.mutate(faq.id);
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

export default FaqTable;
