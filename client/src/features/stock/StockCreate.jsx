import React, { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Plus, Trash2, Search, Minus } from 'lucide-react';

import { Button } from '@/components/ui/button.jsx';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card.jsx';
import { Input } from '@/components/ui/input.jsx';
import { Axios } from '@/services/http/Axios.js';
import { ButtonLoading } from "@/components/common/ButtonLoading.jsx";
import { TOAST_TYPE } from '@/utils/enums.js';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import { ToastAlert } from '@/components/common/ToastAlert.jsx';
import { URL_NOT_FOUND } from '@/utils/index.js';

const StockCreate = () => {
  const { productId } = useParams();
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState({ page: false, button: false });
  const [toastData, setToastData] = useState({ message: "", type: "", id: Date.now() });

  // Handle initial productId from URL
  useEffect(() => {
    if (productId) {
        const fetchInitialProduct = async () => {
            try {
                const response = await Axios.get(`/products/${productId}`);
                const product = response.data.data;
                setItems([{ 
                    productId: product.id, 
                    name: product.name, 
                    quantity: 1, 
                    purchasedPrice: product.price || '' 
                }]);
            } catch (error) {
                console.error("Error fetching initial product", error);
                showToast("Product not found", TOAST_TYPE.ERROR);
            }
        };
        fetchInitialProduct();
    }
  }, [productId]);

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

  const searchProducts = async (query) => {
    setIsSearching(true);
    try {
      const response = await Axios.get('/products', { params: { name: query } });
      setSearchResults(response.data.data.content);
    } catch (error) {
      console.error("Error searching products", error);
    } finally {
      setIsSearching(false);
    }
  };

  const addProductToItems = (product) => {
    // Check if product already in list
    if (items.some(item => item.productId === product.id)) {
        showToast("Product already added", TOAST_TYPE.INFO);
        setSearchTerm('');
        setSearchResults([]);
        return;
    }

    setItems([...items, { 
        productId: product.id, 
        name: product.name, 
        quantity: 1, 
        purchasedPrice: product.price || 0 
    }]);
    setSearchTerm('');
    setSearchResults([]);
  };

  const handleError = (error) => {
    console.error(error);
    if ([403, 404].includes(error?.status)) navigate(URL_NOT_FOUND, { replace: true });
    setErrors(error.response?.data || { global: error.message });
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (items.length === 0) {
        showToast("Please add at least one product", TOAST_TYPE.ERROR);
        return;
    }

    setIsLoading({ ...isLoading, button: true });
    setErrors({});

    try {
      await Axios.post(`/stocks`, {
        items: items.map(item => ({
            productId: parseInt(item.productId),
            quantity: parseInt(item.quantity),
            purchasedPrice: parseFloat(item.purchasedPrice)
        }))
      });

      showToast("Successfully created stock", TOAST_TYPE.SUCCESS);
      setTimeout(() => navigate('/stocks'), 1500);
    } catch (error) {
      handleError(error);
    } finally {
      setIsLoading({ ...isLoading, button: false });
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
    setToastData({ message, type, id: Date.now() });
  };

  return (
    <>
      {isLoading.page && <PageLoadingOverlay />}

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
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                  <Input
                    className="pl-10"
                    placeholder="Type at least 2 characters to search..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                  {isSearching && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                       <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                    </div>
                  )}
                </div>

                {searchResults.length > 0 && (
                  <div className="absolute z-50 w-full mt-1 bg-white border rounded-md shadow-lg max-h-60 overflow-auto">
                    {searchResults.map((product) => (
                      <div
                        key={product.id}
                        className="p-3 hover:bg-blue-50 cursor-pointer flex justify-between items-center border-b last:border-b-0"
                        onClick={() => addProductToItems(product)}
                      >
                        <div>
                          <p className="font-medium">{product.name}</p>
                          <p className="text-xs text-gray-500">Price: ${product.price} | Stock: {product.quantity}</p>
                        </div>
                        <Plus className="h-4 w-4 text-blue-600" />
                      </div>
                    ))}
                  </div>
                )}
                {searchTerm.trim().length >= 2 && !isSearching && searchResults.length === 0 && (
                     <div className="absolute z-50 w-full mt-1 bg-white border rounded-md shadow-lg p-3 text-center text-gray-500">
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
                      <div key={item.productId} className="flex flex-col md:flex-row md:items-center gap-4 p-4 border rounded-lg bg-gray-50 relative group">
                        <div className="flex-grow">
                          <p className="font-semibold text-lg">{item.name}</p>
                          <p className="text-xs text-gray-500">ID: {item.productId}</p>
                        </div>
                        
                        <div className="flex items-center gap-6">
                          <div className="space-y-1">
                            <label className="text-xs font-medium uppercase text-gray-500">Quantity</label>
                            <div className="flex items-center border rounded-md bg-white">
                              <Button 
                                type="button" 
                                variant="ghost" 
                                size="sm" 
                                className="h-8 w-8 p-0"
                                onClick={() => updateItem(index, 'quantity', Math.max(1, parseInt(item.quantity) - 1))}
                              >
                                <Minus className="h-3 w-3" />
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
                                className="h-8 w-8 p-0"
                                onClick={() => updateItem(index, 'quantity', parseInt(item.quantity) + 1)}
                              >
                                <Plus className="h-3 w-3" />
                              </Button>
                            </div>
                          </div>

                          <div className="space-y-1">
                            <label className="text-xs font-medium uppercase text-gray-500">Unit Price ($)</label>
                            <Input
                              type="number"
                              step="0.01"
                              className="h-9 w-24 bg-white"
                              value={item.purchasedPrice}
                              onChange={(e) => updateItem(index, 'purchasedPrice', e.target.value)}
                              min={0}
                              required
                            />
                          </div>

                          <div className="space-y-1 text-right min-w-[80px]">
                            <label className="text-xs font-medium uppercase text-gray-500">Subtotal</label>
                            <p className="font-bold text-blue-600">
                              ${(parseFloat(item.quantity || 0) * parseFloat(item.purchasedPrice || 0)).toFixed(2)}
                            </p>
                          </div>

                          <Button 
                            type="button" 
                            variant="ghost" 
                            size="icon" 
                            className="text-red-500 hover:text-red-700 hover:bg-red-50"
                            onClick={() => removeItem(index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    ))}

                    <div className="mt-6 p-4 border-t flex justify-end items-center gap-4">
                        <p className="text-gray-600 font-medium">Total Items: {items.reduce((acc, item) => acc + parseInt(item.quantity || 0), 0)}</p>
                        <div className="text-2xl font-bold text-blue-900">
                            Total Cost: ${items.reduce((acc, item) => acc + (parseFloat(item.quantity || 0) * parseFloat(item.purchasedPrice || 0)), 0).toFixed(2)}
                        </div>
                    </div>
                  </div>
                )}
              </CardContent>
              <CardFooter className="bg-gray-50 border-t p-6 flex flex-col gap-4">
                {errors.global && <p className="text-red-600 text-sm font-medium">{errors.global}</p>}
                
                {isLoading.button ? (
                  <ButtonLoading className="w-full" />
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
