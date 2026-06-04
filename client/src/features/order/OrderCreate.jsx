import { useEffect, useState } from "react"
import { useLocation, useNavigate, useParams } from "react-router-dom"
import { CreditCard, Package } from "lucide-react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Separator } from "@/components/ui/separator"
import { PAYMENT_METHOD } from "@/utils/enums"
import { useCartStore } from "@/store/useCartStore"
import StaredLabel from "@/components/common/StaredLabel"
import { useUserStore } from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import { Axios } from "@/services/http/Axios"
import { URL_NOT_FOUND } from "@/utils"
import { ButtonLoading } from "@/components/common/ButtonLoading"

const checkoutSchema = z.object({
    phone: z.string()
        .min(11, 'Phone number must be 11 digits')
        .max(11, 'Phone number must be 11 digits'),
    address: z.string().min(5, 'Address must be at least 5 characters'),
    paymentMethod: z.nativeEnum(PAYMENT_METHOD),
    cardNumber: z.string().optional(),
    expiry: z.string().optional(),
    cvv: z.string().optional(),
    cardName: z.string().optional(),
}).refine((data) => {
    if (data.paymentMethod === PAYMENT_METHOD.CARD) {
        return !!data.cardNumber && !!data.expiry && !!data.cvv && !!data.cardName;
    }
    return true;
}, {
    message: "Card details are required for card payment",
    path: ["cardNumber"],
});

export default function OrderCreate() {
    const navigate = useNavigate();

    const { user } = useUserStore();
    const { cart, getCartTotal, clearCart } = useCartStore();
    const cartTotal = getCartTotal();

    const [isLoading, setIsLoading] = useState({ page: false, button: false });

    const {
        register,
        handleSubmit,
        setValue,
        watch,
        formState: { errors },
    } = useForm({
        resolver: zodResolver(checkoutSchema),
        defaultValues: {
            phone: '',
            address: '',
            paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
        },
    });

    const paymentMethod = watch('paymentMethod');

    const handleError = (error) => {
        console.error(error);
        if ([403, 404].includes(error?.status)) navigate(URL_NOT_FOUND, { replace: true });
    };

    const onSubmit = async (data) => {
        setIsLoading({ ...isLoading, button: true });

        try {
            await Axios.post('/orders', {
                items: cart,
                phone: data.phone,
                address: data.address,
                paymentMethod: data.paymentMethod,
            });

            clearCart();
            navigate("/orders");
        } catch (error) {
            handleError(error);
        } finally {
            setIsLoading({ ...isLoading, button: false });
        }
    };

    return (
        <>
            {isLoading.page && <PageLoadingOverlay />}

            <div className="container pb-8">
                <div className="max-w-4xl mx-auto">
                    <div className="grid gap-8 lg:grid-cols-2">
                        {/* Checkout Form */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <Package className="h-5 w-5" />
                                    Checkout
                                </CardTitle>
                                <CardDescription>Complete your order by filling out the information below</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-6">
                                <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                                    <div className="space-y-4">
                                        <div className="grid gap-2">
                                            <Label>Name</Label>
                                            <InputReadOnly value={user?.name} />
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Phone Number" />
                                            <Input 
                                                type="tel" 
                                                placeholder="+1 (555) 123-4567"
                                                {...register('phone')}
                                            />
                                            {errors.phone && <p className='validation-error text-red-500 text-xs'>{errors.phone.message}</p>}
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Street Address" />
                                            <Input 
                                                placeholder="123 Main Street"
                                                {...register('address')}
                                            />
                                            {errors.address && <p className='validation-error text-red-500 text-xs'>{errors.address.message}</p>}
                                        </div>
                                    </div>

                                    <Separator />

                                    {/* Payment Method */}
                                    <div className="space-y-4">
                                        <div className="flex items-center gap-2">
                                            <CreditCard className="h-4 w-4" />
                                            <h3 className="font-semibold">Payment Method</h3>
                                        </div>
                                        <RadioGroup 
                                            value={paymentMethod} 
                                            onValueChange={(value) => setValue('paymentMethod', value)}
                                        >
                                            {Object.values(PAYMENT_METHOD).map((item) => (
                                                <div key={item} className="flex items-center space-x-2">
                                                    <RadioGroupItem value={item} />
                                                    <Label htmlFor={item}>{item}</Label>
                                                </div>
                                            ))}
                                        </RadioGroup>

                                        {paymentMethod === PAYMENT_METHOD.CARD && (
                                            <div className="space-y-4 pt-4">
                                                <div className="grid gap-2">
                                                    <StaredLabel label="Card Number" />
                                                    <Input placeholder="1234 5678 9012 3456" {...register('cardNumber')} />
                                                    {errors.cardNumber && <p className='validation-error text-red-500 text-xs'>{errors.cardNumber.message}</p>}
                                                </div>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="grid gap-2">
                                                        <StaredLabel label="Expiry Date" />
                                                        <Input placeholder="MM/YY" {...register('expiry')} />
                                                    </div>
                                                    <div className="grid gap-2">
                                                        <StaredLabel label="CVV" />
                                                        <Input placeholder="123" {...register('cvv')} />
                                                    </div>
                                                </div>
                                                <div className="grid gap-2">
                                                    <StaredLabel label="Name on Card" />
                                                    <Input placeholder="John Doe" {...register('cardName')} />
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                    
                                    {isLoading.button ?
                                        <ButtonLoading />
                                        :
                                        <Button type="submit" className="w-full bg-blue-600" size="lg">
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
                                {/* View Items Button */}
                                <Dialog>
                                    <DialogTrigger asChild>
                                        <Button variant="outline" className="w-full">
                                            View Items
                                        </Button>
                                    </DialogTrigger>
                                    <DialogContent className="max-w-2xl">
                                        <DialogHeader>
                                            <DialogTitle>Order Items</DialogTitle>
                                            <DialogDescription>Review the items in your order</DialogDescription>
                                        </DialogHeader>
                                        <div className="space-y-4">
                                            {cart?.map((item) => (
                                                <div key={item.id} className="flex items-center gap-4 p-4 border rounded-lg">
                                                    <img
                                                        src={item?.images[0]?.image || "/placeholder.svg"}
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

                                <Separator />

                                {/* Order Totals */}
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

                                    <Separator />

                                    <div className="flex justify-between font-bold text-lg">
                                        <span>Total</span>
                                        <span>${cartTotal.toFixed(2)}</span>
                                    </div>
                                </div>

                                {/* Estimated Delivery */}
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
