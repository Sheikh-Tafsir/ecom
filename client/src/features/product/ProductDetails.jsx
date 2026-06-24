import {useEffect, useState} from "react"
import {useParams} from "react-router-dom"

import {Star, ShoppingCart, Minus, Plus} from "lucide-react"
import {Button} from "@/components/ui/button"
import {Badge} from "@/components/ui/badge"
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card"
import {Separator} from "@/components/ui/separator"
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.jsx"
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
import {Label} from "@/components/ui/label"
import {Textarea} from "@/components/ui/textarea"
import {ButtonLoading} from "@/components/common/ButtonLoading"
import {ScrollArea} from "@/components/ui/scroll-area"
import ReviewCard from "./ReviewCard"
import {ToastAlert} from "@/components/common/ToastAlert"
import {TOAST_TYPE} from "@/utils/enums"
import {useUserStore} from "@/store/useUserStore"
import {toastInitialState} from "@/utils/index.js";

export default function ProductDetails() {
    const {id} = useParams()
    const {user} = useUserStore();
    const {cart, getCartTotal, addToCart} = useCartStore();

    const [isPageLoading, setIsPageLoading] = useState(true);
    const [isButtonLoading, setIsButtonLoading] = useState(false);
    const [product, setProduct] = useState({});
    const [quantity, setQuantity] = useState(1);
    const [review, setReview] = useState({});
    const [errors, setErrors] = useState({});
    const [toastData, setToastData] = useState(toastInitialState);

    const itemInCart = product?.id ? cart.find(item => item.productId == product.id) : null;
    const inCartQuantity = itemInCart ? itemInCart.quantity : 0;
    const maxAvailable = product?.quantity ? (product.quantity - inCartQuantity) : 0;

    useEffect(() => {
        fetchProduct(id);
    }, [id])

    const fetchProduct = async () => {
        setIsPageLoading(true);
        try {
            const response = await Axios.get(`/products/${id}`);
            setProduct(response.data.data);
        } catch (err) {
            console.error('Error fetching product:', err);
            showToast("Could not get product", TOAST_TYPE.ERROR);
        } finally {
            setIsPageLoading(false);
        }
    }

    const handleAddToCart = () => {
        addToCart(product, quantity);
        setQuantity(1);
        showToast("Product added", TOAST_TYPE.SUCCESS);
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

    const handleComment = async (e) => {
        e.preventDefault();
        setIsButtonLoading(true);

        try {
            await Axios.post(`/products/${id}/review`, {
                ...review
            });

            await fetchProduct();
        } catch (error) {
            console.error("Error adding review", error);
            handleError(error);
        } finally {
            setIsButtonLoading(false);
        }
    }

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    if (!isPageLoading && !product) {
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
            {isPageLoading && <PageLoadingOverlay/>}

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
                            <h1 className="text-3xl font-bold mb-4">{product.name}</h1>

                            <div className="flex flex-wrap gap-2 mb-6">
                                {product.categories?.map((category) => (
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
                                                className={`h-5 w-5 ${i < Math.floor(product.rating) ? "text-yellow-400 fill-current" : "text-gray-300"
                                                }`}
                                            />
                                        ))}
                                    </div>
                                    <span className="text-gray-600 ml-2">
                                        {product.reviewCount === 0 ? 'Not Rated Yet' : `${product.rating} out of 5`}
                                    </span>
                                </div>

                                <p className="text-2xl font-bold text-primary">${product.price}</p>
                            </div>

                            <p className="text-gray-600 mb-6 leading-relaxed">{product.description}</p>
                        </div>

                        <Separator/>

                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <span className="font-medium">Stock:</span>
                                <span className={`font-medium ${product.quantity > 0 ? "text-green-600" : "text-red-600"}`}>
                                    {product.quantity > 0 ? `${product.quantity} available` : "Out of stock"}
                                </span>
                            </div>

                            {product.quantity > 0 && (
                                <>
                                    <div className="flex items-center space-x-4">
                                        <span className="font-medium">Quantity:</span>
                                        <div className="flex items-center space-x-2">
                                            <Button variant="outline" size="sm" onClick={decrementQuantity}
                                                    disabled={quantity <= 1 || maxAvailable <= 0}>
                                                <Minus className="h-4 w-4"/>
                                            </Button>
                                            <span className="w-12 text-center font-medium">{maxAvailable <= 0 ? 0 : quantity}</span>
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
                                {product?.reviews?.length > 0 ?
                                    product.reviews.map((review) => (
                                        <ReviewCard key={review.id} review={review}/>
                                    ))
                                    :
                                    <p>No Reviews till now</p>
                                }
                            </ScrollArea>
                        </CardContent>
                    </Card>

                    {user?.id && !product.reviews?.some(r => r.user.id === user.id) && (
                        <Card className="w-[49%] h-fit">
                            <form onSubmit={handleComment}>
                                <CardHeader>
                                    <CardTitle>Add Review</CardTitle>
                                </CardHeader>
                                <CardContent className="px-6 space-y-4">
                                    <div className="space-y-2">
                                        <Label className="flex">Rating<p className='text-red-600'>*</p></Label>
                                        <Select
                                            onValueChange={(value) =>
                                                setReview((prev) => ({...prev, rating: Number(value)}))
                                            }
                                            value={review.rating || 5}
                                        >
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select Rating (1–5)"/>
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectGroup>
                                                    {[1, 2, 3, 4, 5].map((num) => (
                                                        <SelectItem key={num} value={num} className="cursor-pointer">
                                                            {num} Star{num > 1 && "s"}
                                                        </SelectItem>
                                                    ))}
                                                </SelectGroup>
                                            </SelectContent>
                                        </Select>
                                        <p className='validation-error'>{errors.rating || ""}</p>
                                    </div>
                                    <div className="space-y-2">
                                        <Label className="flex">
                                            Review<p className="text-red-600">*</p>
                                        </Label>
                                        <Textarea
                                            placeholder=""
                                            value={review.comment}
                                            onChange={(event) =>
                                                setReview((prev) => ({...prev, comment: event.target.value}))
                                            }
                                        />
                                        <p className="validation-error">{errors.comment || ""}</p>
                                    </div>
                                    <p className="validation-error">{errors.global || ""}</p>
                                </CardContent>

                                <CardFooter className="flex-col gap-2">
                                    {isButtonLoading ? (
                                        <ButtonLoading/>
                                    ) : (
                                        <Button type="submit" className="w-full bg-blue-600">
                                            Add
                                        </Button>
                                    )}
                                </CardFooter>
                            </form>
                        </Card>
                    )}
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
