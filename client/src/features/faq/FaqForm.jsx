import React from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { DialogFooter } from "@/components/ui/dialog";
import InputError from '@/components/common/InputError';
import FormField from '@/components/common/FormField';
import {GLOBAL_ERROR} from '@/utils/ErrorUtils';

const FaqForm = ({ register, errors, isPending, editingFaq, onSubmit }) => {
    return (
        <form onSubmit={onSubmit} className="space-y-6 pt-4">
            <InputError errors={errors} field={GLOBAL_ERROR}/>

            <FormField label="Question" field="question" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Input 
                    {...register('question')}
                    className={errors.question ? "border-red-500 font-bold" : "font-bold"}
                />
            </FormField>

            <FormField label="Answer" field="answer" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Textarea 
                    {...register('answer')}
                    className={errors.answer ? "border-red-500 font-medium h-32" : "font-medium h-32"}
                />
            </FormField>

            <FormField label="Display Order" field="displayOrder" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs" helperText="Lower numbers appear first">
                <Input 
                    type="number"
                    {...register('displayOrder')}
                    className="font-bold w-32"
                />
            </FormField>

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
