import {useEffect, useState} from "react"
import {useParams} from "react-router-dom"
import {Package, CheckCircle2, Truck, Home, Clock, AlertCircle} from "lucide-react"

import {Button} from "@/components/ui/button"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import {Label} from "@/components/ui/label"
import {Separator} from "@/components/ui/separator"
import StaredLabel from "@/components/common/StaredLabel"
import {useUserStore} from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import {Axios} from "@/services/http/Axios"
import {ORDER_STATUS} from "@/constants/order.constants";
import {TOAST_TYPE} from "@/constants/app.constants";
import {toastify} from "@/common/toastify.js"
import {useQuery} from "@tanstack/react-query"
import {BackButton} from "@/components/common/BackButton"
import {getTimeAgo} from "@/utils/DateUtils"
import {cn} from "@/lib/utils"

const fetchOrder = async (id) => {
    const response = await Axios.get(`/orders/${id}`);
    return response.data.data;
}

const STEPS = [
    {status: ORDER_STATUS.PENDING, label: "Placed", icon: Clock},
    {status: ORDER_STATUS.ACCEPTED, label: "Approved", icon: CheckCircle2},
    {status: ORDER_STATUS.SHIPPED, label: "Shipped", icon: Truck},
    {status: ORDER_STATUS.DELIVERED, label: "Delivered", icon: Home},
];

