import React from 'react';
import { Controller } from 'react-hook-form';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { DialogFooter } from "@/components/ui/dialog";
import InputError from '@/components/common/InputError';
import FormField from '@/components/common/FormField';
import {GLOBAL_ERROR} from '@/utils/ErrorUtils';

const BannerForm = ({ register, control, errors, isPending, editingBanner, onSubmit }) => {
    return (
        <form onSubmit={onSubmit} className="space-y-6 pt-4">
            <InputError errors={errors} field={GLOBAL_ERROR}/>

            <FormField label="Banner Title" field="title" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Input 
                    {...register('title')}
                    className={errors.title ? "border-red-500 font-medium" : "font-medium"}
                />
            </FormField>
            <FormField label="Subtitle (Optional)" field="subtitle" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Input 
                    {...register('subtitle')}
                    className="font-medium"
                />
            </FormField>
            <FormField label="Image URL" field="imageUrl" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Input 
                    {...register('imageUrl')}
                    className={errors.imageUrl ? "border-red-500 font-medium" : "font-medium"}
                />
            </FormField>
            <FormField label="Link URL (Optional)" field="linkUrl" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                <Input 
                    {...register('linkUrl')}
                    className={errors.linkUrl ? "border-red-500 font-medium" : "font-medium"}
                />
            </FormField>
            <div className="grid grid-cols-2 gap-4">
                <FormField label="Display Order" field="displayOrder" errors={errors} labelClassName="font-bold uppercase tracking-widest text-xs">
                    <Input 
                        type="number"
                        {...register('displayOrder')}
                        className="font-medium"
                    />
                </FormField>
                <div className="flex items-center space-x-2 pt-8">
                    <Controller
                        name="active"
                        control={control}
                        render={({ field }) => (
                            <Switch 
                                checked={field.value}
                                onCheckedChange={field.onChange}
                            />
                        )}
                    />
                    <Label className="font-bold uppercase tracking-widest text-xs">Active</Label>
                    <InputError errors={errors} field="active" />
                </div>
            </div>
            <DialogFooter>
                <Button 
                    type="submit" 
                    className="w-full bg-blue-600 font-bold uppercase tracking-widest py-6"
                    disabled={isPending}
                >
                    {editingBanner ? 'Save Changes' : 'Create Banner'}
                </Button>
            </DialogFooter>
        </form>
    );
};

export default BannerForm;
