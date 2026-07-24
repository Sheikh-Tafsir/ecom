import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Axios } from "@/services/http/Axios";
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion";
import { Skeleton } from "@/components/ui/skeleton";

const getAllFaqs = async () => {
    const response = await Axios.get("/faqs");
    return response.data.data;
};

const Faqs = () => {
    const { data: faqs, isLoading } = useQuery({
        queryKey: ['faqs'],
        queryFn: getAllFaqs
    });

    if (isLoading) {
        return (
            <div className="mx-auto py-10 px-4">
                <Skeleton className="h-10 w-48 mb-8" />
                <div className="space-y-4">
                    {[1, 2, 3, 4, 5].map((i) => (
                        <Skeleton key={i} className="h-16 w-full" />
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto py-10 px-4 max-w-4xl">
            <h1 className="text-4xl font-bold text-slate-900 mb-10 text-center uppercase tracking-tighter">
                Frequently Asked Questions
            </h1>

            {faqs?.length === 0 ? (
                <div className="text-center py-20 bg-slate-50 rounded-xl border-2 border-dashed border-slate-200">
                    <p className="text-slate-500 font-bold">No FAQs found.</p>
                </div>
            ) : (
                <div className="space-y-4">
                    <Accordion type="single" collapsible className="w-full">
                        {faqs?.map((faq) => (
                            <AccordionItem key={faq.id} value={`faq-${faq.id}`} className="bg-white border rounded-xl mb-4 px-6 shadow-sm overflow-hidden">
                                <AccordionTrigger className="text-left font-bold text-slate-700 hover:text-blue-600 hover:no-underline py-5">
                                    {faq.question}
                                </AccordionTrigger>
                                <AccordionContent className="text-slate-600 leading-relaxed font-medium pb-5">
                                    {faq.answer}
                                </AccordionContent>
                            </AccordionItem>
                        ))}
                    </Accordion>
                </div>
            )}
        </div>
    );
};

export default Faqs;