export default function OrderDetails() {
    const {id} = useParams()
    const {user} = useUserStore();

    const {
        data: order = {},
        isFetching: isPageLoading,
        isError,
        error,
    } = useQuery({
        enabled: !!id,
        queryKey: ["order", id],
        queryFn: () => fetchOrder(id),
    })

    useEffect(() => {
        if (!isError) return;

        console.error(error);
        toastify(TOAST_TYPE.ERROR, "Failed to show order");
    }, [isError, error]);

    const getCurrentStep = () => {
        if (!order?.status) return 0;
        const index = STEPS.findIndex(s => s.status == order.status);
        if (index == -1) {
            // If status is COMPLETED, it's essentially Delivered
            if (order.status == ORDER_STATUS.COMPLETED) return 3;
            // If status is CANCELLED/REJECTED/etc., we might want to handle it differently
            return 0;
        }
        return index;
    };

    const currentStep = getCurrentStep();
    const isCancelled = [ORDER_STATUS.CANCELLED, ORDER_STATUS.REJECTED].includes(order?.status);

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container py-10 min-h-screen bg-slate-50/30">
                <BackButton url="/orders"/>

                <div className="max-w-5xl mx-auto mt-6 space-y-8">
                    {/* Status Dashboard */}
                    <Card className="border-none shadow-lg shadow-slate-200/50 overflow-hidden bg-white">
                        <div className="bg-blue-600 px-8 py-6 text-white">
                            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                <div>
                                    <h2 className="text-2xl font-bold tracking-tight flex items-center gap-2">
                                        Order #{id}
                                    </h2>
                                    <p className="text-blue-100 font-medium mt-1">
                                        Placed {getTimeAgo(order?.createdAt)}
                                    </p>
                                </div>
                                <div
                                    className="bg-white/20 backdrop-blur-md px-4 py-2 rounded-lg border border-white/20">
                                    <span className="text-sm font-bold uppercase tracking-widest">
                                        Current Status: {order?.status}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <CardContent className="p-8 md:p-12">
                            {isCancelled ? (
                                <div className="flex flex-col items-center justify-center py-6 text-center">
                                    <div
                                        className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-4">
                                        <AlertCircle className="w-8 h-8 text-red-500"/>
                                    </div>
                                    <h3 className="text-xl font-bold text-slate-900">Order {order?.status}</h3>
                                    <p className="text-slate-500 mt-2">This order has been {order?.status.toLowerCase()}.
                                        Please contact support if you have any questions.</p>
                                </div>
                            ) : (
                                <div className="relative">
                                    {/* Line connecting steps */}
                                    <div
                                        className="absolute top-1/2 left-0 w-full h-1 bg-slate-100 -translate-y-1/2 hidden md:block"/>
                                    <div
                                        className="absolute top-1/2 left-0 h-1 bg-blue-600 -translate-y-1/2 transition-all duration-1000 hidden md:block"
                                        style={{width: `${(currentStep / (STEPS.length - 1)) * 100}%`}}
                                    />

                                    <div className="relative flex flex-col md:flex-row justify-between gap-8 md:gap-4">
                                        {STEPS.map((step, index) => {
                                            const Icon = step.icon;
                                            const isActive = index <= currentStep;
                                            const isCurrent = index == currentStep;

                                            return (
                                                <div key={step.label}
                                                     className="flex md:flex-col items-center gap-4 md:gap-3 z-10">
                                                    <div className={cn(
                                                        "w-12 h-12 rounded-full flex items-center justify-center transition-all duration-500 border-4",
                                                        isActive
                                                            ? "bg-blue-600 border-white text-white shadow-lg shadow-blue-200"
                                                            : "bg-white border-slate-100 text-slate-300"
                                                    )}>
                                                        <Icon className={cn("w-5 h-5", isCurrent && "animate-pulse")}/>
                                                    </div>
                                                    <div className="text-left md:text-center">
                                                        <p className={cn(
                                                            "font-bold text-sm uppercase tracking-widest transition-colors duration-500",
                                                            isActive ? "text-slate-900" : "text-slate-400"
                                                        )}>
                                                            {step.label}
                                                        </p>
                                                        {isCurrent && (
                                                            <p className="text-[10px] font-bold text-blue-600 uppercase tracking-widest mt-1">
                                                                Active
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            <Separator className="my-10 opacity-50"/>

                            <div className="grid md:grid-cols-3 gap-8">
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-[0.2em]">Estimated
                                        Delivery</p>
                                    <p className="text-lg font-bold text-slate-900">3-5 Business Days</p>
                                    <p className="text-xs text-slate-500">From the date of approval</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-[0.2em]">Shipping
                                        Method</p>
                                    <p className="text-lg font-bold text-slate-900">Standard Courier</p>
                                    <p className="text-xs text-slate-500">Doorstep Delivery</p>
                                </div>
                                <div className="space-y-1">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-[0.2em]">Total
                                        Amount</p>
                                    <p className="text-3xl font-black text-blue-600 tracking-tighter">${order?.totalPrice}</p>
                                    <p className="text-xs text-slate-500">{order?.paymentMethod}</p>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    <div className="grid gap-8 lg:grid-cols-5">
                        {/* Details Area */}
                        <div className="lg:col-span-3 space-y-8">
                            <Card
                                className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden bg-white">
                                <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-blue-600 rounded-lg text-white">
                                            < Truck className="h-5 w-5"/>
                                        </div>
                                        <div>
                                            <CardTitle className="text-xl font-bold text-slate-800 tracking-tight">Delivery
                                                Information</CardTitle>
                                            <CardDescription className="text-slate-500 font-medium mt-1">Where we're
                                                delivering your order</CardDescription>
                                        </div>
                                    </div>
                                </CardHeader>
                                <CardContent className="p-8">
                                    <div className="grid md:grid-cols-2 gap-8">
                                        <div className="space-y-2">
                                            <Label
                                                className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Recipient
                                                Name</Label>
                                            <div
                                                className="bg-slate-50 px-4 py-3 rounded-lg border border-slate-100 text-slate-900 font-bold text-sm">
                                                {user?.name}
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <Label
                                                className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Phone
                                                Number</Label>
                                            <div
                                                className="bg-slate-50 px-4 py-3 rounded-lg border border-slate-100 text-slate-900 font-bold text-sm">
                                                {order?.phone}
                                            </div>
                                        </div>

                                        <div className="space-y-2 md:col-span-2">
                                            <Label
                                                className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Shipping
                                                Address</Label>
                                            <div
                                                className="bg-slate-50 px-4 py-3 rounded-lg border border-slate-100 text-slate-900 font-bold text-sm">
                                                {order?.address}
                                            </div>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            <Card
                                className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden bg-white">
                                <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8">
                                    <CardTitle className="text-xl font-bold text-slate-800 tracking-tight">Order
                                        Items</CardTitle>
                                    <CardDescription className="text-slate-500 font-medium mt-1">Detailed list of
                                        products in this order</CardDescription>
                                </CardHeader>
                                <CardContent className="p-8">
                                    <div className="space-y-4">
                                        {order?.items?.map((item) => (
                                            <div key={item.id}
                                                 className="flex items-center gap-6 p-4 rounded-xl bg-slate-50 border border-slate-100 group">
                                                <div
                                                    className="w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-white border border-slate-200 shadow-sm">
                                                    <img
                                                        src={item.productImage || "/placeholder.svg"}
                                                        alt={item.productName}
                                                        className="w-full h-full object-cover transition-transform group-hover:scale-110 duration-500"
                                                    />
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <h4 className="text-lg font-bold text-slate-800 truncate">{item.productName}</h4>
                                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Quantity: {item.quantity}</p>
                                                </div>
                                                <div className="text-right">
                                                    <p className="text-xl font-black text-slate-900 tracking-tighter">${(item.productPrice * item.quantity).toFixed(2)}</p>
                                                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">${item.productPrice} each</p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>
                        </div>

                        {/* Summary Sidebar */}
                        <div className="lg:col-span-2 space-y-8">
                            <Card
                                className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden bg-white sticky top-24">
                                <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8">
                                    <CardTitle className="text-xl font-bold text-slate-800 tracking-tight">Payment
                                        Summary</CardTitle>
                                </CardHeader>
                                <CardContent className="p-8 space-y-6">
                                    <div className="space-y-4">
                                        <div className="flex justify-between items-center">
                                            <span className="text-slate-500 font-medium">Subtotal</span>
                                            <span className="text-slate-900 font-bold">${order?.orderPrice}</span>
                                        </div>
                                        <div className="flex justify-between items-center text-emerald-600">
                                            <span className="font-medium">Shipping</span>
                                            <span
                                                className="font-bold text-[10px] uppercase tracking-widest">Free</span>
                                        </div>
                                        <Separator className="opacity-50"/>
                                        <div className="flex justify-between items-center pt-2">
                                            <span className="text-lg font-bold text-slate-900">Total Price</span>
                                            <span
                                                className="text-3xl font-black text-blue-600 tracking-tighter">${order?.totalPrice}</span>
                                        </div>
                                    </div>

                                    <div className={cn(
                                        "p-6 rounded-2xl flex flex-col gap-3",
                                        order?.paid ? "bg-emerald-50 border border-emerald-100" : "bg-amber-50 border border-amber-100"
                                    )}>
                                        <div className="flex items-center justify-between">
                                            <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Payment
                                                Status</p>
                                            <span className={cn(
                                                "px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-widest",
                                                order?.paid ? "bg-emerald-500 text-white" : "bg-amber-500 text-white"
                                            )}>
                                                {order?.paid ? "Paid" : "Unpaid"}
                                            </span>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <div className={cn(
                                                "p-2 rounded-lg text-white",
                                                order?.paid ? "bg-emerald-600" : "bg-amber-600"
                                            )}>
                                                {order?.paid ? <CheckCircle2 className="h-5 w-5"/> :
                                                    <Clock className="h-5 w-5"/>}
                                            </div>
                                            <div className="space-y-0.5">
                                                <p className="text-sm font-bold text-slate-800">{order?.paymentMethod}</p>
                                                <p className="text-[10px] font-medium text-slate-500 uppercase tracking-widest">
                                                    {order?.paid ? "Transaction verified" : "Pending payment confirmation"}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="pt-4 text-center">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Need
                                            help with your order?</p>
                                        <Button variant="link" className="text-blue-600 font-bold text-sm mt-1">Contact
                                            Support</Button>
                                    </div>
                                </CardContent>
                            </Card>
                        </div>
                    </div>
                </div>
            </div>
        </>
    )
}
