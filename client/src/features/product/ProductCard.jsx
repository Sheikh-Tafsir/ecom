import React from "react"
import {Link, useNavigate} from "react-router-dom"

import {Star, ShoppingCart} from "lucide-react"
import {Card, CardContent, CardFooter} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {Badge} from "@/components/ui/badge"
import {useCartStore} from "@/store/useCartStore"
import {PERMISSION, TOAST_TYPE} from "@/utils/enums"
import {hasPermission} from "@/utils/index.js";
import { notify } from "@/components/common/notification"
import { useUserStore } from "@/store/useUserStore"

const ProductCard = ({product}) => {
    const {user} = useUserStore();
    const {addToCart} = useCartStore();
    const navigate = useNavigate();

    const handleAddToCart = (e) => {
        e.preventDefault();

        const response = addToCart(product, 1);
        notify(TOAST_TYPE.SUCCESS, response ? "Added to cart" : "Item increased")
    }

    const deleteProduct = () => {

    }

    const navigateToEdit = (e) => {
        e.preventDefault();
        navigate(`/products/${product.id}/edit`)
    }

    return (
        <Link to={`/products/${product.id}`}>
            <Card className="h-full hover:shadow-lg transition-shadow cursor-pointer">
                <CardContent className="p-4">
                    <div className="aspect-square relative">
                        <img
                            src={product.image || "/placeholder.svg"}
                            alt={product.name}
                            className="h-[258px] w-full object-cover rounded-md"
                            loading="lazy"
                        />
                        <div className="absolute top-2 right-2 flex flex-wrap justify-end gap-1">
                            {product.categories?.map((category) => (
                                <Badge key={category}>{category}</Badge>
                            ))}
                        </div>
                    </div>

                    <h3 className="font-semibold text-xl mb-2 line-clamp-2">{product.name}</h3>

                    <div className="flex justify-between">
                        <div>
                            <div className="flex items-center">
                                <div className="flex items-center">
                                    {[...Array(5)].map((_, i) => (
                                        <Star
                                            key={i}
                                            className={`h-4 w-4 ${i < Math.floor(product.rating) ? "text-yellow-400 fill-current" : "text-gray-300"
                                            }`}
                                        />
                                    ))}
                                </div>
                                <span className="text-sm text-gray-600 ml-2">{product?.rating}</span>
                            </div>
                            <span className="text-sm text-gray-500">{product.quantity} in stock</span>
                        </div>

                        <span className="text-2xl font-bold text-primary">${product.price}</span>
                    </div>
                </CardContent>

                <CardFooter className="p-4 pt-0">
                    {hasPermission(user, PERMISSION.SUPER_ADMIN_ACCESS)?
                        <div className="flex justify-between w-full mt-4 gap-4">
                            <Button className='w-[50%]' onClick={navigateToEdit}>
                                Edit
                            </Button>
                            <Button variant="outline" className='w-[50%]' onClick={deleteProduct}>
                                Delete
                            </Button>
                        </div>
                        :
                        <Button onClick={handleAddToCart} className="w-full ml-2 bg-blue-600">
                            <ShoppingCart className="h-4 w-4 mr-1"/>
                            Add
                        </Button>
                    }
                </CardFooter>
            </Card>
        </Link>
    )
}

export default React.memo(ProductCard);
