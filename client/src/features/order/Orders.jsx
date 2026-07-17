import React, {useEffect, useMemo, useCallback} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery, keepPreviousData} from "@tanstack/react-query";
import {useForm, Controller} from "react-hook-form";
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {Axios} from '@/services/http/Axios';
import PaginationButton from '@/components/common/PaginationButton';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import {formatDateAndTime, GLOBAL_ERROR, handleErrors, hasPermission} from '@/utils';
import {FIRST_PAGE, getQueryString, normalizeQuery, redirectWhenInvalidPage, ALL_SELECTED, getSelectValue} from '@/utils/PaginationUtils';
import {Label} from '@/components/ui/label';
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Input} from '@/components/ui/input';
import {Button} from '@/components/ui/button';
import {APP_MODULE, ORDER_STATUS, PERMISSION, TOAST_TYPE} from '@/utils/enums';
import InputError from "@/components/common/InputError";
import {notify} from "@/components/common/notification";
import {queryClient} from "@/services/queryClient";
import {useUserStore} from "@/store/useUserStore";
import { cn } from "@/lib/utils";

import {ReportDialog} from "@/components/common/ReportDialog";

const fetchOrders = async ({queryKey}) => {
    const [, params] = queryKey;

    const response = await Axios.get("/orders", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            productName: params.productName || undefined,
            fromDate: params.fromDate || undefined,
            toDate: params.toDate || undefined,
            status: params.status == ALL_SELECTED ? undefined : params.status,
        },
    });

    return response.data.data;
};

const orderFilterSchema = z.object({
    productName: z.string().optional(),
    fromDate: z.string().optional(),
    toDate: z.string().optional(),
    status: z.string().optional(),
});

