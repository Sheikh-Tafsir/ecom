import React, { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import { Input } from '@/components/ui/input.jsx';
import { Label } from '@/components/ui/label.jsx';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.jsx"
import StaredLabel from '@/components/common/StaredLabel';
import { Axios } from '@/services/http/Axios';
import { ButtonLoading } from "@/components/common/ButtonLoading";
import { TOAST_TYPE } from '@/utils/enums';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import { ToastAlert } from '@/components/common/ToastAlert';
import MultiImageInput from '@/components/common/MultiImageInput';
import { Textarea } from '@/components/ui/textarea';
import InputReadOnly from "@/components/common/InputReadOnly"
import { URL_NOT_FOUND } from '@/utils';

const MAX_IMAGES = 5;

const ProductCreate = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const isCreatePage = location.pathname.includes("/create");

    const [categories, setCategories] = useState([]);
    const [product, setProduct] = useState({
        name: '',
        description: '',
        price: '',
        quantity: '',
        cost: '',
        categoryId: '',
    });
    const [images, setImages] = useState([]);
    const [existingImages, setExistingImages] = useState([]);
    const [errors, setErrors] = useState({});
    const [isLoading, setIsLoading] = useState({ page: true, button: false });
    const [resetImagesKey, setResetImagesKey] = useState(Date.now());
    const [toastData, setToastData] = useState({ message: "", type: "", id: Date.now() });

    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const response = await Axios.get("/categories");
                const filteredCategories = response.data.data.filter(
                    (category) => category.name !== 'All'
                );
                setCategories(filteredCategories);
            } catch (error) {
                handleError(error);
            } finally {
                setIsLoading({ ...isLoading, page: false });
            }
        }

        fetchCategories();
    }, []);

    useEffect(() => {
        const fetchProduct = async (id) => {
            try {
                const response = await Axios.get(`/products/${id}`);
                const temp = response.data.data;

                setProduct({
                    name: temp.name,
                    description: temp.description,
                    price: temp.price,
                    quantity: temp.quantity,
                    categoryId: temp.categoryId,
                });
                setExistingImages(temp.images);
            } catch (error) {
                handleError(error);
            }
        }

        if (!isCreatePage && id) {
            fetchProduct(id);
        }
    }, [isCreatePage, id])

    const handleError = (error) => {
        console.error(error);
        if ([403, 404].includes(error?.status)) navigate(URL_NOT_FOUND, { replace: true });
        setErrors(error.response?.data || { global: error.message });
    };

    const prepareFormData = () => {
        const formData = new FormData();
        Object.entries(product).forEach(([key, val]) => {
            if (val !== undefined && val !== null) {
                formData.append(key, val);
            }
        });

        images.forEach((image) => {
            formData.append('images', image);
        });

        return formData;
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setIsLoading({ ...isLoading, button: true });
        setErrors({});

        try {
            await Axios.post('/products', prepareFormData(), {
                headers: { 'Content-Type': 'multipart/form-data' },
                timeout: 20000
            });

            setProduct({
                name: '',
                description: '',
                price: '',
                quantity: '',
                categoryId: '',
            });
            setImages([]);
            setResetImagesKey(Date.now());
            showToast("Successfully created", TOAST_TYPE.SUCCESS);
        } catch (error) {
            handleError(error);
        } finally {
            setIsLoading({ ...isLoading, button: false });
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        setIsLoading({ ...isLoading, button: true });
        setErrors({});

        try {
            await Axios.put(`/products/${id}`, {
                ...product
            });

            setProduct({
                name: '',
                description: '',
                price: '',
                quantity: '',
                categoryId: '',
            });

            navigate('/products');
        } catch (error) {
            handleError(error);
        } finally {
            setIsLoading({ ...isLoading, button: false });
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setProduct((prev) => ({ ...prev, [name]: value }));
    };

    const showToast = (message, type) => {
        setToastData({ message, type, id: Date.now() });
    };

    return (
        <>
            {isLoading.page && <PageLoadingOverlay />}

            <div className="container lg:flex min-h-[100vh] pb-8">
                <Card className="mx-auto my-auto w-[450px] lg:w-[550px]">
                    <form onSubmit={isCreatePage ? handleSave : handleUpdate}>
                        <CardHeader>
                            <CardTitle>{isCreatePage ? 'Create' : 'Edit'} Product</CardTitle>
                            <CardDescription>{isCreatePage ? 'Add new' : 'Edit'} product by filling out the information below</CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            {isCreatePage ?
                                <MultiImageInput key={resetImagesKey} onChangeImages={setImages} errors={errors} maxImages={MAX_IMAGES} />
                                :
                                <div className="flex space-x-4 overflow-x-auto py-2">
                                    {existingImages.map((item) => (
                                        <div key={item.id} className="relative min-w-[100px] max-w-[160px]">
                                            <img
                                                src={item.image}
                                                alt={`preview-${item.image}`}
                                                className="max-h-[150px] object-cover rounded-md border"
                                            />
                                        </div>
                                    ))}
                                </div>
                            }

                            <div className='space-y-2'>
                                <StaredLabel label="Name" />
                                <Input
                                    type="text"
                                    name="name"
                                    placeholder="Book"
                                    value={product.name}
                                    onChange={handleChange}
                                    required
                                />
                                {errors.name && <p className='validation-error'>{errors.name}</p>}
                            </div>

                            <div className='space-y-2'>
                                <Label htmlFor="description">Description</Label>
                                <Textarea
                                    type="text"
                                    name="description"
                                    placeholder="Short description"
                                    value={product.description}
                                    onChange={handleChange}
                                />
                                {errors.description && <p className='validation-error'>{errors.description}</p>}
                            </div>

                            <div className='space-y-2'>
                                <StaredLabel label="Selling Price" />
                                <Input
                                    type="number"
                                    name="price"
                                    placeholder="100"
                                    value={product.price || ''}
                                    onChange={handleChange}
                                    min={0}
                                    required
                                />
                                {errors.price && <p className='validation-error'>{errors.price}</p>}
                            </div>

                            <div className='space-y-2'>
                                <StaredLabel label="Quantity" />
                                {isCreatePage ?
                                    <Input
                                        type="number"
                                        name="quantity"
                                        placeholder="10"
                                        value={product.quantity || ''}
                                        onChange={handleChange}
                                        required
                                    />
                                    :
                                    <InputReadOnly value={product.quantity} />
                                }
                                {errors.quantity && <p className='validation-error'>{errors.quantity}</p>}
                            </div>

                            {isCreatePage &&
                                <div className='space-y-2'>
                                    <StaredLabel label="cost" />
                                    <Input
                                        type="number"
                                        name="cost"
                                        placeholder="100"
                                        value={product.cost || ''}
                                        onChange={handleChange}
                                        min={0}
                                        required
                                    />
                                    {errors.description && <p className='validation-error'>{errors.description}</p>}
                                </div>
                            }

                            <div className="flex flex-col space-y-1.5">
                                <StaredLabel label="Category" />
                                {isCreatePage ?
                                    <Select onValueChange={(value) => setProduct((prev) => ({ ...prev, "categoryId": value }))} value={product.categoryId || ""}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select Category" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectGroup>
                                                {categories?.map((item) => (
                                                    <SelectItem key={item.id} value={String(item.id)}>
                                                        {item.name}
                                                    </SelectItem>
                                                ))}
                                            </SelectGroup>
                                        </SelectContent>
                                    </Select>
                                    :
                                    <InputReadOnly value={categories?.find(cat => cat.id === product.categoryId)?.name || ""} />
                                }
                                <p className="validation-error">{errors.categoryId}</p>
                            </div>

                            {errors.global && <p className="validation-error">{errors.global}</p>}
                        </CardContent>

                        <CardFooter className="flex-col gap-2">
                            {isLoading.button ? (
                                <ButtonLoading css="w-full" />
                            ) : (
                                <Button type="submit" className="w-full cursor-pointer" style={{ backgroundColor: 'rgb(24,62,139)' }}>
                                    Save
                                </Button>
                            )}

                            <Link to="/products" className='text-xs text-blue-600 mt-2'>Back to list</Link>
                        </CardFooter>
                    </form>
                </Card>
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    );
};

export default ProductCreate;