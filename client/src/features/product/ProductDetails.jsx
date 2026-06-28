import {useEffect, useState} from "react"
import {useParams} from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query";

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
import { notify } from "@/components/common/notification"
import ReviewCreate from "./ReviewCreate";

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
        queryKey: ["reviews", id],
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

    const handleError = (error) => {
        const responseErrors = error.response?.data || {global: error.message};
        if (responseErrors.product_id && responseErrors.user_id) setErrors({global: "Can add only 1 review"})
        else setErrors(responseErrors);
    };


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
        <>
            {isProductLoading && <PageLoadingOverlay/>}

            <div className="container pb-8 pt-6">
                <div className="grid md:grid-cols-2 gap-8 mb-10">
                    <div className="w-full">
                        <Carousel className="w-[82%] mx-auto">
                            <CarouselContent>
                                {product?.images?.map((image) => (
                                    <CarouselItem key={image.id}>
                                        <Card>
                                            <CardContent className="flex aspect-square items-center justify-center">
                                                <img
                                                    src={image.image || "https://media.istockphoto.com/id/1147544807/vector/thumbnail-image-vector-graphic.jpg?s=1024x1024&w=is&k=20&c=5aen6wD1rsiMZSaVeJ9BWM4GGh5LE_9h97haNpUQN5I="}
                                                    alt={product.name}
                                                    className="object-cover rounded-lg w-full h-full"
                                                />
                                            </CardContent>
                                        </Card>
                                    </CarouselItem>
                                ))}
                            </CarouselContent>
                            <CarouselPrevious/>
                            <CarouselNext/>
                        </Carousel>
                    </div>

                    <div className="space-y-6 bg-white h-fit p-10 rounded-lg">
                        <div>
                            <h1 className="text-3xl font-bold mb-4">{product?.name}</h1>

                            <div className="flex flex-wrap gap-2 mb-6">
                                {product?.categories?.map((category) => (
                                    <Badge key={category.id} variant="secondary">
                                        {category.name}
                                    </Badge>
                                ))}
                            </div>

                            <div className="flex justify-between mb-4">
                                <div className="flex items-center">
                                    <div className="flex items-center">
                                        {[...Array(5)].map((_, i) => (
                                            <Star
                                                key={i}
                                                className={`h-5 w-5 ${i < Math.floor(product?.rating) ? "text-yellow-400 fill-current" : "text-gray-300"
                                                }`}
                                            />
                                        ))}
                                    </div>
                                    <span className="text-gray-600 ml-2">
                                        {product?.reviewCount === 0 ? 'Not Rated Yet' : `${product?.rating} out of 5`}
                                    </span>
                                </div>

                                <p className="text-2xl font-bold text-primary">${product?.price}</p>
                            </div>

                            <p className="text-gray-600 mb-6 leading-relaxed">{product?.description}</p>
                        </div>

                        <Separator/>

                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <span className="font-medium">Stock:</span>
                                <span
                                    className={`font-medium ${product?.quantity > 0 ? "text-green-600" : "text-red-600"}`}>
                                    {product?.quantity > 0 ? `${product?.quantity} available` : "Out of stock"}
                                </span>
                            </div>

                            {product?.quantity > 0 && (
                                <>
                                    <div className="flex items-center space-x-4">
                                        <span className="font-medium">Quantity:</span>
                                        <div className="flex items-center space-x-2">
                                            <Button variant="outline" size="sm" onClick={decrementQuantity}
                                                    disabled={quantity <= 1 || maxAvailable <= 0}>
                                                <Minus className="h-4 w-4"/>
                                            </Button>
                                            <span
                                                className="w-12 text-center font-medium">{maxAvailable <= 0 ? 0 : quantity}</span>
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={incrementQuantity}
                                                disabled={quantity >= maxAvailable}
                                            >
                                                <Plus className="h-4 w-4"/>
                                            </Button>
                                        </div>
                                    </div>

                                    <Button
                                        onClick={handleAddToCart}
                                        size="lg"
                                        className="w-full bg-blue-600"
                                        disabled={maxAvailable <= 0}
                                    >
                                        <ShoppingCart className="h-5 w-5 mr-2"/>
                                        {maxAvailable <= 0 ? 'Maximum quantity in cart' : `Add ${quantity} to Cart`}
                                    </Button>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                <div className="flex gap-6">
                    <Card className="w-[49%] h-fit pb-4">
                        <CardHeader>
                            <CardTitle>All Reviews</CardTitle>
                        </CardHeader>
                        <CardContent className="px-6 max-h-[270px] overflow-y-scroll">
                            <ScrollArea>
                                {reviews?.length > 0 ?
                                    reviews.map((review) => (
                                        <ReviewCard key={review.id} review={review}/>
                                    ))
                                    :
                                    <p>No Reviews till now</p>
                                }
                            </ScrollArea>
                        </CardContent>
                    </Card>

                    <ReviewCreate />
                </div>
            </div>
        </>
    )
}
