import React from "react"
import {Link, useNavigate} from "react-router-dom"

import {Star, ShoppingCart, Trash2} from "lucide-react"
import {Card, CardContent, CardFooter} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {Badge} from "@/components/ui/badge"
import {useCartStore} from "@/store/useCartStore"
import {PERMISSION, TOAST_TYPE} from "@/utils/enums"
import {hasPermission} from "@/utils/index.js";
import { notify } from "@/components/common/notification"
import { useUserStore } from "@/store/useUserStore"

import { cn } from "@/lib/utils"

const ProductCard = ({product}) => {
    const {user} = useUserStore();
    const {addToCart} = useCartStore();
    const navigate = useNavigate();

    const handleAddToCart = (e) => {
        e.preventDefault();
        const response = addToCart(product, 1);
        notify(TOAST_TYPE.SUCCESS, response ? "Added to cart" : "Item increased")
    }

    const deleteProduct = (e) => {
        e.preventDefault();
        // Delete logic
    }

    const navigateToEdit = (e) => {
        e.preventDefault();
        navigate(`/products/${product.id}/edit`)
    }

    return (
        <Link to={`/products/${product.id}`} className="group h-full">
            <Card className="h-[500px] bg-white border-slate-100 shadow-sm hover:shadow-xl hover:shadow-slate-200/50  overflow-hidden transition-all duration-500 cursor-pointer flex flex-col">
                <div className="relative aspect-[4/5] overflow-hidden bg-slate-50">
                    <img
                        src={product.image || "/placeholder.svg"}
                        alt={product.name}
                        className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                        loading="lazy"
                    />
                    
                    <div className="absolute top-4 right-4 flex flex-col gap-2 translate-x-12 group-hover:translate-x-0 transition-transform duration-500">
                        {product.quantity > 0 ? (
                            <button 
                                onClick={handleAddToCart}
                                className="w-10 h-10 rounded-lg bg-white/90 backdrop-blur-md shadow-lg flex items-center justify-center text-blue-600 hover:bg-blue-600 hover:text-white transition-all active:scale-90"
                            >
                                <ShoppingCart className="h-5 w-5" />
                            </button>
                        ) : (
                            <div className="w-10 h-10 rounded-lg bg-red-500/90 backdrop-blur-md shadow-lg flex items-center justify-center text-white">
                                <ShoppingCart className="h-5 w-5 opacity-50" />
                            </div>
                        )}
                        <button className="w-10 h-10 rounded-lg bg-white/90 backdrop-blur-md shadow-lg flex items-center justify-center text-slate-400 hover:text-red-500 transition-all active:scale-90">
                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
                        </button>
                    </div>

                    {product.quantity <= 0 && (
                        <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px] flex items-center justify-center">
                            <span className="px-4 py-2 rounded-full bg-white/90 text-red-600 text-[10px] font-black uppercase tracking-widest shadow-lg">
                                Out of Stock
                            </span>
                        </div>
                    )}
                </div>

                <CardContent className="p-6 flex-1 flex flex-col">
                    <div className="flex items-center gap-2 mb-3">
                        <div className="flex items-center bg-amber-50 px-2 py-0.5 rounded-lg border border-amber-100">
                            <Star className="h-3 w-3 text-amber-400 fill-current" />
                            <span className="text-[10px] font-black text-amber-800 ml-1">{product?.rating || 0}</span>
                        </div>
                        <div className="h-4 w-px bg-slate-100" />
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                            {product.quantity} Left
                        </span>
                    </div>

                    <h3 className="font-semibold text-slate-900 text-lg mb-2 line-clamp-2 leading-tight transition-colors">
                        {product.name}
                    </h3>

                    <div className="flex items-end justify-between gap-4">
                        <div className="flex items-baseline gap-1">
                            <span className="text-sm font-bold text-slate-400">$</span>
                            <span className="text-2xl font-bold text-slate-900 tracking-tighter">{product.price}</span>
                        </div>
                        
                        <div className="flex flex-wrap justify-end gap-1 max-w-[50%]">
                            {product.categories?.slice(0, 1).map((category) => (
                                <Badge key={category} className="bg-blue-50 text-blue-700 hover:bg-blue-100 border-none px-2 py-0.5 rounded-md text-[9px] font-black uppercase tracking-widest whitespace-nowrap">
                                    {category}
                                </Badge>
                            ))}
                        </div>
                    </div>
                </CardContent>

                <CardFooter className="px-6 pb-6 pt-0">
                    {hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS) ? (
                        <div className="flex gap-2 w-full">
                            <Button 
                                variant="outline"
                                className="flex-1 h-10 rounded-lg font-bold text-xs border-slate-200 hover:bg-slate-50"
                                onClick={navigateToEdit}
                            >
                                Edit
                            </Button>
                            <Button 
                                variant="outline"
                                className="h-10 w-10 rounded-lg p-0 border-slate-200 text-red-500 hover:bg-red-50 hover:border-red-100"
                                onClick={deleteProduct}
                            >
                                <Trash2 className="h-4 w-4" />
                            </Button>
                        </div>
                    ) : (
                        <Button 
                            onClick={handleAddToCart}
                            disabled={product.quantity <= 0}
                            className={cn(
                                "w-full h-11 rounded-lg font-black text-xs uppercase tracking-widest transition-all active:scale-95 group/btn",
                                product.quantity > 0 
                                    ? "bg-blue-600 hover:bg-blue-700 text-white shadow-lg shadow-blue-200" 
                                    : "bg-slate-100 text-slate-400 cursor-not-allowed"
                            )}
                        >
                            <ShoppingCart className="h-4 w-4 mr-2 group-hover/btn:rotate-12 transition-transform" />
                            Add to Cart
                        </Button>
                    )}
                </CardFooter>
            </Card>
        </Link>
    )
}

export default React.memo(ProductCard);
