import {useEffect, useMemo, useCallback} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {keepPreviousData, useQuery} from "@tanstack/react-query";
import {Filter, ArrowRight, Database} from "lucide-react";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {z} from "zod";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Axios} from '@/services/http/Axios.js';
import PaginationButton from '@/components/common/PaginationButton.jsx';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {
    FIRST_PAGE,
    redirectWhenInvalidPage,
    normalizeQuery,
    formatDateAndTime,
    getQueryString,
    GLOBAL_ERROR,
    handleErrors
} from '@/utils/index.js';
import {Button} from '@/components/ui/button.jsx';
import {TOAST_TYPE} from "@/utils/enums.js";
import InputError from "@/components/common/InputError";
import {notify} from '@/components/common/notification';

const fetchStocks = async ({queryKey}) => {
    const [, params] = queryKey

    const response = await Axios.get("/stocks", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            size: params.size,
            productName: params.productName || undefined,
            fromDate: params.fromDate || undefined,
            toDate: params.toDate || undefined,
        },
    })

    return response.data.data
}

const stockFilterSchema = z.object({
    productName: z.string().optional(),
    fromDate: z.string().optional(),
    toDate: z.string().optional(),
});

const Stocks = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    
    const filters = useMemo(() => ({
        ...normalizeQuery(queryParams, []),
        productName: queryParams.productName || "",
        fromDate: queryParams.fromDate || "",
        toDate: queryParams.toDate || "",
    }), [queryParams]);

    const {page, productName, fromDate, toDate} = filters;

    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: {errors},
    } = useForm({
        resolver: zodResolver(stockFilterSchema),
        defaultValues: {
            productName: "",
            fromDate: "",
            toDate: "",
        },
    });

    const {
        data,
        isPending: isPageLoading,
        isError,
        error
    } = useQuery({
        queryKey: ["stocks", filters],
        queryFn: fetchStocks,
        placeholderData: keepPreviousData,
    })

    const stocks = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams]);

    useEffect(() => {
        reset({
            productName,
            fromDate,
            toDate,
        });
    }, [productName, fromDate, toDate, reset]);

    const handleFilter = useCallback(
        (data) => {
            navigate(
                getQueryString({
                    ...queryParams,
                    productName: data.productName || undefined,
                    fromDate: data.fromDate || undefined,
                    toDate: data.toDate || undefined,
                    page: FIRST_PAGE,
                }),
                {replace: true}
            );
        },
        [navigate, queryParams]
    );

    useEffect(() => {
        if (isError) {
            console.error(error);
            handleErrors(error, setError);
            notify(TOAST_TYPE.ERROR, "Failed to load stocks")
        }
    }, [error, isError]);

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Stock Inventory</h1>
                        <p className="text-slate-500 font-medium">Manage and track your product stock levels and history</p>
                    </div>
                </div>

                <div className='grid lg:grid-cols-4 gap-10 items-start'>
                    {/* Filter Sidebar */}
                    <Card className='lg:col-span-1 border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden sticky top-24'>
                        <form onSubmit={handleSubmit(handleFilter)}>
                            <CardHeader className="bg-slate-100 border-b border-slate-100 pb-4">
                                <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="p-1.5 bg-blue-600 rounded-lg text-white">
                                        <Filter className="w-4 h-4" />
                                    </span>
                                    Filter Stocks
                                </CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-6 pt-6">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>
                                
                                {/* Product Name */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">Product Name</Label>
                                    <Input
                                        placeholder="Search by product..."
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        {...register("productName")}
                                    />
                                    <InputError errors={errors} field="productName"/>
                                </div>

                                {/* From Date */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">From Date</Label>
                                    <Input
                                        type="date"
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        {...register("fromDate")}
                                    />
                                    <InputError errors={errors} field="fromDate"/>
                                </div>

                                {/* To Date */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">To Date</Label>
                                    <Input
                                        type="date"
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        {...register("toDate")}
                                    />
                                    <InputError errors={errors} field="toDate"/>
                                </div>
                            </CardContent>

                            <CardFooter className="pt-2">
                                <Button 
                                    type="submit"
                                    className="w-full h-12 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-200 transition-all active:scale-95"
                                >
                                    Apply Filters
                                </Button>
                            </CardFooter>
                        </form>
                    </Card>

                    <div className='lg:col-span-3 space-y-6'>
                        <div className="bg-white rounded-lg border border-slate-100 shadow-sm overflow-hidden">
                            <Table className="bg-white">
                                <TableHeader>
                                    <TableRow className="bg-slate-100 border-b border-slate-100 hover:bg-slate-50/50 transition-none">
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Stock ID</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Total Cost</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Date & Time</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {stocks.length > 0 ?
                                        stocks.map((stock) => (
                                            <TableRow key={stock.id} className="group hover:bg-slate-50/50 border-b border-slate-50 transition-colors cursor-pointer" onClick={() => navigate(`/stocks/${stock.id}`)}>
                                                <TableCell className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-[10px] font-black text-blue-600">
                                                            #{stock.id}
                                                        </div>
                                                        <span className="font-bold text-slate-700">Stock Purchase</span>
                                                    </div>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="font-semibold text-slate-900">${stock.totalCost}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="text-xs font-semibold text-slate-500">{formatDateAndTime(stock.createdAt)}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-right">
                                                    <Button 
                                                        variant="ghost" 
                                                        size="sm" 
                                                        className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 font-bold text-xs gap-1"
                                                    >
                                                        Details <ArrowRight className="w-3 h-3" />
                                                    </Button>
                                                </TableCell>
                                            </TableRow>
                                        ))
                                        :
                                        <TableRow>
                                            <TableCell colSpan={4} className="py-20 text-center">
                                                <div className="flex flex-col items-center gap-2 opacity-40">
                                                    <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                        <Database className="w-6 h-6 text-slate-400" />
                                                    </div>
                                                    <p className="text-sm font-black uppercase tracking-widest">No stock entries found</p>
                                                    <p className="text-xs font-medium">Try adjusting your filters</p>
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    }
                                </TableBody>
                            </Table>
                        </div>

                        <PaginationButton totalPages={totalPages}/>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default Stocks
