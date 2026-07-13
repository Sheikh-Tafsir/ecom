import {useEffect, useState} from "react"
import {useParams} from "react-router-dom"
import {useQueryClient} from "@tanstack/react-query";

import {Star, ShoppingCart, Minus, Plus} from "lucide-react"
import {Button} from "@/components/ui/button"
import {Badge} from "@/components/ui/badge"
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card"
import {Separator} from "@/components/ui/separator"
import {
    Carousel,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from "@/components/ui/carousel"
import {useCartStore} from '@/store/useCartStore'
import {Axios} from "@/services/http/Axios"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import {ScrollArea} from "@/components/ui/scroll-area"
import ReviewCard from "./ReviewCard"
import {TOAST_TYPE} from "@/utils/enums"
import {useQuery} from "@tanstack/react-query"
import {notify} from "@/components/common/notification"
import ReviewCreate from "./ReviewCreate";
import { BackButton } from "@/components/common/BackButton";

const fetchProduct = async (id) => {
    const response = await Axios.get(`/products/${id}`);
    return response.data.data;
};

const fetchReviews = async (id) => {
    const response = await Axios.get(`/products/${id}/reviews`);
    return response.data.data;
}

export default function ProductDetails() {
    const {id} = useParams()
    const {cart, addToCart} = useCartStore();
    const queryClient = useQueryClient();

    const [quantity, setQuantity] = useState(1);

    const {
        data: product,
        isFetching: isProductLoading,
        isError: isProductError,
        error: productError,
    } = useQuery({
        enabled: !!id,
        queryKey: ["product", id],
        queryFn: () => fetchProduct(id),
    });

    const {
        data: reviewsData,
        isFetching: isReviewsLoading,
        isError: isReviewsError,
        error: reviewsError,
        refetch: reviewsRefetch
    } = useQuery({
        enabled: !!id,
        queryKey: ["reviews", id], //product id
        queryFn: () => fetchReviews(id),
    });

    const reviews = reviewsData?.content ?? [];

    const itemInCart = product?.id ? cart.find(item => item.productId == product.id) : null;
    const inCartQuantity = itemInCart ? itemInCart.quantity : 0;
    const maxAvailable = product?.quantity ? (product.quantity - inCartQuantity) : 0;

    const handleAddToCart = () => {
        addToCart(product, quantity);
        setQuantity(1);
        notify(TOAST_TYPE.SUCCESS, "Product added")
    }

    const incrementQuantity = () => {
        if (quantity < maxAvailable) {
            setQuantity(quantity + 1)
        }
    }

    const decrementQuantity = () => {
        if (quantity > 1) {
            setQuantity(quantity - 1)
        }
    }

    useEffect(() => {
        if (!isProductError) return;

        console.error(productError);
        notify(TOAST_TYPE.ERROR, "Failed to show product");
    }, [isProductError, productError]);

    useEffect(() => {
        if (!isReviewsError) return;

        console.error(reviewsError);
        notify(TOAST_TYPE.ERROR, "Failed to show reviews");
    }, [isReviewsError, reviewsError]);

    if (!isProductLoading && !product) {
        return (
            <div className="container mx-auto px-4 py-8 bg-gradient-to-br from-blue-50 to-indigo-100 ">
                <div className="text-center">
                    <h1 className="text-2xl font-bold mb-4">Product Not Found</h1>
                    <p className="text-gray-600">The product you`&apos`re looking for doesn`&apos`t exist.</p>
                </div>
            </div>
        )
    }

    return (
        <div className="bg-slate-50 min-h-screen">
            {isProductLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-4 md:py-10">
                <BackButton url="/products"/>
                
                <div className="grid md:grid-cols-2 gap-12 lg:gap-20 items-start mb-20 pt-4">
                    {/* Left: Image Gallery */}
                    <div className="space-y-6 animate-in fade-in slide-in-from-left duration-700">
                        <div
                            className="relative group overflow-hidden rounded-[2.5rem] bg-white p-4 shadow-xl shadow-slate-200/50 border border-slate-100">
                            <Carousel className="w-full">
                                <CarouselContent>
                                    {product?.images?.map((image) => (
                                        <CarouselItem key={image.id}>
                                            <div
                                                className="aspect-[4/5] md:aspect-square flex items-center justify-center overflow-hidden rounded-3xl">
                                                <img
                                                    src={image.image || "https://media.istockphoto.com/id/1147544807/vector/thumbnail-image-vector-graphic.jpg?s=1024x1024&w=is&k=20&c=5aen6wD1rsiMZSaVeJ9BWM4GGh5LE_9h97haNpUQN5I="}
                                                    alt={product.name}
                                                    className="object-cover w-full h-full transition-transform duration-700 group-hover:scale-110"
                                                />
                                            </div>
                                        </CarouselItem>
                                    ))}
                                </CarouselContent>
                                <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex items-center gap-2">
                                    <CarouselPrevious
                                        className="relative left-0 translate-x-0 h-10 w-10 border-slate-200 bg-white/80 backdrop-blur-md hover:bg-white"/>
                                    <CarouselNext
                                        className="relative right-0 translate-x-0 h-10 w-10 border-slate-200 bg-white/80 backdrop-blur-md hover:bg-white"/>
                                </div>
                            </Carousel>
                        </div>
                    </div>

                    {/* Right: Product Info */}
                    <div className="flex flex-col animate-in fade-in slide-in-from-right duration-700">
                        <div className="mb-8">
                            <div className="flex items-center gap-3 mb-6">
                                {product?.categories?.map((category) => (
                                    <Badge key={category.id} variant="secondary"
                                           className="bg-blue-50 text-blue-700 hover:bg-blue-100 border-none px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest">
                                        {category.name}
                                    </Badge>
                                ))}
                                {product?.quantity > 0 && (
                                    <Badge
                                        className="bg-emerald-50 text-emerald-700 border-none px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest ml-auto">
                                        In Stock
                                    </Badge>
                                )}
                            </div>

                            <h1 className="text-4xl md:text-5xl font-black text-slate-900 mb-4 tracking-tight leading-tight">{product?.name}</h1>

                            <div className="flex items-center gap-4 mb-8">
                                <div
                                    className="flex items-center bg-amber-50 px-3 py-1.5 rounded-xl border border-amber-100 shadow-sm">
                                    <div className="flex items-center gap-0.5">
                                        {[...Array(5)].map((_, i) => (
                                            <Star
                                                key={i}
                                                className={`h-4 w-4 ${i < Math.floor(product?.rating) ? "text-amber-400 fill-current" : "text-slate-200"
                                                }`}
                                            />
                                        ))}
                                    </div>
                                    <span className="text-amber-800 font-black text-sm ml-2">
                                        {product?.rating || 0}
                                    </span>
                                </div>
                                <span className="text-slate-400 font-bold text-sm">
                                    {product?.reviewCount || 0} customer reviews
                                </span>
                            </div>

                            <div className="items-right flex gap-1 ml-auto">
                                    <span className="text-3xl font-black text-slate-900 tracking-tighter">
                                        {product?.price}
                                    </span>
                                <span className="text-xs font-bold text-slate-400 uppercase tracking-widest ml-2">
                                        Tk
                                    </span>
                            </div>

                            <p className="text-slate-600 font-medium leading-relaxed text-lg mb-10">
                                {product?.description}
                            </p>
                        </div>

                        <Separator className="mb-10 opacity-50"/>

                        <div className="space-y-8">
                            {product?.quantity}
                            {product?.quantity > 0 ? (
                                <>
                                    <div className="flex items-center justify-between">
                                        <div className="space-y-1">
                                            <p className="text-sm font-bold text-slate-900 uppercase tracking-widest">Quantity</p>
                                            <p className="text-xs font-medium text-slate-500">{product?.quantity} items
                                                available</p>
                                        </div>
                                        <div
                                            className="flex items-center gap-1 bg-slate-100 p-1 rounded-2xl border border-slate-200/50">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                onClick={decrementQuantity}
                                                disabled={quantity <= 1 || maxAvailable <= 0}
                                                className="h-10 w-10 rounded-xl hover:bg-white hover:shadow-sm transition-all"
                                            >
                                                <Minus className="h-4 w-4"/>
                                            </Button>
                                            <span
                                                className="w-10 text-center font-black text-slate-800">{maxAvailable <= 0 ? 0 : quantity}</span>
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                onClick={incrementQuantity}
                                                disabled={quantity >= maxAvailable}
                                                className="h-10 w-10 rounded-xl hover:bg-white hover:shadow-sm transition-all"
                                            >
                                                <Plus className="h-4 w-4"/>
                                            </Button>
                                        </div>
                                    </div>

                                    <div className="flex flex-col sm:flex-row gap-4">
                                        <Button
                                            onClick={handleAddToCart}
                                            size="lg"
                                            className="flex-1 h-16 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-black text-lg shadow-xl shadow-blue-600/20 transition-all hover:scale-[1.02] active:scale-95 group"
                                            disabled={maxAvailable <= 0}
                                        >
                                            <ShoppingCart
                                                className="h-6 w-6 mr-3 group-hover:rotate-12 transition-transform"/>
                                            {maxAvailable <= 0 ? 'Limit Reached' : 'Add to Cart'}
                                        </Button>
                                        <Button
                                            variant="outline"
                                            size="lg"
                                            className="h-16 w-16 rounded-2xl border-slate-200 hover:bg-slate-50 text-slate-400 hover:text-red-500 transition-all"
                                        >
                                            <svg className="h-6 w-6" fill="none" stroke="currentColor"
                                                 viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                                      d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                                            </svg>
                                        </Button>
                                    </div>
                                </>
                            ) : (
                                <div
                                    className="p-8 rounded-[2.5rem] bg-red-50 border border-red-100 flex flex-col items-center text-center">
                                    <div
                                        className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600 mb-4">
                                        <ShoppingCart className="h-6 w-6 opacity-50"/>
                                    </div>
                                    <h3 className="text-xl font-black text-red-900 mb-1">Out of Stock</h3>
                                    <p className="text-sm font-medium text-red-700/70">This item is currently
                                        unavailable. Check back soon!</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                <>
                    <div className="flex items-center justify-between mb-8">
                        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Customer Reviews</h2>
                        <div className="h-px flex-1 bg-slate-100 mx-6"/>
                    </div>
                    <div className="grid lg:grid-cols-5 gap-12">
                        <div className="lg:col-span-3">
                            <div className="space-y-6">
                                {reviews?.length > 0 ? (
                                    reviews.map((review) => (
                                        <ReviewCard key={review.id} review={review}/>
                                    ))
                                ) : (
                                    <div
                                        className="bg-white rounded-3xl p-12 border border-slate-100 text-center shadow-sm">
                                        <div
                                            className="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-200 mx-auto mb-4">
                                            <Star className="h-8 w-8"/>
                                        </div>
                                        <h3 className="text-lg font-bold text-slate-800 mb-1">No reviews yet</h3>
                                        <p className="text-sm font-medium text-slate-500">Be the first to share your
                                            thoughts about this product!</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="lg:col-span-2 space-y-8">
                            <ReviewCreate/>
                        </div>
                    </div>
                </>
            </div>
        </div>
    )
}
