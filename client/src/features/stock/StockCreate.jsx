import {useState, useEffect} from 'react'
import {Link, useNavigate, useParams} from 'react-router-dom';
import {Plus, Trash2, Search, Minus} from 'lucide-react';

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
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {GLOBAL_ERROR, handleErrors, toastInitialState} from '@/utils/index.js';
import InputError from "@/components/common/InputError.jsx";
import {
    getIdempotencyKey,
    IDEMPOTENCY_HEADER,
    removeIdempotencyKey
} from "@/utils/idempotencyUtil.js";

const StockCreate = () => {
    const navigate = useNavigate();

    const [items, setItems] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    const [errors, setErrors] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [toastData, setToastData] = useState(toastInitialState);

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
            setSearchResults(response.data.data.content);
        } catch (error) {
            console.error("Error searching products", error);
        } finally {
            setIsSearching(false);
        }
    };

    const addProductToItems = (product) => {
        if (items.some(item => item.productId === product.id)) {
            showToast("Product already added", TOAST_TYPE.INFO);
            setSearchTerm('');
            setSearchResults([]);
            return;
        }

        setItems([...items, {
            productId: product.id,
            name: product.name,
            image: product.image,
            quantity: 1,
            purchasePrice: product.price || 0
        }]);
        setSearchTerm('');
        setSearchResults([]);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        if (items.length === 0) {
            showToast("Please add at least one product", TOAST_TYPE.ERROR);
            return;
        }

        setIsSubmitting(true)
        setErrors({});

        try {
            const idempotencyKey = getIdempotencyKey();

            const response = await Axios.post(`/stocks`, {
                items: items.map(item => ({
                    productId: parseInt(item.productId),
                    quantity: parseInt(item.quantity),
                    purchasePrice: parseFloat(item.purchasePrice)
                }))
            }, {
                headers: {
                    [IDEMPOTENCY_HEADER]: idempotencyKey,
                },
            });

            setItems([]);
            removeIdempotencyKey()

            showToast("Successfully created stock", TOAST_TYPE.SUCCESS);
            setTimeout(() => navigate(`/stocks/${response.data.data}`), 500);
        } catch (error) {
            console.error(error)
            handleErrors(error, setErrors);
        } finally {
            setIsSubmitting(false)
        }
    };

    const updateItem = (index, field, value) => {
        const newItems = [...items];
        newItems[index][field] = value;
        setItems(newItems);
    };

    const removeItem = (index) => {
        const newItems = items.filter((_, i) => i !== index);
        setItems(newItems);
    };

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
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
                                {searchTerm.trim().length >= 2 && !isSearching && searchResults.length === 0 && (
                                    <div
                                        className="absolute z-50 w-full mt-1 bg-white border rounded-md shadow-lg p-3 text-center text-gray-500">
                                        No products found
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    <form onSubmit={handleSave} className="space-y-6">
                        <Card>
                            <CardHeader className="pb-3">
                                <CardTitle>Selected Items</CardTitle>
                                <CardDescription>Adjust quantities and purchase prices for each item</CardDescription>
                            </CardHeader>
                            <CardContent>
                                {items.length === 0 ? (
                                    <div className="text-center py-10 text-gray-500 border-2 border-dashed rounded-lg">
                                        No items selected yet. Use the search bar above to add products.
                                    </div>
                                ) : (
                                    <div className="space-y-4">
                                        {items.map((item, index) => (
                                            <div key={item.productId}
                                                 className="flex flex-col md:flex-row md:items-center gap-4 p-4 border rounded-lg bg-gray-50 relative group">
                                                <div className='w-[15%] mr-4'>
                                                    <img src={item.image} alt={""}/>
                                                </div>
                                                <div className="flex-grow">
                                                    <p className="font-semibold text-lg">{item.name}</p>
                                                    <p className="text-xs text-gray-500">ID: {item.productId}</p>
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
                                                                onClick={() => updateItem(index, 'quantity', Math.max(1, parseInt(item.quantity) - 1))}
                                                            >
                                                                <Minus className="h-3 w-3"/>
                                                            </Button>
                                                            <input
                                                                type="number"
                                                                className="w-12 text-center text-sm border-none focus:ring-0"
                                                                value={item.quantity}
                                                                onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                                                                min={1}
                                                                required
                                                            />
                                                            <Button
                                                                type="button"
                                                                variant="ghost"
                                                                size="sm"
                                                                className="h-8 w-8 p-0 border-1 border-gray-300"
                                                                onClick={() => updateItem(index, 'quantity', parseInt(item.quantity) + 1)}
                                                            >
                                                                <Plus className="h-3 w-3"/>
                                                            </Button>
                                                        </div>
                                                    </div>

                                                    <div className="space-y-1">
                                                        <label className="text-xs font-medium uppercase text-gray-500">Unit
                                                            Price ($)</label>
                                                        <Input
                                                            type="number"
                                                            step="0.01"
                                                            className="h-9 w-24 bg-white"
                                                            value={item.purchasePrice * (80 / 100)}
                                                            onChange={(e) => updateItem(index, 'purchasePrice', e.target.value)}
                                                            min={0}
                                                            required
                                                        />
                                                    </div>

                                                    <div className="space-y-1 text-right min-w-[80px]">
                                                        <label
                                                            className="text-xs font-medium uppercase text-gray-500">Subtotal</label>
                                                        <p className="font-bold text-blue-600">
                                                            ${(parseFloat(item.quantity || 0) * parseFloat(item.purchasePrice || 0)).toFixed(2)}
                                                        </p>
                                                    </div>

                                                    <Button
                                                        type="button"
                                                        variant="outline"
                                                        size="icon"
                                                        className="text-red-500 hover:text-red-700 hover:bg-red-50"
                                                        onClick={() => removeItem(index)}
                                                    >
                                                        <Trash2 className="h-4 w-4"/>
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}

                                        <div className="mt-6 p-4 border-t flex justify-between">
                                            <p className="text-xl text-gray-600 font-medium">
                                                Total
                                                Items: {items.reduce((acc, item) => acc + parseInt(item.quantity || 0), 0)}
                                            </p>
                                            <p className="text-xl font-bold text-blue-900">
                                                Total Cost:
                                                ${items.reduce((acc, item) => acc + (parseFloat(item.quantity || 0)
                                                * parseFloat(item.purchasePrice || 0)), 0).toFixed(2)}
                                            </p>
                                        </div>
                                    </div>
                                )}
                            </CardContent>
                            <CardFooter className="bg-gray-50 border-t p-6 flex flex-col gap-4">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>
                                {isSubmitting ? (
                                    <ButtonLoading className="w-full"/>
                                ) : (
                                    <Button
                                        type="submit"
                                        className="w-full bg-blue-600 text-lg"
                                        disabled={items.length === 0}
                                    >
                                        Save Stock Purchase
                                    </Button>
                                )}
                            </CardFooter>
                        </Card>
                    </form>
                </div>
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    );
};

export default StockCreate;
