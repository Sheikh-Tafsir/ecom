import {useEffect, useState} from "react"
import {useParams} from "react-router-dom"
import {Package} from "lucide-react"

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
import {TOAST_TYPE} from "@/utils/enums"
import {notify} from "@/components/common/notification"
import {useQuery} from "@tanstack/react-query"
import { BackButton } from "@/components/common/BackButton"

const fetchOrder = async (id) => {
    const response = await Axios.get(`/orders/${id}`);
    return response.data.data;
}

export default function OrderDetails() {
    const {id} = useParams()
    const {user} = useUserStore();

    const {
        data: order = [],
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
        notify(TOAST_TYPE.ERROR, "Failed to show order");
    }, [isError, error]);

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container py-10">
                <BackButton url="/orders"/>

                <div className="max-w-4xl mx-auto mt-2">
                    <div className="grid gap-8 lg:grid-cols-2">
                        {/* Checkout Form */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <Package className="h-5 w-5"/>
                                    Checkout
                                </CardTitle>
                                <CardDescription>Complete your order by filling out the information below</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-6">
                                <div className="space-y-6">
                                    <div className="space-y-4">
                                        <div className="grid gap-2">
                                            <Label>Name</Label>
                                            <InputReadOnly value={user?.name}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Phone Number"/>
                                            <InputReadOnly value={order?.phone}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Street Address"/>
                                            <InputReadOnly value={order?.address}/>
                                        </div>

                                        <div className="grid gap-2">
                                            <StaredLabel label="Payment Method"/>
                                            <InputReadOnly value={order?.paymentMethod}/>
                                        </div>
                                        <div className="grid gap-2">
                                            <Label>Status</Label>
                                            <InputReadOnly value={order?.status}/>
                                        </div>
                                        <div className="grid gap-2">
                                            <Label>Paid</Label>
                                            <InputReadOnly value={order?.paid ? 'Yes' : 'No'}/>
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
                                                <div key={item.id}
                                                     className="flex items-center gap-4 p-4 border rounded-lg">
                                                    <img
                                                        src={item?.productImage || "/placeholder.svg"}
                                                        alt={item.name}
                                                        className="w-16 h-16 object-cover rounded-md"
                                                    />
                                                    <div className="flex-1">
                                                        <h4 className="font-medium">{item?.productName}</h4>
                                                        <p className="text-sm text-muted-foreground">Quantity: {item.quantity}</p>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="font-medium">{(item.productPrice * item.quantity).toFixed(2)}</p>
                                                        <p className="text-sm text-muted-foreground">${item.productPrice} each</p>
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

                                    <Separator/>

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
