import React from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DialogFooter } from "@/components/ui/dialog";
import InputError from '@/components/common/InputError';

const FaqForm = ({ register, errors, isPending, editingFaq, onSubmit }) => {
    return (
        <form onSubmit={onSubmit} className="space-y-6 pt-4">
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Question</Label>
                <Input 
                    {...register('question')}
                    className={errors.question ? "border-red-500 font-bold" : "font-bold"}
                />
                <InputError errors={errors} field="question" />
            </div>

            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Answer</Label>
                <Textarea 
                    {...register('answer')}
                    className={errors.answer ? "border-red-500 font-medium h-32" : "font-medium h-32"}
                />
                <InputError errors={errors} field="answer" />
            </div>

            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Display Order</Label>
                <Input 
                    type="number"
                    {...register('displayOrder')}
                    className="font-bold w-32"
                />
                <p className="text-[10px] text-slate-400 font-bold uppercase">Lower numbers appear first</p>
                <InputError errors={errors} field="displayOrder" />
            </div>

            <DialogFooter>
                <Button 
                    type="submit" 
                    className="w-full bg-indigo-600 font-bold uppercase tracking-widest py-6"
                    disabled={isPending}
                >
                    {editingFaq ? 'Update FAQ' : 'Create FAQ'}
                </Button>
            </DialogFooter>
        </form>
    );
};

export default FaqForm;
