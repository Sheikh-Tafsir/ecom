import {useNavigate, useSearchParams} from "react-router-dom";
import {CheckCircle2, ShoppingBag, ArrowRight, Receipt} from "lucide-react";

import {Button} from "@/components/ui/button";
import {Card, CardContent, CardHeader, CardTitle, CardDescription} from "@/components/ui/card";
import {Separator} from "@/components/ui/separator";

export default function PaymentSuccess() {
    const navigate = useNavigate();

    const [params] = useSearchParams();
    const paymentID = params.get("paymentID");
    const trxID = params.get("trxID");
    const amount = params.get("amount");
    const orderId = params.get("orderId") || undefined;

    return (
        <div className="min-h-[80vh] flex items-center justify-center p-4 bg-slate-50/50">
            <Card className="max-w-md w-full border-slate-100 shadow-2xl shadow-slate-200/50 rounded-2xl overflow-hidden bg-white animate-in fade-in zoom-in duration-500">
                <CardHeader className="bg-emerald-50/50 border-b border-emerald-50 p-8 text-center">
                    <div className="flex justify-center mb-4">
                        <div className="p-3 bg-emerald-500 rounded-full text-white shadow-lg shadow-emerald-500/20">
                            <CheckCircle2 className="h-10 w-10" />
                        </div>
                    </div>
                    <CardTitle className="text-2xl font-bold text-slate-800 tracking-tight">Payment Successful!</CardTitle>
                    <CardDescription className="text-emerald-700 font-medium mt-1">
                        Your transaction has been completed successfully.
                    </CardDescription>
                </CardHeader>
                
                <CardContent className="p-8 space-y-6">
                    <div className="bg-slate-50 rounded-xl p-5 border border-slate-100 space-y-4">
                        <div className="flex items-center gap-3 mb-2">
                            <Receipt className="h-4 w-4 text-slate-400" />
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Transaction Details</span>
                        </div>
                        
                        <div className="flex justify-between items-center text-sm">
                            <span className="text-slate-500 font-medium">Payment ID</span>
                            <span className="text-slate-900 font-bold font-mono">{paymentID}</span>
                        </div>
                        
                        <div className="flex justify-between items-center text-sm">
                            <span className="text-slate-500 font-medium">Transaction ID</span>
                            <span className="text-slate-900 font-bold font-mono">{trxID}</span>
                        </div>
                        
                        <Separator className="opacity-50" />
                        
                        <div className="flex justify-between items-center pt-2">
                            <span className="text-slate-800 font-bold">Total Amount Paid</span>
                            <span className="text-2xl font-bold text-emerald-600 tracking-tighter">৳{amount}</span>
                        </div>
                    </div>

                    <div className="space-y-3 pt-2">
                        {orderId && (
                            <Button 
                                onClick={() => navigate(`/orders/${orderId}`)} 
                                className="w-full h-12 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-all hover:scale-[1.02] active:scale-95 group"
                            >
                                View Order Details
                                <ArrowRight className="h-4 w-4 ml-2 group-hover:translate-x-1 transition-transform" />
                            </Button>
                        )}
                        
                        <Button 
                            variant="outline" 
                            onClick={() => navigate("/products")} 
                            className="w-full h-12 border-slate-200 text-slate-600 font-bold rounded-xl hover:bg-slate-50 transition-all"
                        >
                            <ShoppingBag className="h-4 w-4 mr-2" />
                            Continue Shopping
                        </Button>
                    </div>

                    <p className="text-center text-[10px] text-slate-400 font-medium uppercase tracking-widest pt-2">
                        A confirmation email has been sent to your inbox.
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}