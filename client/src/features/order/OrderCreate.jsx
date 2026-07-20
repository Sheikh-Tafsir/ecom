import {useNavigate} from "react-router-dom"
import {Package, ChevronRight, ShieldCheck, Clock} from "lucide-react"
import {useForm, Controller} from "react-hook-form"
import {zodResolver} from "@hookform/resolvers/zod"
import * as z from "zod"

import {Button} from "@/components/ui/button"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {Separator} from "@/components/ui/separator"
import {useCartStore} from "@/store/useCartStore"
import {useUserStore} from "@/store/useUserStore"
import {Axios} from "@/services/http/Axios"
import {GLOBAL_ERROR, handleErrors} from "@/utils"
import {ButtonLoading} from "@/components/common/ButtonLoading"
import InputError from "@/components/common/InputError.jsx"
import {PAYMENT_METHOD, TOAST_TYPE} from "@/utils/enums.js"
import {getIdempotencyKey, IDEMPOTENCY_HEADER, removeIdempotencyKey} from "@/utils/idempotencyUtil.js"
import {toastify} from "@/common/toastify.js"
import {cn} from "@/lib/utils"

const checkoutSchema = z.object({
    phone: z.string()
        .min(11, 'Phone number must be 11 digits')
        .max(11, 'Phone number must be 11 digits'),
    address: z.string().min(5, 'Address must be at least 5 characters'),
    paymentMethod: z.string().min(1, 'Please select a payment method'),
})

const createOrder = async (items, name, data, idempotencyKey) => {
    try {
        return Axios.post("/orders", {
                items,
                name,
                ...data,
            },
            {
                headers: {
                    [IDEMPOTENCY_HEADER]: idempotencyKey,
                },
            }
        );
    } catch (error) {
        console.error(error);
        toastify(TOAST_TYPE.ERROR, "Order placed Failed!");
        throw error;
    }
};

export const createPayment = async (order, userId) => {
    try {
        return await Axios.post("/payment", {
            userId: userId,
            orderId: order.id,
            amount: order.totalPrice,
            payerReference: order.phone,
        });
    } catch (error) {
        console.error(error);
        toastify(TOAST_TYPE.ERROR, "Payment Failed!");
        throw error;
    }
};