const Orders = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);

    const {user} = useUserStore();

    const defaultStatus = hasPermission(user, PERMISSION.DELIVERY_MAN_ACCESS) ? ORDER_STATUS.SHIPPED : ORDER_STATUS.PENDING;

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, []),
            productName: queryParams.productName || "",
            fromDate: queryParams.fromDate || "",
            toDate: queryParams.toDate || "",
            status: queryParams.status || defaultStatus,
        }),
        [queryParams]
    );
    const {page, productName, fromDate, toDate, status} = filters;

    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: {errors},
        control,
    } = useForm({
        resolver: zodResolver(orderFilterSchema),
        defaultValues: {
            productName: "",
            fromDate: "",
            toDate: "",
            status: defaultStatus
        },
    });

    const {
        data,
        isPending: isPageLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["orders", filters],
        queryFn: fetchOrders,
        placeholderData: keepPreviousData,
    });

    const orders = data?.content ?? [];
    const totalPages = data?.totalPages ?? FIRST_PAGE;

    useEffect(() => {
        if (isError) {
            console.error("error:", error);
            handleErrors(error, setError);
            notify(TOAST_TYPE.ERROR, "Failed to show orders")
        }
    }, [error, isError]);

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    useEffect(() => {
        reset({
            productName,
            fromDate,
            toDate,
            status,
        });
    }, [productName, fromDate, toDate, status, reset]);

    const handleFilter = useCallback(
        (data) => {
            navigate(
                getQueryString({
                    ...queryParams,
                    productName: data.productName || undefined,
                    fromDate: data.fromDate || undefined,
                    toDate: data.toDate || undefined,
                    status: data.status || undefined,
                    page: FIRST_PAGE,
                }),
                {replace: true}
            );
        },
        [navigate, queryParams]
    );

    const changeOrderStatus = async (id, status) => {
        try {
            await Axios.patch(`/orders/${id}/status`, {
                status
            });

            await queryClient.invalidateQueries({queryKey: ["orders"]});
            notify(TOAST_TYPE.SUCCESS, `Updated order with ID ${id} status changed to ${status}`)
        } catch (error) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, error.response.data.errors.global[0])
        }
    }

    const cancelOrder = async (id) => {
        try {
            await Axios.patch(`/orders/${id}/cancel`);

            await queryClient.invalidateQueries({queryKey: ["orders"]});
            notify(TOAST_TYPE.SUCCESS, `Cancelled order with ID ${id}`)
        } catch (error) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, error.response.data.errors.global[0])
        }
    }

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">My Orders</h1>
                        <p className="text-slate-500 font-medium">Track and manage your recent purchases</p>
                    </div>
                    {(hasPermission(user, PERMISSION.ADMIN_ACCESS) || hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS)) 
                        && <ReportDialog module={APP_MODULE.ORDER} />}
                </div>

                <div className='grid lg:grid-cols-4 gap-10 items-start'>
                    {/* Filter Sidebar */}
                    <Card className='lg:col-span-1 border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden sticky top-24'>
                        <form onSubmit={handleSubmit(handleFilter)}>
                            <CardHeader className="bg-slate-100 border-b border-slate-100 pb-4">
                                <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="p-1.5 bg-blue-600 rounded-lg text-white">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"/></svg>
                                    </span>
                                    Filter Orders
                                </CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-6 pt-6">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>

                                {/* Status */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">Order Status</Label>
                                    <Controller
                                        name="status"
                                        control={control}
                                        render={({ field }) => (
                                            <Select 
                                                onValueChange={field.onChange} 
                                                value={getSelectValue(field.value)}
                                            >
                                                <SelectTrigger className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all">
                                                    <SelectValue placeholder="Select Status" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value={ALL_SELECTED}>All Status</SelectItem>
                                                    {Object.values(ORDER_STATUS).map((status) => (
                                                        <SelectItem key={status} value={status}>
                                                            {status}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                    />
                                    <InputError errors={errors} field="status"/>
                                </div>

                                {/* Product Name */}
                                <div className="space-y-2">
                                    <Label className="text-base font-semibold uppercase tracking-widest ml-1">Product Name</Label>
                                    <Input
                                        placeholder="Search by name..."
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
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Customer</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Total Amount</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Date & Time</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-center">Payment Method</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-center">Paid</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-center">Status</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {orders.length > 0 ?
                                        orders.map((item) => (
                                            <TableRow key={item.id} className="group hover:bg-slate-50/50 border-b border-slate-50 transition-colors">
                                                <TableCell className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-[10px] font-black text-blue-600">
                                                            {item.userName?.slice(0, 1)}
                                                        </div>
                                                        <span className="font-bold text-slate-700">{item.userName}</span>
                                                    </div>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="font-semibold text-slate-900">${item.totalPrice}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="text-xs font-semibold text-slate-500">{formatDateAndTime(item.createdAt)}</span>
                                                </TableCell>
                                                 <TableCell className="px-6 py-4">
                                                    <span className="text-xs font-semibold text-slate-500">{item.paymentMethod}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <span className="text-xs font-semibold text-slate-500">{item.paid ? 'Yes' : 'No'}</span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-center">
                                                    <span className={cn(
                                                        "inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-widest",
                                                        item.status == ORDER_STATUS.PENDING && "bg-amber-100 text-amber-700",
                                                        item.status == ORDER_STATUS.ACCEPTED && "bg-emerald-100 text-emerald-700",
                                                        item.status == ORDER_STATUS.REJECTED && "bg-red-100 text-red-700",
                                                        item.status == ORDER_STATUS.CANCELLED && "bg-slate-200 text-slate-600",
                                                        item.status == ORDER_STATUS.SHIPPED && "bg-indigo-100 text-indigo-700",
                                                        item.status == ORDER_STATUS.DELIVERED && "bg-emerald-200 text-emerald-800",
                                                        item.status == ORDER_STATUS.LOST && "bg-orange-100 text-orange-700",
                                                        item.status == ORDER_STATUS.COMPLETED && "bg-blue-100 text-blue-700"
                                                    )}>
                                                        {item.status}
                                                    </span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-right">
                                                    <div className="flex justify-end gap-2">
                                                        <Button 
                                                            variant="outline"
                                                            size="sm"
                                                            className="h-9 px-4 rounded-lg font-bold text-xs text-blue-600 hover:bg-blue-50"
                                                            onClick={() => navigate(`/orders/${item.id}`)} 
                                                        >
                                                            View
                                                        </Button>

                                                        {/* Admin Actions */}
                                                        {(hasPermission(user, PERMISSION.ADMIN_ACCESS) || hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS)) ? (
                                                            <>
                                                                {(item.status === ORDER_STATUS.PENDING || item.status === ORDER_STATUS.ACCEPTED || item.status === ORDER_STATUS.DELIVERED) && (
                                                                    <DropdownMenu>
                                                                        <DropdownMenuTrigger asChild>
                                                                            <Button variant="outline" size="sm" className="h-9 px-4 rounded-lg font-bold text-xs text-slate-600 hover:bg-slate-50">
                                                                                Actions
                                                                            </Button>
                                                                        </DropdownMenuTrigger>
                                                                        <DropdownMenuContent align="end" className="w-40">
                                                                            {item.status === ORDER_STATUS.PENDING && (
                                                                                <>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-emerald-600 focus:text-emerald-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.ACCEPTED)}
                                                                                    >
                                                                                        Accept
                                                                                    </DropdownMenuItem>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-red-600 focus:text-red-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.REJECTED)}
                                                                                    >
                                                                                        Reject
                                                                                    </DropdownMenuItem>
                                                                                </>
                                                                            )}
                                                                            {item.status === ORDER_STATUS.ACCEPTED && (
                                                                                <>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-red-600 focus:text-red-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.REJECTED)}
                                                                                    >
                                                                                        Reject
                                                                                    </DropdownMenuItem>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-indigo-600 focus:text-indigo-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.SHIPPED)}
                                                                                    >
                                                                                        Shipped
                                                                                    </DropdownMenuItem>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-emerald-600 focus:text-emerald-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.DELIVERED)}
                                                                                    >
                                                                                        Delivered
                                                                                    </DropdownMenuItem>
                                                                                    <DropdownMenuItem 
                                                                                        className="font-bold text-xs text-orange-600 focus:text-orange-600"
                                                                                        onClick={() => changeOrderStatus(item.id, ORDER_STATUS.LOST)}
                                                                                    >
                                                                                        Lost
                                                                                    </DropdownMenuItem>
                                                                                </>
                                                                            )}
                                                                            {item.status === ORDER_STATUS.DELIVERED && (
                                                                                <DropdownMenuItem 
                                                                                    className="font-bold text-xs text-blue-600 focus:text-blue-600"
                                                                                    onClick={() => changeOrderStatus(item.id, ORDER_STATUS.COMPLETED)}
                                                                                >
                                                                                    Complete
                                                                                </DropdownMenuItem>
                                                                            )}
                                                                        </DropdownMenuContent>
                                                                    </DropdownMenu>
                                                                )}
                                                            </>
                                                        ) : hasPermission(user, PERMISSION.DELIVERY_MAN_ACCESS) ? (
                                                            /* Delivery Man Actions */
                                                            item.status === ORDER_STATUS.SHIPPED && (
                                                                <Button
                                                                    variant="outline"
                                                                    size="sm"
                                                                    className="h-9 px-4 rounded-lg font-bold text-xs text-emerald-600 hover:bg-emerald-50"
                                                                    onClick={() => changeOrderStatus(item.id, ORDER_STATUS.DELIVERED)}
                                                                >
                                                                    Deliver
                                                                </Button>
                                                            )
                                                        ) : (
                                                            /* Customer Actions */
                                                            item.status === ORDER_STATUS.PENDING && (
                                                                <Button
                                                                    variant="outline"
                                                                    size="sm"
                                                                    className="h-9 px-4 rounded-lg font-bold text-xs text-red-600 hover:bg-red-50"
                                                                    onClick={() => cancelOrder(item.id)}
                                                                >
                                                                    Cancel
                                                                </Button>
                                                            )
                                                        )}
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ))
                                        :
                                        <TableRow>
                                            <TableCell colSpan={5} className="py-20 text-center">
                                                <div className="flex flex-col items-center gap-2 opacity-40">
                                                    <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/></svg>
                                                    </div>
                                                    <p className="text-sm font-black uppercase tracking-widest">No orders found</p>
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
};

export default Orders;
