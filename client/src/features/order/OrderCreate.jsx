import {useNavigate} from "react-router-dom"
import {Package} from "lucide-react"
import {useForm, Controller} from "react-hook-form"
import {zodResolver} from "@hookform/resolvers/zod"
import * as z from "zod"

import {Button} from "@/components/ui/button"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "@/components/ui/dialog"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.jsx"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {Separator} from "@/components/ui/separator"
import {useCartStore} from "@/store/useCartStore"
import StaredLabel from "@/components/common/StaredLabel"
import {useUserStore} from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import {Axios} from "@/services/http/Axios"
import {GLOBAL_ERROR, handleErrors} from "@/utils"
import {ButtonLoading} from "@/components/common/ButtonLoading"
import InputError from "@/components/common/InputError.jsx"
import {PAYMENT_METHOD, TOAST_TYPE} from "@/utils/enums.js"
import {getIdempotencyKey, IDEMPOTENCY_HEADER, removeIdempotencyKey} from "@/utils/idempotencyUtil.js"
import {notify} from "@/components/common/notification"

const checkoutSchema = z.object({
    phone: z.string()
        .min(11, 'Phone number must be 11 digits')
        .max(11, 'Phone number must be 11 digits'),
    address: z.string().min(5, 'Address must be at least 5 characters'),
    paymentMethod: z.string().min(1, 'Please select a payment method'),
})

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

    const createOrder = async (data, idempotencyKey) => {
        try {
            return Axios.post("/orders", {
                    items: cart,
                    name: user?.name,
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
            notify(TOAST_TYPE.ERROR, "Order placed Failed!");
        }
    };

    const createPayment = async (order, phone) => {
        try {
            return await Axios.post("/payment", {
                userId: user.id,
                orderId: order.id,
                amount: order.amount,
                payerReference: phone,
            });
        } catch (error) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Payment Failed!");
        }
    };

    const cleanupAfterOrder = () => {
        clearCart();
        reset();
        removeIdempotencyKey();
    };

    const saveOrder = async (data) => {
        const idempotencyKey = getIdempotencyKey();

        try {
            const orderResponse = await createOrder(data, idempotencyKey);
            const order = orderResponse.data.data;


            if (data.paymentMethod == PAYMENT_METHOD.CASH_ON_DELIVERY) {
                cleanupAfterOrder();
                notify(TOAST_TYPE.SUCCESS, "Order placed successfully!");
                navigate(`/orders/${order.id}`);
                return;
            }

            const paymentResponse = await createPayment(order, data.phone);
            console.log(paymentResponse.data.data);

            cleanupAfterOrder();
            notify(TOAST_TYPE.SUCCESS, "Order placed and payment completed successfully!");
            window.location.assign(paymentResponse.data.data);
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    };

    return (
        <>
            <div className="container pb-8">
                <div className="max-w-4xl mx-auto">
                    <div className="grid gap-8 lg:grid-cols-2">
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <Package className="h-5 w-5"/>
                                    Checkout
                                </CardTitle>
                                <CardDescription>Complete your order by filling out the information below</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-6">
                                <form className="space-y-6" onSubmit={handleSubmit(saveOrder)}>
                                    <div className="space-y-4">
                                        <InputError errors={errors} field={GLOBAL_ERROR}/>

                                        <div className="grid gap-2">
                                            <Label>Name</Label>
                                            <InputReadOnly value={user?.name}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Phone Number"/>
                                            <Input
                                                type="tel"
                                                placeholder="+1 (555) 123-4567"
                                                {...register('phone')}
                                            />
                                            <InputError errors={errors} field={"phone"}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Street Address"/>
                                            <Input
                                                placeholder="123 Main Street"
                                                {...register('address')}
                                            />
                                            <InputError errors={errors} field={"address"}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Payment Method"/>
                                            <Controller
                                                name="paymentMethod"
                                                control={control}
                                                render={({field}) => (
                                                    <Select value={field.value} onValueChange={field.onChange}>
                                                        <SelectTrigger className="w-full">
                                                            <SelectValue placeholder="Select payment method"/>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            {Object.entries(PAYMENT_METHOD).map(([key, label]) => (
                                                                <SelectItem key={key} value={label}>
                                                                    {label}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                )}
                                            />
                                            <InputError errors={errors} field={"paymentMethod"}/>
                                        </div>
                                    </div>

                                    {isSubmitting
                                        ? <ButtonLoading/>
                                        : <Button type="submit" className="w-full bg-blue-600" size="lg">
                                            Complete Order
                                        </Button>
                                    }
                                </form>
                            </CardContent>
                        </Card>

                        {/* Order Summary */}
                        <Card className='h-fit'>
                            <CardHeader>
                                <CardTitle>Order Summary</CardTitle>
                                <CardDescription>Review your items and total</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <Dialog>
                                    <DialogTrigger asChild>
                                        <Button variant="outline" className="w-full">View Items</Button>
                                    </DialogTrigger>
                                    <DialogContent className="max-w-2xl">
                                        <DialogHeader>
                                            <DialogTitle>Order Items</DialogTitle>
                                            <DialogDescription>Review the items in your order</DialogDescription>
                                        </DialogHeader>
                                        <div className="space-y-4">
                                            {cart?.map((item) => (
                                                <div key={item.productId}
                                                     className="flex items-center gap-4 p-4 border rounded-lg">
                                                    <img
                                                        src={item.image || "/placeholder.svg"}
                                                        alt={item.name}
                                                        className="w-16 h-16 object-cover rounded-md"
                                                        loading="lazy"
                                                    />
                                                    <div className="flex-1">
                                                        <h4 className="font-medium">{item.name}</h4>
                                                        <p className="text-sm text-muted-foreground">Quantity: {item.quantity}</p>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="font-medium">${(item.price * item.quantity).toFixed(2)}</p>
                                                        <p className="text-sm text-muted-foreground">${item?.price} each</p>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </DialogContent>
                                </Dialog>

                                <Separator/>

                                <div className="space-y-2">
                                    <div className="flex justify-between">
                                        <span>Subtotal ({cart.reduce((sum, item) => sum + item.quantity, 0)} items)</span>
                                        <span>${cartTotal.toFixed(2)}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Shipping</span>
                                        <span>0</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Discount</span>
                                        <span>0</span>
                                    </div>
                                    <Separator/>
                                    <div className="flex justify-between font-bold text-lg">
                                        <span>Total</span>
                                        <span>${cartTotal.toFixed(2)}</span>
                                    </div>
                                </div>

                                <div className="bg-muted p-4 rounded-lg">
                                    <h4 className="font-medium mb-2">Estimated Delivery</h4>
                                    <p className="text-sm text-muted-foreground">3-5 business days</p>
                                </div>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        </>
    )
}