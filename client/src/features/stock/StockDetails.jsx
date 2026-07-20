import {useEffect} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {useQuery} from "@tanstack/react-query";
import {ArrowLeft, Package, Calendar, DollarSign, Hash, Layers} from "lucide-react";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge"
import {Separator} from "@/components/ui/separator"
import {Axios} from '@/services/http/Axios.js';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {formatDateAndTime} from '@/utils/index.js';
import {TOAST_TYPE} from "@/utils/enums.js";
import {toastify} from '@/common/toastify.js';
import { BackButton } from '@/components/common/BackButton';

const fetchStock = async (id) => {
    const response = await Axios.get(`/stocks/${id}`)
    return response.data.data
}

const StockDetails = () => {
    const {id} = useParams();
    const navigate = useNavigate();

    const {
        data: stock,
        isFetching: isPageLoading,
        isError,
        error
    } = useQuery({
        enabled: !!id,
        queryKey: ["stock", id],
        queryFn: () => fetchStock(id),
    });

    useEffect(() => {
        if (isError) {
            console.error(error);
            toastify(TOAST_TYPE.ERROR, "Failed to load stock details")
        }
    }, [error, isError]);

    return (
        <div className="min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 md:py-10 animate-in fade-in duration-700">
                {/* Header with Back Button */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
                    <div className="space-y-4">
                        <BackButton url="/stocks"/>

                        <div>
                            <h1 className="text-4xl font-bold text-slate-900 tracking-tight leading-tight flex items-center gap-4">
                                Stock Receipt
                                <Badge className="bg-blue-100 text-blue-700 border-none px-4 py-1 rounded-full text-xs font-black uppercase tracking-widest">
                                    #{id}
                                </Badge>
                            </h1>
                            <p className="text-slate-500 font-medium mt-2">Detailed breakdown of stock items and costs for this entry</p>
                        </div>
                    </div>
                </div>

                {/* Summary Section */}
                <div className="grid md:grid-cols-3 gap-8 mb-12">
                    <Card className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-[1rem] overflow-hidden">
                        <CardHeader className="bg-slate-50/50 border-b border-slate-50 pb-4 pt-6 px-8">
                            <CardTitle className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                <DollarSign className="w-3.5 h-3.5" /> Total Investment
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-6 px-8 pb-8">
                            <div className="flex items-baseline gap-1">
                                <span className="text-4xl font-bold text-slate-900 tracking-tighter">
                                    {stock?.totalCost || '0.00'}
                                </span>
                                <span className="text-sm font-bold text-slate-400 uppercase tracking-widest ml-1">Tk</span>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-[1rem] overflow-hidden">
                        <CardHeader className="bg-slate-50/50 border-b border-slate-50 pb-4 pt-6 px-8">
                            <CardTitle className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                <Calendar className="w-3.5 h-3.5" /> Entry Date
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-6 px-8 pb-8">
                            <p className="text-xl font-bold text-slate-800 tracking-tight leading-tight">
                                {stock?.createdAt ? formatDateAndTime(stock.createdAt) : 'N/A'}
                            </p>
                        </CardContent>
                    </Card>

                    <Card className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-[1rem] overflow-hidden">
                        <CardHeader className="bg-slate-50/50 border-b border-slate-50 pb-4 pt-6 px-8">
                            <CardTitle className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                <Package className="w-3.5 h-3.5" /> Total Items
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-6 px-8 pb-8">
                            <p className="text-4xl font-bold text-slate-900 tracking-tighter">
                                {stock?.items?.length || 0}
                            </p>
                        </CardContent>
                    </Card>
                </div>

                {/* Items Table */}
                <div className="bg-white rounded-[1rem] border border-slate-100 shadow-xl shadow-slate-200/50 overflow-hidden mb-12 animate-in slide-in-from-bottom duration-1000">
                    <div className="px-10 py-8 border-b border-slate-50 flex items-center justify-between">
                        <h2 className="text-xl font-black text-slate-900 tracking-tight flex items-center gap-3">
                            <Layers className="w-6 h-6 text-blue-600" />
                            Stock Items
                        </h2>
                    </div>

                    <div className="overflow-x-auto">
                        <Table>
                            <TableHeader>
                                <TableRow className="bg-slate-50/50 border-b border-slate-50 hover:bg-slate-50/50 transition-none">
                                    <TableHead className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 px-10 py-5">
                                        <div className="flex items-center gap-2"><Hash className="w-3 h-3" /> Item ID</div>
                                    </TableHead>
                                    <TableHead className="text-md font-bold uppercase tracking-[0.2em] text-slate-400 px-10 py-5">Product Name</TableHead>
                                    <TableHead className="text-md font-bold uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-center">Initial Qty</TableHead>
                                    <TableHead className="text-md font-bold uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-center">Remaining</TableHead>
                                    <TableHead className="text-md font-bold uppercase tracking-[0.2em] text-slate-400 px-10 py-5">Unit Price</TableHead>
                                    <TableHead className="text-md font-bold uppercase tracking-[0.2em] text-slate-400 px-10 py-5 text-right">Subtotal</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {stock?.items?.map((item, index) => (
                                    <TableRow key={item.id} className="group border-b border-slate-50/50 hover:bg-slate-50/30 transition-colors">
                                        <TableCell className="px-10 py-6">
                                            <span className="font-bold text-slate-400 group-hover:text-blue-600 transition-colors">#{item.id}</span>
                                        </TableCell>
                                        <TableCell className="px-10 py-6">
                                            <div className="flex flex-col">
                                                <span className="font-bold text-slate-900 text-lg group-hover:text-blue-700 transition-colors">{item.productName}</span>
                                            </div>
                                        </TableCell>
                                        <TableCell className="px-10 py-6 text-center">
                                            <span className="inline-flex items-center justify-center w-10 h-10 rounded-md bg-slate-100 font-black text-slate-700 text-sm">
                                                {item.quantity}
                                            </span>
                                        </TableCell>
                                        <TableCell className="px-10 py-6 text-center">
                                            <Badge className={`px-4 py-1.5 rounded-xl border-none font-black text-[11px] uppercase tracking-widest ${
                                                item.remaining > 0 
                                                ? "bg-emerald-50 text-emerald-700" 
                                                : "bg-rose-50 text-rose-700"
                                            }`}>
                                                {item.remaining} Left
                                            </Badge>
                                        </TableCell>
                                        <TableCell className="px-10 py-6">
                                            <div className="flex items-center gap-1 font-bold text-slate-600">
                                                <span>{item.purchasePrice}</span>
                                                <span className="text-[10px] text-slate-400">Tk</span>
                                            </div>
                                        </TableCell>
                                        <TableCell className="px-10 py-6 text-right">
                                            <div className="flex items-center justify-end gap-1">
                                                <span className="text-lg font-black text-slate-900 tracking-tighter">{item.subtotal}</span>
                                                <span className="text-[10px] font-bold text-slate-400 uppercase">Tk</span>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                </div>

                <Separator className="mb-12 opacity-50" />

                <div className="flex items-center justify-center">
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-[0.3em]">End of Stock Receipt</p>
                </div>
            </div>
        </div>
    )
}

export default StockDetails