export default function OrderCreate() {
    const navigate = useNavigate();

    const {user} = useUserStore();
    const {cart, getCartTotal, clearCart} = useCartStore();
    const cartTotal = getCartTotal();

    const {
        register,
        handleSubmit,
        setError,
        control,
        reset,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(checkoutSchema),
        defaultValues: {
            phone: '',
            address: '',
            paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
        },
    });

    const cleanupAfterOrder = () => {
        clearCart();
        reset();
        removeIdempotencyKey();
    };

    const saveOrder = async (data) => {
        const idempotencyKey = getIdempotencyKey();

        try {
            const orderResponse = await createOrder(cart, user?.name, data, idempotencyKey);
            const order = orderResponse.data.data;

            if (data.paymentMethod == PAYMENT_METHOD.CASH_ON_DELIVERY) {
                cleanupAfterOrder();
                toastify(TOAST_TYPE.SUCCESS, "Order placed successfully!");
                navigate(`/orders/${order.id}`);
                return;
            }

            const paymentResponse = await createPayment(order, user.id, data.phone);

            cleanupAfterOrder();
            toastify(TOAST_TYPE.SUCCESS, "Order placed and payment completed successfully!");
            window.location.assign(paymentResponse.data.data);
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
            if (error.response) {
                removeIdempotencyKey()
            }
        }
    };

    return (
        <div className="bg-slate-50 min-h-screen">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Checkout</h1>
                        <p className="text-slate-500 font-medium">Complete your order details below</p>
                    </div>
                </div>

                <div className="grid lg:grid-cols-5 gap-10 items-start">
                    {/* Main Form Area */}
                    <div className="lg:col-span-3 space-y-8 animate-in fade-in slide-in-from-left duration-700">
                        <Card
                            className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden bg-white">
                            <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8">
                                <div className="flex items-center gap-3">
                                    <div className="p-2 bg-blue-600 rounded-lg text-white">
                                        <Package className="h-5 w-5"/>
                                    </div>
                                    <div>
                                        <CardTitle className="text-xl font-bold text-slate-800 tracking-tight">Shipping
                                            Information</CardTitle>
                                        <CardDescription className="text-slate-500 font-medium mt-1">Where should we
                                            send your items?</CardDescription>
                                    </div>
                                </div>
                            </CardHeader>
                            <CardContent className="p-8">
                                <form id="checkout-form" className="space-y-8" onSubmit={handleSubmit(saveOrder)}>
                                    <InputError errors={errors} field={GLOBAL_ERROR}/>

                                    <div className="grid md:grid-cols-2 gap-6">
                                        <div className="space-y-2">
                                            <Label
                                                className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Full
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
                                            <Input
                                                type="tel"
                                                placeholder="+1 (555) 000-0000"
                                                className="h-12 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-medium"
                                                {...register('phone')}
                                            />
                                            <InputError errors={errors} field={"phone"}/>
                                        </div>
                                    </div>

                                    <div className="space-y-2">
                                        <Label
                                            className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Street
                                            Address</Label>
                                        <Input
                                            placeholder="123 Education St, Knowledge City"
                                            className="h-12 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-medium"
                                            {...register('address')}
                                        />
                                        <InputError errors={errors} field={"address"}/>
                                    </div>

                                    <div className="space-y-2">
                                        <Label
                                            className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Payment
                                            Method</Label>
                                        <Controller
                                            name="paymentMethod"
                                            control={control}
                                            render={({field}) => (
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                    {Object.entries(PAYMENT_METHOD).map(([key, label]) => (
                                                        <div
                                                            key={key}
                                                            onClick={() => field.onChange(label)}
                                                            className={cn(
                                                                "relative flex items-center gap-4 p-4 rounded-lg border-2 transition-all cursor-pointer group",
                                                                field.value == label
                                                                    ? "border-blue-600 bg-blue-50/50 shadow-md"
                                                                    : "border-slate-100 bg-white hover:border-slate-200"
                                                            )}
                                                        >
                                                            <div className={cn(
                                                                "w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all",
                                                                field.value == label ? "border-blue-600 bg-blue-600" : "border-slate-200 group-hover:border-slate-300"
                                                            )}>
                                                                {field.value == label && <div
                                                                    className="w-1.5 h-1.5 rounded-full bg-white"/>}
                                                            </div>
                                                            <div>
                                                                <p className="text-sm font-bold text-slate-800">{label}</p>
                                                                <p className="text-[10px] font-medium text-slate-500 uppercase tracking-widest">
                                                                    {label == PAYMENT_METHOD.CASH_ON_DELIVERY ? "Pay when you receive" : "Instant processing"}
                                                                </p>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        />
                                        <InputError errors={errors} field={"paymentMethod"}/>
                                    </div>
                                </form>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Right: Order Summary Sidebar */}
                    <div className="lg:col-span-2 space-y-8 animate-in fade-in slide-in-from-right duration-700">
                        <Card
                            className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden bg-white sticky top-24">
                            <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8">
                                <CardTitle className="text-xl font-bold text-slate-800 tracking-tight">Order
                                    Summary</CardTitle>
                                <CardDescription className="text-slate-500 font-medium mt-1">Review your items before
                                    placing the order</CardDescription>
                            </CardHeader>
                            <CardContent className="p-8 space-y-6">
                                <div className="max-h-[300px] overflow-y-auto pr-2 space-y-4 custom-scrollbar">
                                    {cart?.map((item) => (
                                        <div key={item.productId}
                                             className="flex items-center gap-4 p-3 rounded-lg bg-slate-50 border border-slate-100 group">
                                            <div
                                                className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0 bg-white border border-slate-200 shadow-sm">
                                                <img
                                                    src={item.image || "/placeholder.svg"}
                                                    alt={item.name}
                                                    className="w-full h-full object-cover transition-transform group-hover:scale-110 duration-500"
                                                />
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <h4 className="text-sm font-bold text-slate-800 truncate">{item.name}</h4>
                                                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Qty: {item.quantity}</p>
                                            </div>
                                            <div className="text-right">
                                                <p className="text-sm font-bold text-slate-900">${(item.price * item.quantity).toFixed(2)}</p>
                                            </div>
                                        </div>
                                    ))}
                                </div>

                                <Separator className="opacity-50"/>

                                <div className="space-y-4">
                                    <div className="flex justify-between items-center">
                                        <span className="text-slate-500 font-medium text-sm">Subtotal</span>
                                        <span className="text-slate-900 font-bold">${cartTotal.toFixed(2)}</span>
                                    </div>
                                    <div className="flex justify-between items-center text-emerald-600">
                                        <span className="font-medium text-sm">Shipping</span>
                                        <span className="font-bold text-[10px] uppercase tracking-widest">Free</span>
                                    </div>
                                    <Separator className="opacity-50"/>
                                    <div className="flex justify-between items-center pt-2">
                                        <span className="text-lg font-bold text-slate-900">Order Total</span>
                                        <span
                                            className="text-3xl font-bold text-blue-600 tracking-tighter">${cartTotal.toFixed(2)}</span>
                                    </div>
                                </div>

                                <div
                                    className="p-4 rounded-lg bg-blue-50 border border-blue-100 flex items-center gap-3">
                                    <div className="p-2 bg-blue-600 rounded-lg text-white">
                                        <Clock className="h-4 w-4"/>
                                    </div>
                                    <div className="space-y-0.5">
                                        <p className="text-[10px] font-bold text-blue-800 uppercase tracking-widest leading-none">Estimated
                                            Delivery</p>
                                        <p className="text-sm font-bold text-blue-900">3-5 Business Days</p>
                                    </div>
                                </div>

                                {isSubmitting ? (
                                    <ButtonLoading className="w-full h-16 rounded-lg bg-blue-600"/>
                                ) : (
                                    <Button
                                        type="submit"
                                        form="checkout-form"
                                        className="w-full h-16 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold text-xl shadow-xl shadow-blue-600/20 transition-all hover:scale-[1.02] active:scale-95 group"
                                    >
                                        Place Order Now
                                        <ChevronRight
                                            className="h-6 w-6 ml-2 group-hover:translate-x-1 transition-transform"/>
                                    </Button>
                                )}

                                <div className="flex items-center justify-center gap-2 pt-2">
                                    <ShieldCheck className="h-4 w-4 text-emerald-500"/>
                                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Encrypted & Secure Transaction</span>
                                </div>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    )
}