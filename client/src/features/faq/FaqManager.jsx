import React, {useState} from 'react';
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import * as z from 'zod';
import {AuthenticatedAxios} from "@/services/http/Axios";
import {Button} from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import {toastify} from '@/common/toastify';
import {TOAST_TYPE} from "@/constants/app.constants";
import {HelpCircle, Plus} from 'lucide-react';
import {handleErrors} from "@/utils/ErrorUtils";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import FaqForm from './FaqForm';
import FaqTable from './FaqTable';

const faqSchema = z.object({
    question: z.string().min(1, "Question is required"),
    answer: z.string().min(1, "Answer is required"),
    displayOrder: z.coerce.number().default(0)
});

const getAllFaqs = async () => {
    const response = await AuthenticatedAxios.get("/faqs");
    return response.data.data;
};

const createFaq = async (data) => {
    const response = await AuthenticatedAxios.post("/faqs", data);
    return response.data.data;
};

const updateFaq = async (id, data) => {
    const response = await AuthenticatedAxios.put(`/faqs/${id}`, data);
    return response.data.data;
};

const deleteFaq = async (id) => {
    const response = await AuthenticatedAxios.delete(`/faqs/${id}`);
    return response.data;
};

const FaqManager = () => {
    const queryClient = useQueryClient();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingFaq, setEditingFaq] = useState(null);

    const {register, handleSubmit, reset, setError, formState: {errors}} = useForm({
        resolver: zodResolver(faqSchema),
        defaultValues: {
            question: '',
            answer: '',
            displayOrder: 0
        }
    });

    const {data: faqs = [], isLoading: isPageLoading} = useQuery({
        queryKey: ['faqs'],
        queryFn: getAllFaqs
    });

    const createMutation = useMutation({
        mutationFn: createFaq,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['faqs']});
            toastify(TOAST_TYPE.SUCCESS, "FAQ created successfully");
            setIsDialogOpen(false);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const updateMutation = useMutation({
        mutationFn: ({id, data}) => updateFaq(id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['faqs']});
            toastify(TOAST_TYPE.SUCCESS, "FAQ updated successfully");
            setIsDialogOpen(false);
            setEditingFaq(null);
            reset();
        },
        onError: (err) => handleErrors(err, setError)
    });

    const deleteMutation = useMutation({
        mutationFn: deleteFaq,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['faqs']});
            toastify(TOAST_TYPE.SUCCESS, "FAQ deleted successfully");
        }
    });

    React.useEffect(() => {
        if (editingFaq) {
            reset({
                question: editingFaq.question,
                answer: editingFaq.answer,
                displayOrder: editingFaq.displayOrder
            });
        } else {
            reset({
                question: '',
                answer: '',
                displayOrder: 0
            });
        }
    }, [editingFaq, reset]);

    const onSubmit = (data) => {
        if (editingFaq) {
            updateMutation.mutate({id: editingFaq.id, data});
        } else {
            createMutation.mutate(data);
        }
    };

    const handleEdit = (faq) => {
        setEditingFaq(faq);
        setIsDialogOpen(true);
    };

    return (
        <div className="container mx-auto py-10 px-4">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 uppercase tracking-tighter flex items-center gap-3">
                        <HelpCircle className="w-8 h-8 text-indigo-600"/>
                        FAQ Management
                    </h1>
                </div>

                <Dialog open={isDialogOpen} onOpenChange={(open) => {
                    setIsDialogOpen(open);
                    if (!open) {
                        setEditingFaq(null);
                    }
                }}>
                    <DialogTrigger asChild>
                        <Button
                            className="bg-indigo-600 hover:bg-indigo-700 font-bold px-6 py-6 rounded-xl shadow-lg shadow-indigo-200">
                            <Plus className="w-5 h-5 mr-2"/> Add New FAQ
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[600px]">
                        <DialogHeader>
                            <DialogTitle className="text-2xl font-bold uppercase tracking-tighter">
                                {editingFaq ? 'Edit FAQ' : 'Create New FAQ'}
                            </DialogTitle>
                        </DialogHeader>
                        <FaqForm
                            register={register}
                            errors={errors}
                            isPending={createMutation.isPending || updateMutation.isPending}
                            editingFaq={editingFaq}
                            onSubmit={handleSubmit(onSubmit)}
                        />
                    </DialogContent>
                </Dialog>
            </div>

            <div
                className="bg-white rounded-2xl shadow-xl shadow-slate-200 border border-slate-100 overflow-hidden relative">
                {!isPageLoading && faqs.length == 0 ? (
                    <div className="text-center py-10 font-bold text-slate-400">No FAQs found</div>
                ) : (
                    <FaqTable
                        faqs={faqs}
                        handleEdit={handleEdit}
                        deleteMutation={deleteMutation}
                    />
                )}
            </div>
        </div>
    );
};

export default FaqManager;
