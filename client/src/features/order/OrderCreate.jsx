import {useState} from "react"
import {useNavigate} from "react-router-dom"
import {Package} from "lucide-react"
import {useForm} from "react-hook-form"
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
    DialogTrigger,
} from "@/components/ui/dialog"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {Separator} from "@/components/ui/separator"
import {useCartStore} from "@/store/useCartStore"
import StaredLabel from "@/components/common/StaredLabel"
import {useUserStore} from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import {Axios} from "@/services/http/Axios"
import {GLOBAL_ERROR, handleErrors, toastInitialState} from "@/utils"
import {ButtonLoading} from "@/components/common/ButtonLoading"
import InputError from "@/components/common/InputError.jsx";
import {TOAST_TYPE} from "@/utils/enums.js";
import {ToastAlert} from "@/components/common/ToastAlert.jsx";
import {
    getIdempotencyKey, IDEMPOTENCY_HEADER,
    removeIdempotencyKey
} from "@/utils/idempotencyUtil.js";

const checkoutSchema = z.object({
    phone: z.string()
        .min(11, 'Phone number must be 11 digits')
        .max(11, 'Phone number must be 11 digits'),
    address: z.string().min(5, 'Address must be at least 5 characters'),
})

export default function OrderCreate() {
    const navigate = useNavigate();

    const {user} = useUserStore();
    const {cart, getCartTotal, clearCart} = useCartStore();
    const cartTotal = getCartTotal();

    const [toastData, setToastData] = useState(toastInitialState);

    const {
        register,
        handleSubmit,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(checkoutSchema),
        defaultValues: {
            phone: '',
            address: '',
        },
    });

    const saveOrder = async (data) => {
        const idempotencyKey = getIdempotencyKey();

        try {
            const response = await Axios.post('/orders', {
                items: cart,
                ...data,
            }, {
                headers: {
                    [IDEMPOTENCY_HEADER]: idempotencyKey,
                },
            });

            clearCart();
            removeIdempotencyKey()

            showToast("Successfully updated", TOAST_TYPE.SUCCESS);

            setTimeout(() => {
                navigate(`/orders/${response.data.data}`);
            }, 500);
        } catch (error) {
            console.error(error)
            handleErrors(error, setError);
        }
    };

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now(),});
    };

    return (
        <>
            <div className="container pb-8">
                <div className="max-w-4xl mx-auto">
                    <div className="grid gap-8 lg:grid-cols-2">
                        {/* Checkout Form */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <Package className="h-5 w-5"/>
                                    Checkout
                                </CardTitle>
                                <CardDescription>Complete your order by filling out the information
                                    below</CardDescription>
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
                                    </div>

                                    {isSubmitting ?
                                        <ButtonLoading/>
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
                                                <div key={item.id}
                                                     className="flex items-center gap-4 p-4 border rounded-lg">
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

                                <Separator/>

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

                                    <Separator/>

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

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    )
}
