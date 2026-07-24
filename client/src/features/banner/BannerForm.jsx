import React from 'react';
import { Controller } from 'react-hook-form';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { DialogFooter } from "@/components/ui/dialog";
import InputError from '@/components/common/InputError';

const BannerForm = ({ register, control, errors, isPending, editingBanner, onSubmit }) => {
    return (
        <form onSubmit={onSubmit} className="space-y-6 pt-4">
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Banner Title</Label>
                <Input 
                    {...register('title')}
                    className={errors.title ? "border-red-500 font-medium" : "font-medium"}
                />
                <InputError errors={errors} field="title" />
            </div>
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Subtitle (Optional)</Label>
                <Input 
                    {...register('subtitle')}
                    className="font-medium"
                />
                <InputError errors={errors} field="subtitle" />
            </div>
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Image URL</Label>
                <Input 
                    {...register('imageUrl')}
                    className={errors.imageUrl ? "border-red-500 font-medium" : "font-medium"}
                />
                <InputError errors={errors} field="imageUrl" />
            </div>
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Link URL (Optional)</Label>
                <Input 
                    {...register('linkUrl')}
                    className={errors.linkUrl ? "border-red-500 font-medium" : "font-medium"}
                />
                <InputError errors={errors} field="linkUrl" />
            </div>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                    <Label className="font-bold uppercase tracking-widest text-xs">Display Order</Label>
                    <Input 
                        type="number"
                        {...register('displayOrder')}
                        className="font-medium"
                    />
                    <InputError errors={errors} field="displayOrder" />
                </div>
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
