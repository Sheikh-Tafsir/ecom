import React from 'react';
import { Controller } from 'react-hook-form';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { 
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { DialogFooter } from "@/components/ui/dialog";
import InputError from '@/components/common/InputError';

const modules = {
    toolbar: [
        [{ 'header': [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike', 'blockquote'],
        [{ 'list': 'ordered' }, { 'list': 'bullet' }, { 'indent': '-1' }, { 'indent': '+1' }],
        ['link', 'clean']
    ],
};

const BlogForm = ({ register, control, errors, isPending, editingPost, onSubmit }) => {
    return (
        <form onSubmit={onSubmit} className="space-y-6 pt-4">
            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs">Post Title</Label>
                <Input 
                    {...register('title')}
                    className={errors.title ? "border-red-500 font-bold" : "font-bold"}
                />
                <InputError errors={errors} field="title" />
            </div>

            <div className="space-y-2">
                <Label className="font-bold uppercase tracking-widest text-xs mb-2 block">Content (Visual Editor)</Label>
                <div className="rounded-xl overflow-hidden border border-slate-200">
                    <Controller
                        name="content"
                        control={control}
                        render={({ field }) => (
                            <ReactQuill 
                                theme="snow" 
                                value={field.value} 
                                onChange={field.onChange}
                                modules={modules}
                                className="bg-white"
                                placeholder="Write your article here..."
                                style={{ height: '350px', marginBottom: '45px' }}
                            />
                        )}
                    />
                </div>
                <InputError errors={errors} field="content" />
            </div>

            <div className="grid md:grid-cols-3 gap-6">
                <div className="space-y-2">
                    <Label className="font-bold uppercase tracking-widest text-xs">Author</Label>
                    <Input 
                        {...register('author')}
                        className="font-medium"
                    />
                    <InputError errors={errors} field="author" />
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
                    <Label className="font-bold uppercase tracking-widest text-xs">Status</Label>
                    <Controller
                        name="status"
                        control={control}
                        render={({ field }) => (
                            <Select onValueChange={field.onChange} value={field.value}>
                                <SelectTrigger className="font-bold">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="DRAFT">DRAFT</SelectItem>
                                    <SelectItem value="PUBLISHED">PUBLISHED</SelectItem>
                                </SelectContent>
                            </Select>
                        )}
                    />
                    <InputError errors={errors} field="status" />
                </div>
            </div>

            <DialogFooter>
                <Button 
                    type="submit" 
                    className="w-full bg-blue-600 font-bold uppercase tracking-widest py-6"
                    disabled={isPending}
                >
                    {editingPost ? 'Update Post' : 'Publish Post'}
                </Button>
            </DialogFooter>
        </form>
    );
};

export default BlogForm;
