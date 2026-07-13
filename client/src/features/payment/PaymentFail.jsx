import {useSearchParams, useNavigate} from "react-router-dom";
import {XCircle, RefreshCw, Eye, AlertCircle, ShoppingCart} from "lucide-react";

import {Button} from "@/components/ui/button";
import { useUserStore } from "@/store/useUserStore";
import { TOAST_TYPE } from "@/utils/enums";
import { Axios } from "@/services/http/Axios";
import { createPayment } from "@/features/order/OrderCreate";
import { notify } from "@/components/common/notification";
import {Card, CardContent, CardHeader, CardTitle, CardDescription} from "@/components/ui/card";

const tryAgain = async (orderId, userId, navigate) => {
  try {
    const response = await Axios.get(`/orders/${orderId}`);
    const order = response.data?.data;

    // Notify user that payment is being initiated
    notify(TOAST_TYPE.SUCCESS, "Initiating payment retry...");

    const paymentResponse = await createPayment(order, userId, order.phone);

    if (paymentResponse.data.data) {
        window.location.assign(paymentResponse.data.data);
    } else {
        notify(TOAST_TYPE.ERROR, "Failed to get payment URL.");
    }

  } catch (error) {
    console.error("Error occurred while trying again:", error);
    notify(TOAST_TYPE.ERROR, "Failed to initiate payment. Please try again later.");
  }
};

export default function PaymentFail() {
    const navigate = useNavigate();
    const {user} = useUserStore();

    const [params] = useSearchParams();
    const reason = params.get("reason") || "Payment was not completed.";
    const orderId = params.get("orderId") || undefined;

    return (
        <div className="min-h-[80vh] flex items-center justify-center p-4 bg-slate-50/50">
            <Card className="max-w-md w-full border-slate-100 shadow-2xl shadow-slate-200/50 rounded-2xl overflow-hidden bg-white animate-in fade-in zoom-in duration-500">
                <CardHeader className="bg-rose-50/50 border-b border-rose-50 p-8 text-center">
                    <div className="flex justify-center mb-4">
                        <div className="p-3 bg-rose-500 rounded-full text-white shadow-lg shadow-rose-500/20">
                            <XCircle className="h-10 w-10" />
                        </div>
                    </div>
                    <CardTitle className="text-2xl font-bold text-slate-800 tracking-tight">Payment Failed</CardTitle>
                    <CardDescription className="text-rose-700 font-medium mt-1">
                        We couldn't process your payment.
                    </CardDescription>
                </CardHeader>
                
                <CardContent className="p-8 space-y-6">
                    <div className="p-4 rounded-xl bg-rose-50 border border-rose-100 flex items-start gap-3">
                        <AlertCircle className="h-5 w-5 text-rose-500 mt-0.5 flex-shrink-0" />
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-rose-800 uppercase tracking-widest leading-none">Reason</p>
                            <p className="text-sm font-semibold text-rose-900 leading-tight">
                                {reason === "out_of_stock" 
                                    ? "One or more items in your order are no longer available in stock."
                                    : reason}
                            </p>
                        </div>
                    </div>

                    <div className="text-center space-y-2">
                        <p className="text-sm text-slate-500 font-medium">
                            Don't worry, your order has been placed as <b>Pending</b>. You can try paying again or pay via Cash on Delivery.
                        </p>
                    </div>

                    <div className="space-y-3 pt-2">
                        {orderId && (
                            <>
                                <Button 
                                    onClick={() => tryAgain(orderId, user.id, navigate)} 
                                    className="w-full h-12 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow-lg shadow-blue-600/20 transition-all hover:scale-[1.02] active:scale-95 group"
                                >
                                    <RefreshCw className="h-4 w-4 mr-2 group-hover:rotate-180 transition-transform duration-500" />
                                    Try Paying Again
                                </Button>
                                
                                <Button 
                                    variant="outline" 
                                    onClick={() => navigate(`/orders/${orderId}`)} 
                                    className="w-full h-12 border-slate-200 text-slate-700 font-bold rounded-xl hover:bg-slate-50 transition-all"
                                >
                                    <Eye className="h-4 w-4 mr-2" />
                                    View My Order
                                </Button>
                            </>
                        )}
                        
                        <Button 
                            variant="ghost" 
                            onClick={() => navigate("/products")} 
                            className="w-full h-12 text-slate-500 font-bold rounded-xl hover:text-slate-700 transition-all"
                        >
                            <ShoppingCart className="h-4 w-4 mr-2" />
                            Return to Shop
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}