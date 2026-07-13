import React, {useEffect, useMemo, useCallback} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery, keepPreviousData} from "@tanstack/react-query";
import {Filter, TrendingUp, DollarSign, Package, BarChart3, PieChart} from "lucide-react";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {z} from "zod";
import {Bar, BarChart, CartesianGrid, XAxis, YAxis, ResponsiveContainer, Cell} from "recharts";

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

import {formatDate, GLOBAL_ERROR, handleErrors} from "@/utils";
import {FIRST_PAGE, getQueryString, normalizeQuery, redirectWhenInvalidPage} from '@/utils/PaginationUtils';

import {Label} from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
    CardDescription
} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import InputError from "@/components/common/InputError";
import {APP_MODULE, TOAST_TYPE} from "@/utils/enums";
import {notify} from "@/components/common/notification";
import { cn } from "@/lib/utils";
import {
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
} from "@/components/ui/chart";

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

const saleFilterSchema = z.object({
    productName: z.string().optional(),
    fromDate: z.string().optional(),
    toDate: z.string().optional(),
});

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

    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: {errors},
    } = useForm({
        resolver: zodResolver(saleFilterSchema),
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
            notify(TOAST_TYPE.ERROR, "Failed to show sales")
        }
    }, [error, isError]);

    // Dashboard Calculations
    const stats = useMemo(() => {
        if (!sales.length) return null;
        
        const totalProfit = sales.reduce((sum, item) => sum + (item.profit || 0), 0);
        const totalUnits = sales.reduce((sum, item) => sum + (item.quantity || 0), 0);
        const avgProfit = totalProfit / sales.length;
        
        // Group by product for chart
        const productStats = sales.reduce((acc, item) => {
            if (!acc[item.productName]) {
                acc[item.productName] = { name: item.productName, profit: 0, units: 0 };
            }
            acc[item.productName].profit += item.profit || 0;
            acc[item.productName].units += item.quantity || 0;
            return acc;
        }, {});

        const chartData = Object.values(productStats)
            .sort((a, b) => b.profit - a.profit)
            .slice(0, 8); // Top 8 products

        return {
            totalProfit: totalProfit.toFixed(2),
            totalUnits,
            avgProfit: avgProfit.toFixed(2),
            saleCount: sales.length,
            chartData
        };
    }, [sales]);

    const chartConfig = {
        profit: {
            label: "Profit",
            color: "hsl(var(--chart-1))",
        },
    };

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                {/* Header */}
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Sales Analytics</h1>
                        <p className="text-slate-500 font-medium italic">Empower your business with data-driven insights</p>
                    </div>
                    <ReportDialog module={APP_MODULE.SALE} />
                </div>

                {/* Dashboard Stats */}
                {stats && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10 animate-in fade-in slide-in-from-top duration-700">
                        <Card className="border-none shadow-xl shadow-blue-100/50 bg-gradient-to-br from-blue-600 to-indigo-700 text-white rounded-[1rem] overflow-hidden group">
                            <CardContent className="p-8 relative">
                                <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:scale-110 transition-transform">
                                    <DollarSign className="w-20 h-20" />
                                </div>
                                <p className="text-blue-100 font-black uppercase tracking-widest text-xs mb-2">Total Profit</p>
                                <h3 className="text-4xl font-bold tracking-tighter mb-1">TK{stats.totalProfit}</h3>
                                <p className="text-blue-200 text-xs font-bold flex items-center gap-1">
                                    <TrendingUp className="w-3 h-3" /> Based on current page
                                </p>
                            </CardContent>
                        </Card>

                        <Card className="border-none shadow-xl shadow-emerald-100/50 bg-white rounded-[1rem] overflow-hidden group">
                            <CardContent className="p-8 relative">
                                <div className="absolute top-0 right-0 p-6 text-emerald-100 group-hover:scale-110 transition-transform">
                                    <Package className="w-20 h-20" />
                                </div>
                                <p className="text-slate-400 font-black uppercase tracking-widest text-xs mb-2">Units Sold</p>
                                <h3 className="text-4xl font-bold text-slate-900 tracking-tighter mb-1">{stats.totalUnits}</h3>
                                <p className="text-emerald-600 text-xs font-bold uppercase tracking-widest">Items dispatched</p>
                            </CardContent>
                        </Card>

                        <Card className="border-none shadow-xl shadow-amber-100/50 bg-white rounded-[1rem] overflow-hidden group">
                            <CardContent className="p-8 relative">
                                <div className="absolute top-0 right-0 p-6 text-amber-100 group-hover:scale-110 transition-transform">
                                    <BarChart3 className="w-20 h-20" />
                                </div>
                                <p className="text-slate-400 font-black uppercase tracking-widest text-xs mb-2">Avg. Profit</p>
                                <h3 className="text-4xl font-bold text-slate-900 tracking-tighter mb-1">TK{stats.avgProfit}</h3>
                                <p className="text-amber-600 text-xs font-bold uppercase tracking-widest">Per sale average</p>
                            </CardContent>
                        </Card>

                        <Card className="border-none shadow-xl shadow-rose-100/50 bg-white rounded-[1rem] overflow-hidden group">
                            <CardContent className="p-8 relative">
                                <div className="absolute top-0 right-0 p-6 text-rose-100 group-hover:scale-110 transition-transform">
                                    <PieChart className="w-20 h-20" />
                                </div>
                                <p className="text-slate-400 font-black uppercase tracking-widest text-xs mb-2">Sale Transactions</p>
                                <h3 className="text-4xl font-black text-slate-900 tracking-tighter mb-1">{stats.saleCount}</h3>
                                <p className="text-rose-600 text-xs font-bold uppercase tracking-widest">Entries processed</p>
                            </CardContent>
                        </Card>
                    </div>
                )}

                {/* Main Content Grid */}
                <div className="grid lg:grid-cols-4 gap-10 items-start">
                    {/* Filter Sidebar */}
                    <Card className='lg:col-span-1 border-slate-100 shadow-xl shadow-slate-200/50 rounded-[1rem] overflow-hidden sticky top-24 animate-in slide-in-from-left duration-700'>
                        <form onSubmit={handleSubmit(handleFilter)}>
                            <CardHeader className="bg-slate-50 border-b border-slate-100 pb-4 pt-6 px-8">
                                <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-3">
                                    <div className="p-2 bg-blue-600 rounded-xl text-white shadow-lg shadow-blue-200">
                                        <Filter className="w-4 h-4" />
                                    </div>
                                    Narrow Results
                                </CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-6 pt-8 px-8 pb-8">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>
                                
                                <div className="space-y-2">
                                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 ml-1">Product Name</Label>
                                    <Input
                                        placeholder="Search by product..."
                                        className="h-12 rounded-md border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-bold placeholder:font-medium"
                                        {...register("productName")}
                                    />
                                    <InputError errors={errors} field="productName"/>
                                </div>

                                <div className="space-y-2">
                                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 ml-1">From Date</Label>
                                    <Input
                                        type="date"
                                        className="h-12 rounded-md border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-bold"
                                        {...register("fromDate")}
                                    />
                                    <InputError errors={errors} field="fromDate"/>
                                </div>

                                <div className="space-y-2">
                                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 ml-1">To Date</Label>
                                    <Input
                                        type="date"
                                        className="h-12 rounded-md border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-bold"
                                        {...register("toDate")}
                                    />
                                    <InputError errors={errors} field="toDate"/>
                                </div>
                            </CardContent>

                            <CardFooter className="px-8 pb-8 pt-0">
                                <Button 
                                    type="submit"
                                    className="w-full h-14 rounded-md bg-blue-600 hover:bg-blue-700 text-white font-bold text-base shadow-xl shadow-blue-200 transition-all active:scale-95 group"
                                >
                                    Apply Filters
                                    <TrendingUp className="w-4 h-4 ml-2 group-hover:translate-y-[-2px] group-hover:translate-x-[2px] transition-transform" />
                                </Button>
                            </CardFooter>
                        </form>
                    </Card>

                    <div className="lg:col-span-3 space-y-10">
                        {/* Profit Visualization Chart */}
                        {stats && stats.chartData.length > 0 && (
                            <Card className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-[1.5rem] overflow-hidden animate-in zoom-in duration-700">
                                <CardHeader className="px-10 py-8 border-b border-slate-50 bg-slate-50/30">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <CardTitle className="text-xl font-black text-slate-900 tracking-tight">Product Performance</CardTitle>
                                            <CardDescription className="text-slate-500 font-medium">Profit comparison of top selling products</CardDescription>
                                        </div>
                                        <div className="p-3 bg-white rounded-md shadow-sm border border-slate-100">
                                            <BarChart3 className="w-6 h-6 text-blue-600" />
                                        </div>
                                    </div>
                                </CardHeader>
                                <CardContent className="p-10">
                                    <ChartContainer config={chartConfig} className="h-[350px] w-full">
                                        <BarChart data={stats.chartData}>
                                            <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="#f1f5f9" />
                                            <XAxis
                                                dataKey="name"
                                                tickLine={false}
                                                tickMargin={10}
                                                axisLine={false}
                                                tick={{ fill: '#64748b', fontWeight: 'bold', fontSize: 10 }}
                                            />
                                            <YAxis
                                                tickLine={false}
                                                axisLine={false}
                                                tick={{ fill: '#64748b', fontWeight: 'bold', fontSize: 10 }}
                                                tickFormatter={(value) => `Tk${value}`}
                                            />
                                            <ChartTooltip
                                                cursor={{ fill: '#f8fafc' }}
                                                content={<ChartTooltipContent hideLabel />}
                                            />
                                            <Bar dataKey="profit" radius={[8, 8, 0, 0]} barSize={40}>
                                                {stats.chartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={index % 2 == 0 ? "#2563eb" : "#4f46e5"} />
                                                ))}
                                            </Bar>
                                        </BarChart>
                                    </ChartContainer>
                                </CardContent>
                            </Card>
                        )}

                        {/* Detailed Sales Table */}
                        <div className="bg-white rounded-[1.5rem] border border-slate-100 shadow-xl shadow-slate-200/50 overflow-hidden animate-in slide-in-from-bottom duration-700">
                            <div className="px-10 py-8 border-b border-slate-50 flex items-center justify-between">
                                <h2 className="text-xl font-black text-slate-900 tracking-tight flex items-center gap-3">
                                    <Package className="w-6 h-6 text-blue-600" />
                                    Transaction Ledger
                                </h2>
                            </div>
                            <div className="overflow-x-auto">
                                <Table className="bg-white">
                                    <TableHeader>
                                        <TableRow className="bg-slate-50/50 border-b border-slate-50 hover:bg-slate-50/50 transition-none">
                                            <TableHead className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 px-10 py-5">Product</TableHead>
                                            <TableHead className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-center">Volume</TableHead>
                                            <TableHead className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-center">Profitability</TableHead>
                                            <TableHead className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-right">Timestamp</TableHead>
                                        </TableRow>
                                    </TableHeader>

                                    <TableBody>
                                        {sales.length > 0 ?
                                            sales.map((item) => (
                                                <TableRow key={item.id} className="group hover:bg-slate-50/30 border-b border-slate-50/50 transition-colors">
                                                    <TableCell className="px-10 py-6">
                                                        <div className="flex items-center gap-4">
                                                            <div className="w-10 h-10 rounded-md bg-blue-50 flex items-center justify-center group-hover:scale-110 transition-transform">
                                                                <TrendingUp className="w-5 h-5 text-blue-600" />
                                                            </div>
                                                            <span className="font-black text-slate-900 group-hover:text-blue-700 transition-colors">{item.productName}</span>
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="px-10 py-6 text-center">
                                                        <span className="inline-flex items-center px-4 py-1.5 rounded-ml text-[11px] font-black uppercase tracking-widest bg-slate-100 text-slate-700">
                                                            {item.quantity} units
                                                        </span>
                                                    </TableCell>
                                                    <TableCell className="px-10 py-6 text-center">
                                                        <div className="flex items-center justify-center gap-1 font-black text-emerald-600">
                                                            <span className="text-lg tracking-tighter">TK{item.profit}</span>
                                                        </div>
                                                    </TableCell>
                                                    <TableCell className="px-10 py-6 text-right">
                                                        <div className="flex flex-col items-end">
                                                            <span className="text-[11px] font-black text-slate-900 uppercase tracking-widest leading-none mb-1">{formatDate(item.createdAt)}</span>
                                                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">Verified Sale</span>
                                                        </div>
                                                    </TableCell>
                                                </TableRow>
                                            ))
                                            :
                                            <TableRow>
                                                <TableCell colSpan={4} className="py-24 text-center">
                                                    <div className="flex flex-col items-center gap-4 opacity-30">
                                                        <div className="w-20 h-20 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                            <DollarSign className="w-10 h-10 text-slate-400" />
                                                        </div>
                                                        <p className="text-base font-black uppercase tracking-[0.3em] text-slate-900">No Sales Records</p>
                                                        <p className="text-sm font-medium">Your sales data will appear here once processed</p>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        }
                                    </TableBody>
                                </Table>
                            </div>
                        </div>

                        <div className="flex items-center justify-center py-4">
                            <PaginationButton totalPages={totalPages}/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
