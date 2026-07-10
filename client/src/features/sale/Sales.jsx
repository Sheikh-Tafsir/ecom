import React, {useEffect, useMemo, useState, useCallback} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery, keepPreviousData} from "@tanstack/react-query";
import {Filter, TrendingUp, DollarSign, Calendar} from "lucide-react";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

import {Axios} from "@/services/http/Axios";
import PaginationButton from "@/components/common/PaginationButton";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";

import {formatDate} from "@/utils";
import {FIRST_PAGE, getQueryString, normalizeQuery, redirectWhenInvalidPage} from '@/utils/PaginationUtils';

import {Label} from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import InputError from "@/components/common/InputError";
import {APP_MODULE, TOAST_TYPE} from "@/utils/enums";
import {notify} from "@/components/common/notification";
import { cn } from "@/lib/utils";

import {ReportDialog} from "@/components/common/ReportDialog";

const fetchSales = async ({queryKey}) => {
    const [, params] = queryKey;

    const response = await Axios.get("/sales", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            productName: params.productName || undefined,
            fromDate: params.fromDate || undefined,
            toDate: params.toDate || undefined,
        },
    });

    return response.data.data;
};

export default function Sales() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, []),
            productName: queryParams.productName || "",
            fromDate: queryParams.fromDate || "",
            toDate: queryParams.toDate || "",
        }),
        [queryParams]
    );
    const {page, productName, fromDate, toDate} = filters;

    const [form, setForm] = useState({
        productName: "",
        fromDate: "",
        toDate: "",
    });

    const {
        data,
        isPending: isPageLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["sales", filters],
        queryFn: fetchSales,
        placeholderData: keepPreviousData,
    });

    const sales = data?.content ?? [];
    const totalPages = data?.totalPages ?? FIRST_PAGE;

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])


    useEffect(() => {
        setForm({
            productName,
            fromDate,
            toDate,
        });
    }, [productName, fromDate, toDate]);

    const handleChange = useCallback((e) => {
        const {name, value} = e.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    }, []);

    const handleFilter = useCallback(
        (e) => {
            e.preventDefault();

            navigate(
                getQueryString({
                    ...queryParams,
                    productName: form.productName || undefined,
                    fromDate: form.fromDate || undefined,
                    toDate: form.toDate || undefined,
                    page: FIRST_PAGE,
                }),
                {replace: true}
            );
        },
        [navigate, queryParams, form]
    );

    useEffect(() => {
        if (isError) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Failed to show sales")
        }
    }, [error, isError]);

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Sales Analytics</h1>
                        <p className="text-slate-500 font-medium">Monitor your sales performance and revenue trends</p>
                    </div>
                    <ReportDialog module={APP_MODULE.SALE} />
                </div>

                <div className="grid lg:grid-cols-4 gap-10 items-start">
                    {/* Filter Sidebar */}
                    <Card className='lg:col-span-1 border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden sticky top-24'>
                        <form onSubmit={handleFilter}>
                            <CardHeader className="bg-slate-100 border-b border-slate-100 pb-4">
                                <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="p-1.5 bg-blue-600 rounded-lg text-white">
                                        <Filter className="w-4 h-4" />
                                    </span>
                                    Filter Sales
                                </CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-6 pt-6">
                                {/* Product Name */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">Product Name</Label>
                                    <Input
                                        placeholder="Search by product..."
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        value={form.productName}
                                        name="productName"
                                        onChange={handleChange}
                                    />
                                    <InputError field="productName"/>
                                </div>

                                {/* From Date */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">From Date</Label>
                                    <Input
                                        type="date"
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        value={form.fromDate}
                                        name="fromDate"
                                        onChange={handleChange}
                                    />
                                    <InputError field="fromDate"/>
                                </div>

                                {/* To Date */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">To Date</Label>
                                    <Input
                                        type="date"
                                        className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        value={form.toDate}
                                        name="toDate"
                                        onChange={handleChange}
                                    />
                                    <InputError field="toDate"/>
                                </div>
                            </CardContent>

                            <CardFooter className="pt-2">
                                <Button className="w-full h-12 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-200 transition-all active:scale-95">
                                    Apply Filters
                                </Button>
                            </CardFooter>
                        </form>
                    </Card>

                    <div className="lg:col-span-3 space-y-6">
                        <div className="bg-white rounded-lg border border-slate-100 shadow-sm overflow-hidden">
                            <Table className="bg-white">
                                <TableHeader>
                                    <TableRow className="bg-slate-100 border-b border-slate-100 hover:bg-slate-50/50 transition-none">
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Product</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Quantity</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Profit</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-right">Date</TableHead>
                                    </TableRow>
                                </TableHeader>

                                <TableBody>
                                    {sales.length > 0 ?
                                        sales.map((item) => (
                                            <TableRow key={item.id} className="group hover:bg-slate-50/50 border-b border-slate-50 transition-colors">
                                                <TableCell className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                                                            <TrendingUp className="w-4 h-4 text-blue-600" />
                                                        </div>
                                                        <span className="font-bold text-slate-700">{item.productName}</span>
                                                    </div>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-widest bg-slate-100 text-slate-600">
                                                        {item.quantity} units
                                                    </span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="font-black text-emerald-600">${item.profit}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-right">
                                                    <span className="text-xs font-semibold text-slate-500">{formatDate(item.createdAt)}</span>
                                                </TableCell>
                                            </TableRow>
                                        ))
                                        :
                                        <TableRow>
                                            <TableCell colSpan={4} className="py-20 text-center">
                                                <div className="flex flex-col items-center gap-2 opacity-40">
                                                    <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                        <DollarSign className="w-6 h-6 text-slate-400" />
                                                    </div>
                                                    <p className="text-sm font-black uppercase tracking-widest">No sales records found</p>
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
    );
}
