import {useState, useEffect} from 'react'
import {Link, useNavigate} from 'react-router-dom';
import {Plus, Trash2, Search, Minus} from 'lucide-react';
import {useForm, useFieldArray} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';

import {Button} from '@/components/ui/button.jsx';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Input} from '@/components/ui/input.jsx';
import {Axios} from '@/services/http/Axios.js';
import {ButtonLoading} from "@/components/common/ButtonLoading.jsx";
import {TOAST_TYPE} from '@/utils/enums.js';
import {GLOBAL_ERROR, handleErrors} from '@/utils/index.js';
import InputError from "@/components/common/InputError.jsx";
import {
    getIdempotencyKey,
    IDEMPOTENCY_HEADER,
    removeIdempotencyKey
} from "@/utils/idempotencyUtil.js";
import { notify } from '@/components/common/notification';

const StockItemSchema = z.object({
    productId: z.number(),
    name: z.string(),
    image: z.string().optional(),
    quantity: z.coerce.number().int().min(1, "Quantity must be at least 1"),
    purchasePrice: z.coerce.number().min(0, "Purchase price must be positive"),
});

const StockSchema = z.object({
    items: z.array(StockItemSchema).min(1, "Please add at least one product"),
});

const StockCreate = () => {
    const navigate = useNavigate();

    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    const {
        register,
        control,
        handleSubmit,
        watch,
        setValue,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(StockSchema),
        defaultValues: {
            items: [],
        },
    });

    const {fields, append, remove, update} = useFieldArray({
        control,
        name: "items",
    });

    const items = watch("items");

    // Debounced search
    useEffect(() => {
        const delayDebounceFn = setTimeout(() => {
            if (searchTerm.trim().length >= 2) {
                searchProducts(searchTerm);
            } else {
                setSearchResults([]);
            }
        }, 300);

        return () => clearTimeout(delayDebounceFn);
    }, [searchTerm]);

    const searchProducts = async (name) => {
        setIsSearching(true);

        try {
            const response = await Axios.get('/products/search', {params: {name}});
            console.log(response.data.data)
            setSearchResults(response.data.data);
        } catch (error) {
            console.error("Error searching products", error);
        } finally {
            setIsSearching(false);
        }
    };

    const addProductToItems = (product) => {
        if (items.some(item => item.productId == product.id)) {
            notify(TOAST_TYPE.INFO, "Product already added")
            setSearchTerm('');
            setSearchResults([]);
            return;
        }

        append({
            productId: product.id,
            name: product.name,
            image: product.image,
            quantity: 1,
            purchasePrice: product.price || 0
        });
        setSearchTerm('');
        setSearchResults([]);
    };

    const handleSave = async (data) => {
        const idempotencyKey = getIdempotencyKey();
        
        try {
            const response = await Axios.post(`/stocks`, {
                items: data.items.map(item => ({
                    productId: item.productId,
                    quantity: item.quantity,
                    purchasePrice: item.purchasePrice
                }))
            }, {
                headers: {
                    [IDEMPOTENCY_HEADER]: idempotencyKey,
                },
            });

            removeIdempotencyKey()

            notify(TOAST_TYPE.SUCCESS, "Successfully created stock")
            setTimeout(() => navigate(`/stocks/${response.data.data}`), 500);
        } catch (error) {
            console.error(error)
            handleErrors(error, setError);
            if (error.response) {
                removeIdempotencyKey();
            }
        }
    };

    const calculateTotalItems = () => {
        return items.reduce((acc, item) => acc + (parseInt(item.quantity) || 0), 0);
    };

    const calculateTotalCost = () => {
        return items.reduce((acc, item) => acc + (parseFloat(item.quantity || 0) * parseFloat(item.purchasePrice || 0)), 0).toFixed(2);
    };

    return (
        <>
            <div className="container py-10 min-h-[100vh]">
                <div className="max-w-4xl mx-auto space-y-6">
                    <div className="flex justify-between items-center">
                        <h1 className="text-3xl font-bold">New Stock Purchase</h1>
                        <Link to="/stocks" className='text-sm text-blue-600 hover:underline'>Back to stock list</Link>
                    </div>

                    <Card className="shadow-sm">
                        <CardHeader className="pb-3">
                            <CardTitle>Search Products</CardTitle>
                            <CardDescription>Search for products by name to add to this purchase</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="relative">
                                <div className="relative">
                                    <Search
                                        className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4"/>
                                    <Input
                                        className="pl-10"
                                        placeholder="Type at least 2 characters to search..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                    />
                                    {isSearching && (
                                        <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                                            <div
                                                className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                                        </div>
                                    )}
                                </div>

                                {searchResults.length > 0 && (
                                    <div
                                        className="absolute z-50 w-[50%] mt-1 bg-white border rounded-md shadow-lg max-h-60 overflow-auto">
                                        {searchResults.map((product) => (
                                            <div
                                                key={product.id}
                                                className="p-3 hover:bg-blue-50 cursor-pointer flex justify-between items-center border-b last:border-b-0"
                                                onClick={() => addProductToItems(product)}
                                            >
                                                <div className="flex">
                                                    <div className='w-[20%] mr-4'>
                                                        <img src={product.image} alt={""}/>
                                                    </div>
                                                    <div>
                                                        <p className="font-medium mb-1">{product.name}</p>
                                                        <p className="text-xs text-gray-500">Price: ${product.price} |
                                                            Stock: {product.quantity}</p>
                                                    </div>

                                                </div>
                                                <Plus className="h-4 w-4 text-blue-600"/>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                {searchTerm.trim().length >= 2 && !isSearching && searchResults.length == 0 && (
                                    <div
                                        className="absolute z-50 w-full mt-1 bg-white border rounded-md shadow-lg p-3 text-center text-gray-500">
                                        No products found
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    <form onSubmit={handleSubmit(handleSave)} className="space-y-6">
                        <Card>
                            <CardHeader className="pb-3">
                                <CardTitle>Selected Items</CardTitle>
                                <CardDescription>Adjust quantities and purchase prices for each item</CardDescription>
                            </CardHeader>
                            <CardContent>
                                {fields.length == 0 ? (
                                    <div className="text-center py-10 text-gray-500 border-2 border-dashed rounded-lg">
                                        No items selected yet. Use the search bar above to add products.
                                    </div>
                                ) : (
                                    <div className="space-y-4">
                                        {fields.map((field, index) => (
                                            <div key={field.id}
                                                 className="flex flex-col md:flex-row md:items-center gap-4 p-4 border rounded-lg bg-gray-50 relative group">
                                                <div className='w-[15%] mr-4'>
                                                    <img src={items[index]?.image} alt={""}/>
                                                </div>
                                                <div className="flex-grow">
                                                    <p className="font-semibold text-lg">{items[index]?.name}</p>
                                                    <p className="text-xs text-gray-500">ID: {items[index]?.productId}</p>
                                                </div>

                                                <div className="flex items-center gap-6">
                                                    <div className="space-y-1">
                                                        <label
                                                            className="text-xs font-medium uppercase text-gray-500">Quantity</label>
                                                        <div className="flex items-center border rounded-md bg-white">
                                                            <Button
                                                                type="button"
                                                                variant="ghost"
                                                                size="sm"
                                                                className="h-8 w-8 p-0"
                                                                onClick={() => {
                                                                    const currentQty = parseInt(items[index].quantity) || 1;
                                                                    update(index, {...items[index], quantity: Math.max(1, currentQty - 1)});
                                                                }}
                                                            >
                                                                <Minus className="h-3 w-3"/>
                                                            </Button>
                                                            <input
                                                                type="number"
                                                                className="w-12 text-center text-sm border-none focus:ring-0"
                                                                {...register(`items.${index}.quantity`)}
                                                                min={1}
                                                                required
                                                            />
                                                            <Button
                                                                type="button"
                                                                variant="ghost"
                                                                size="sm"
                                                                className="h-8 w-8 p-0 border-1 border-gray-300"
                                                                onClick={() => {
                                                                    const currentQty = parseInt(items[index].quantity) || 0;
                                                                    update(index, {...items[index], quantity: currentQty + 1});
                                                                }}
                                                            >
                                                                <Plus className="h-3 w-3"/>
                                                            </Button>
                                                        </div>
                                                        <InputError errors={errors} field={`items.${index}.quantity`}/>
                                                    </div>

                                                    <div className="space-y-1">
                                                        <label className="text-xs font-medium uppercase text-gray-500">Unit
                                                            Price ($)</label>
                                                        <Input
                                                            type="number"
                                                            step="0.01"
                                                            className="h-9 w-24 bg-white"
                                                            {...register(`items.${index}.purchasePrice`)}
                                                            min={0}
                                                            required
                                                        />
                                                        <InputError errors={errors} field={`items.${index}.purchasePrice`}/>
                                                    </div>

                                                    <div className="space-y-1 text-right min-w-[80px]">
                                                        <label
                                                            className="text-xs font-medium uppercase text-gray-500">Subtotal</label>
                                                        <p className="font-bold text-blue-600">
                                                            ${(parseFloat(items[index].quantity || 0) * parseFloat(items[index].purchasePrice || 0)).toFixed(2)}
                                                        </p>
                                                    </div>

                                                    <Button
                                                        type="button"
                                                        variant="outline"
                                                        size="icon"
                                                        className="text-red-500 hover:text-red-700 hover:bg-red-50"
                                                        onClick={() => remove(index)}
                                                    >
                                                        <Trash2 className="h-4 w-4"/>
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}

                                        <div className="mt-6 p-4 border-t flex justify-between">
                                            <p className="text-xl text-gray-600 font-medium">
                                                Total
                                                Items: {calculateTotalItems()}
                                            </p>
                                            <p className="text-xl font-bold text-blue-900">
                                                Total Cost:
                                                ${calculateTotalCost()}
                                            </p>
                                        </div>
                                    </div>
                                )}

                                <InputError errors={errors} field="items" />
                            </CardContent>
                            <CardFooter className="bg-gray-50 border-t p-6 flex flex-col gap-4">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>
                                {isSubmitting ? (
                                    <ButtonLoading className="w-full"/>
                                ) : (
                                    <Button
                                        type="submit"
                                        className="w-full bg-blue-600 text-lg"
                                        disabled={fields.length == 0}
                                    >
                                        Save Stock Purchase
                                    </Button>
                                )}
                            </CardFooter>
                        </Card>
                    </form>
                </div>
            </div>
        </>
    );
};

export default StockCreate;
