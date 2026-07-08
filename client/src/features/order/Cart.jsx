import { Link } from "react-router-dom"
import { Minus, Plus, Trash2, ShoppingBag, ArrowRight, ShieldCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { useUserStore } from '@/store/useUserStore';
import { useCartStore } from "@/store/useCartStore"

export default function Cart() {
  const { user } = useUserStore();
  const { cart, removeFromCart, updateQuantity, clearCart, getCartTotal } = useCartStore();
  const cartTotal = getCartTotal();

  if (cart.length == 0) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center p-8 text-center bg-slate-50">
        <div className="relative mb-8">
            <div className="absolute -inset-4 bg-blue-100/50 rounded-full blur-2xl" />
            <div className="relative w-24 h-24 bg-gradient-to-br from-blue-500 to-blue-700 rounded-lg rotate-12 flex items-center justify-center shadow-xl">
              <ShoppingBag className="h-12 w-12 text-white transform -rotate-12" />
            </div>
        </div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2 tracking-tight">Your cart is empty</h1>
        <p className="text-slate-500 max-w-[320px] mb-8 font-medium">Looks like you haven't added anything to your cart yet. Let's find some great products for you!</p>
        <Link to="/products">
          <Button className="h-12 px-8 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-200 transition-all hover:scale-105 active:scale-95">
            Start Shopping
          </Button>
        </Link>
      </div>
    )
  }

  return (
    <div className="bg-slate-50 min-h-screen">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 md:py-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
          <div>
            <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Shopping Cart</h1>
            <p className="text-slate-500 font-medium">You have <span className="text-blue-600 font-bold">{cart.length} items</span> in your cart</p>
          </div>
          <Button 
            variant="ghost" 
            onClick={clearCart}
            className="text-red-600 hover:text-red-700 hover:bg-red-50 font-bold text-sm w-fit rounded-lg"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Clear Cart
          </Button>
        </div>

        <div className="grid lg:grid-cols-3 gap-10 items-start">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {cart?.map((item) => (
              <div 
                key={item.productId}
                className="group relative bg-white rounded-lg border border-slate-100 p-4 md:p-6 shadow-sm hover:shadow-md transition-all duration-300"
              >
                <div className="flex flex-col sm:flex-row items-center gap-6">
                  <div className="relative w-32 h-32 flex-shrink-0 overflow-hidden rounded-lg bg-slate-50">
                    <img
                      src={item.image || item.images?.[0] || "/placeholder.svg"}
                      alt={item.name}
                      className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                    />
                  </div>

                  <div className="flex-1 min-w-0 text-center sm:text-left">
                    <div className="flex flex-col md:flex-row md:items-start justify-between gap-2 mb-2">
                      <Link to={`/products/${item.productId}`}>
                        <h3 className="text-lg font-bold text-slate-900 hover:text-blue-600 transition-colors truncate">
                          {item.name}
                        </h3>
                      </Link>
                      <p className="text-xl font-bold text-slate-900">${item.price}</p>
                    </div>
                    
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">
                      {item?.category?.name || "Product"}
                    </p>

                    <div className="flex items-center justify-center sm:justify-start gap-6">
                      <div className="flex items-center gap-1 bg-slate-50 p-1 rounded-lg border border-slate-100">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 rounded-lg hover:bg-white hover:shadow-sm"
                          onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                          disabled={item.quantity <= 1}
                        >
                          <Minus className="h-3 w-3" />
                        </Button>
                        <span className="w-8 text-center font-bold text-sm text-slate-800">{item.quantity}</span>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 rounded-lg hover:bg-white hover:shadow-sm"
                          onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                          disabled={item.quantity >= item.stock}
                        >
                          <Plus className="h-3 w-3" />
                        </Button>
                      </div>

                      <button 
                        onClick={() => removeFromCart(item.productId)}
                        className="text-slate-400 hover:text-red-500 transition-colors p-2"
                      >
                        <Trash2 className="h-5 w-5" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg border border-slate-100 p-8 shadow-xl shadow-slate-200/50 sticky top-24">
              <h2 className="text-2xl font-bold text-slate-900 mb-8 tracking-tight">Order Summary</h2>
              
              <div className="space-y-4 mb-8">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium text-sm">Subtotal ({cart.reduce((sum, item) => sum + item.quantity, 0)} items)</span>
                  <span className="text-slate-900 font-bold">${cartTotal.toFixed(2)}</span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium text-sm">Shipping</span>
                  <span className="text-emerald-600 font-bold text-sm uppercase tracking-widest">Free</span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium text-sm">Tax Estimate</span>
                  <span className="text-slate-900 font-bold">$0.00</span>
                </div>

                <Separator className="opacity-50" />

                <div className="flex justify-between items-center pt-2">
                  <span className="text-lg font-bold text-slate-900">Total</span>
                  <span className="text-2xl font-bold text-blue-600 tracking-tight">${cartTotal.toFixed(2)}</span>
                </div>
              </div>

              {user?.id && (
                <Link to="/orders/create">
                  <Button className="w-full h-14 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-bold text-lg shadow-xl shadow-blue-600/20 transition-all hover:scale-[1.02] active:scale-95 group mb-4">
                    Checkout Now
                    <ArrowRight className="h-5 w-5 ml-2 group-hover:translate-x-1 transition-transform" />
                  </Button>
                </Link>
              )}
              
              <div className="flex items-center justify-center gap-2 py-4 px-6 rounded-lg bg-slate-50 border border-slate-100">
                <ShieldCheck className="h-5 w-5 text-emerald-500" />
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-widest">Secure Checkout</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
