import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Axios } from "@/services/http/Axios";
import { Button } from "@/components/ui/button";
import { 
    Dialog, 
    DialogContent, 
    DialogHeader, 
    DialogTitle, 
    DialogTrigger,
} from "@/components/ui/dialog";
import { toastify } from '@/common/toastify';
import { TOAST_TYPE } from '@/utils/enums';
import { Image as ImageIcon, Plus } from 'lucide-react';
import { handleErrors } from "@/utils/ErrorUtils";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import BannerForm from './BannerForm';
import BannerTable from './BannerTable';

const bannerSchema = z.object({
    title: z.string().min(1, "Title is required").max(255),
    subtitle: z.string().optional(),
    imageUrl: z.string().url("Must be a valid URL"),
    linkUrl: z.string().url("Must be a valid URL").or(z.string().length(0)).optional(),
    displayOrder: z.coerce.number().int().nonnegative().default(0),
    active: z.boolean().default(true)
});

const getAllBanners = async () => {
    const response = await Axios.get("/banners");
    return response.data.data;
};

const createBanner = async (data) => {
    const response = await Axios.post("/banners", data);
    return response.data.data;
};

const updateBanner = async (id, data) => {
    const response = await Axios.put(`/banners/${id}`, data);
    return response.data.data;
};

const deleteBanner = async (id) => {
    const response = await Axios.delete(`/banners/${id}`);
    return response.data;
};

const BannerManager = () => {
    const queryClient = useQueryClient();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingBanner, setEditingBanner] = useState(null);

    const { register, handleSubmit, reset, setError, control, formState: { errors } } = useForm({
        resolver: zodResolver(bannerSchema),
        defaultValues: {
            title: '',
            subtitle: '',
            imageUrl: '',
            linkUrl: '',
            displayOrder: 0,
            active: true
        }
    });

    const { data: banners, isLoading: isPageLoading } = useQuery({
        queryKey: ['banners'],
        queryFn: getAllBanners
    });

    const createMutation = useMutation({
        mutationFn: createBanner,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['banners'] });
            toastify(TOAST_TYPE.SUCCESS, "Banner created successfully");
            setIsDialogOpen(false);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, data }) => updateBanner(id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['banners'] });
            toastify(TOAST_TYPE.SUCCESS, "Banner updated successfully");
            setIsDialogOpen(false);
            setEditingBanner(null);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const deleteMutation = useMutation({
        mutationFn: deleteBanner,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['banners'] });
            toastify(TOAST_TYPE.SUCCESS, "Banner deleted successfully");
        }
    });

    useEffect(() => {
        if (editingBanner) {
            reset({
                title: editingBanner.title,
                subtitle: editingBanner.subtitle || '',
                imageUrl: editingBanner.imageUrl,
                linkUrl: editingBanner.linkUrl || '',
                displayOrder: editingBanner.displayOrder,
                active: editingBanner.active
            });
        } else {
            reset({
                title: '',
                subtitle: '',
                imageUrl: '',
                linkUrl: '',
                displayOrder: 0,
                active: true
            });
        }
    }, [editingBanner, reset]);

    const onSubmit = (data) => {
        if (editingBanner) {
            updateMutation.mutate({ id: editingBanner.id, data });
        } else {
            createMutation.mutate(data);
        }
    };

    const handleEdit = (banner) => {
        setEditingBanner(banner);
        setIsDialogOpen(true);
    };

    return (
        <div className="container mx-auto py-10 px-4">
            {isPageLoading && <PageLoadingOverlay />}

            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 uppercase tracking-tighter flex items-center gap-3">
                        <ImageIcon className="w-8 h-8 text-blue-600" />
                        Banner Management
                    </h1>
                </div>

                <Dialog open={isDialogOpen} onOpenChange={(open) => {
                    setIsDialogOpen(open);
                    if (!open) {
                        setEditingBanner(null);
                    }
                }}>
                    <DialogTrigger asChild>
                        <Button className="bg-blue-600 hover:bg-blue-700 font-bold px-6 py-6 rounded-xl shadow-lg shadow-blue-200">
                            <Plus className="w-5 h-5 mr-2" /> Add New Banner
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[500px]">
                        <DialogHeader>
                            <DialogTitle className="text-2xl font-bold uppercase tracking-tighter">
                                {editingBanner ? 'Edit Banner' : 'Create New Banner'}
                            </DialogTitle>
                        </DialogHeader>
                        <BannerForm 
                            register={register}
                            control={control}
                            errors={errors}
                            isPending={createMutation.isPending || updateMutation.isPending}
                            editingBanner={editingBanner}
                            onSubmit={handleSubmit(onSubmit)}
                        />
                    </DialogContent>
                </Dialog>
            </div>

            <div className="bg-white rounded-2xl shadow-xl shadow-slate-200 border border-slate-100 overflow-hidden relative">
                {!isPageLoading && banners?.length == 0 ? (
                    <div className="text-center py-10 font-bold text-slate-400">No banners found</div>
                ) : (
                    <BannerTable 
                        banners={banners || []}
                        handleEdit={handleEdit}
                        deleteMutation={deleteMutation}
                    />
                )}
            </div>
        </div>
    );
};

export default BannerManager;
