import { useEffect, useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { CreditCard, Package } from "lucide-react"

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
import StaredLabel from "@/components/common/StaredLabel"
import { useUserStore } from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import { Axios } from "@/services/http/Axios"
import { URL_NOT_FOUND } from "@/utils"

export default function OrderView() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user } = useUserStore();

    const [isLoading, setIsLoading] = useState({ page: true, button: false });
    const [order, setOrder] = useState({
        phone: '',
        paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
        address: '',
        city: '',
        zip: '',
    });
    const [errors, setErrors] = useState({});

    useEffect(() => {
        const fetchOrder = async (id) => {
            try {
                const response = await Axios.get(`/orders/${id}`);
                //console.log(response.data.data)
                setOrder(response.data.data);
            } catch (error) {
                handleError(error);
            } finally {
                setIsLoading({ ...isLoading, page: false });
            }
        }

        fetchOrder(id);
    }, [id])

    const handleError = (error) => {
        console.error(error);
        if ([403, 404].includes(error?.status)) navigate(URL_NOT_FOUND, { replace: true });
        setErrors(error.response?.data || { global: error.message });
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setOrder((prev) => ({ ...prev, [name]: value }));
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
                                <div className="space-y-6">
                                    <div className="space-y-4">
                                        <div className="grid gap-2">
                                            <Label>Name</Label>
                                            <InputReadOnly value={user?.name} />
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Phone Number" />
                                            <InputReadOnly value={order?.phone} />
                                            {errors.phone && <p className='validation-error'>{errors.phone}</p>}
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Street Address" />
                                            <InputReadOnly value={order?.address} />
                                            {errors.address && <p className='validation-error'>{errors.address}</p>}
                                        </div>
                                    </div>

                                    <Separator />

                                    {/* Payment Method */}
                                    <div className="space-y-4">
                                        <div className="flex items-center gap-2">
                                            <CreditCard className="h-4 w-4" />
                                            <h3 className="font-semibold">Payment Method</h3>
                                        </div>
                                        <RadioGroup value={order.paymentMethod} onValueChange={(value) => setOrder((prev) => ({ ...prev, "paymentMethod": value }))}>
                                            {Object.values(PAYMENT_METHOD).map((item) => (
                                                <div key={item} className="flex items-center space-x-2">
                                                    <RadioGroupItem value={item} />
                                                    <Label htmlFor={item}>{item}</Label>
                                                </div>
                                            ))}
                                        </RadioGroup>

                                        {order?.paymentMethod === PAYMENT_METHOD.CARD && (
                                            <div className="space-y-4 pt-4">
                                                <div className="grid gap-2">
                                                    <StaredLabel label="Card Number" />
                                                    <Input id="cardNumber" placeholder="1234 5678 9012 3456" required />
                                                </div>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="grid gap-2">
                                                        <StaredLabel label="Expiry Date" />
                                                        <Input id="expiry" placeholder="MM/YY" required />
                                                    </div>
                                                    <div className="grid gap-2">
                                                        <StaredLabel label="CVV" />
                                                        <Input id="cvv" placeholder="123" required />
                                                    </div>
                                                </div>
                                                <div className="grid gap-2">
                                                    <StaredLabel label="Name on Card" />
                                                    <Input id="cardName" placeholder="John Doe" required />
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
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
                                            {order?.items?.map((item) => (
                                                <div key={item.id} className="flex items-center gap-4 p-4 border rounded-lg">
                                                    <img
                                                        src={item?.product?.images[0]?.image || "/placeholder.svg"}
                                                        alt={item.name}
                                                        className="w-16 h-16 object-cover rounded-md"
                                                    />
                                                    <div className="flex-1">
                                                        <h4 className="font-medium">{item.product.name}</h4>
                                                        <p className="text-sm text-muted-foreground">Quantity: {item.quantity}</p>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="font-medium">{(item.product.price * item.quantity).toFixed(2)}</p>
                                                        <p className="text-sm text-muted-foreground">${item.product.price} each</p>
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
                                        <span>Subtotal ({order?.items?.length} items)</span>
                                        <span>${order?.orderPrice}</span>
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
                                        <span>${order?.totalPrice}</span>
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
