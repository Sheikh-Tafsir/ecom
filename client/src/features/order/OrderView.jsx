import { useEffect, useState } from "react"
import { useParams } from "react-router-dom"
import { Package } from "lucide-react"

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
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import StaredLabel from "@/components/common/StaredLabel"
import { useUserStore } from "@/store/useUserStore"
import InputReadOnly from "@/components/common/InputReadOnly"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import { Axios } from "@/services/http/Axios"
import {toastInitialState} from "@/utils"
import { TOAST_TYPE } from "@/utils/enums"

export default function OrderView() {
    const { id } = useParams();
    const { user } = useUserStore();

    const [isPageLoading, setIsPageLoading] = useState(true);
    const [order, setOrder] = useState();
    const [toastData, setToastData] = useState(toastInitialState);

    useEffect(() => {
        const fetchOrder = async (id) => {
            try {
                const response = await Axios.get(`/orders/${id}`);
                setOrder(response.data.data);
            } catch (error) {
                console.error(error)
                showToast("Could not get order", TOAST_TYPE.ERROR);
            } finally {
                setIsPageLoading(false);
            }
        }

        fetchOrder(id);
    }, [id])


    const showToast = (message, type) => {
        setToastData({ message, type, id: Date.now() })
    }

    return (
        <>
            {isPageLoading && <PageLoadingOverlay />}

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
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Street Address" />
                                            <InputReadOnly value={order?.address} />
                                        </div>
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

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    )
}
