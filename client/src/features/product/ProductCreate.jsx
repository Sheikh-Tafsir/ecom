import {useEffect, useState} from 'react'
import {Link, useLocation, useNavigate, useParams} from 'react-router-dom';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import * as z from 'zod';

import {Button} from '@/components/ui/button';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Input} from '@/components/ui/input.jsx';
import {Label} from '@/components/ui/label.jsx';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.jsx"
import StaredLabel from '@/components/common/StaredLabel';
import {Axios} from '@/services/http/Axios';
import {ButtonLoading} from "@/components/common/ButtonLoading";
import {TOAST_TYPE} from '@/utils/enums';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import {ToastAlert} from '@/components/common/ToastAlert';
import MultiImageInput from '@/components/common/MultiImageInput';
import {Textarea} from '@/components/ui/textarea';
import InputReadOnly from "@/components/common/InputReadOnly"
import {handleErrors} from '@/utils';
import InputError from "@/components/common/InputError.jsx";

const MAX_IMAGES = 5;

const productSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    description: z.string().optional(),
    price: z.coerce.number().min(0, 'Price must be positive'),
    quantity: z.coerce.number().int().min(0, 'Quantity must be positive'),
    cost: z.coerce.number().min(0, 'Cost must be positive'),
    categoryId: z.string().min(1, 'Category is required'),
});

const ProductCreate = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const isCreatePage = location.pathname.includes("/create");

    const [categories, setCategories] = useState([]);
    const [images, setImages] = useState([]);
    const [existingImages, setExistingImages] = useState([]);
    const [isLoading, setIsLoading] = useState({page: true, button: false});
    const [resetImagesKey, setResetImagesKey] = useState(Date.now());
    const [toastData, setToastData] = useState({message: "", type: "", id: Date.now()});

    const {
        register,
        handleSubmit,
        setValue,
        watch,
        reset,
        setError,
        formState: {errors},
    } = useForm({
        resolver: zodResolver(productSchema),
        defaultValues: {
            name: '',
            description: '',
            price: '',
            categoryId: '',
        },
    });

    const categoryId = watch('categoryId');

    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const response = await Axios.get("/categories");
                const filteredCategories = response.data.data.filter(
                    (category) => category.name !== 'All'
                );
                setCategories(filteredCategories);
            } catch (error) {
                handleErrors(error, setError);
            } finally {
                setIsLoading({...isLoading, page: false});
            }
        }

        fetchCategories();
    }, []);

    useEffect(() => {
        const fetchProduct = async (id) => {
            try {
                const response = await Axios.get(`/products/${id}`);
                const temp = response.data.data;

                reset({
                    name: temp.name,
                    description: temp.description,
                    price: temp.price,
                    quantity: temp.quantity,
                    categoryId: String(temp.categoryId),
                });
                setExistingImages(temp.images);
            } catch (error) {
                handleErrors(error, setError);
            }
        }

        if (!isCreatePage && id) {
            fetchProduct(id);
        }
    }, [isCreatePage, id, reset])

    const onSubmit = async (data) => {
        setIsLoading({...isLoading, button: true});

        try {
            if (isCreatePage) {
                const formData = new FormData();
                Object.entries(data).forEach(([key, val]) => {
                    formData.append(key, val);
                });
                images.forEach((image) => {
                    formData.append('images', image);
                });

                await Axios.post('/products', formData, {
                    headers: {'Content-Type': 'multipart/form-data'},
                });

                reset();
                setImages([]);
                setResetImagesKey(Date.now());
                showToast("Successfully created", TOAST_TYPE.SUCCESS);
            } else {
                await Axios.put(`/products/${id}`, data);
                navigate('/products');
            }
        } catch (error) {
            handleErrors(error, setError);
        } finally {
            setIsLoading({...isLoading, button: false});
        }
    };

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    return (
        <>
            {isLoading.page && <PageLoadingOverlay/>}

            <div className="container lg:flex min-h-[100vh] pb-8 pt-8">
                <Card className="mx-auto my-auto w-[450px] lg:w-[550px]">
                    <form onSubmit={handleSubmit(onSubmit)}>
                        <CardHeader>
                            <CardTitle>{isCreatePage ? 'Create' : 'Edit'} Product</CardTitle>
                            <CardDescription>{isCreatePage ? 'Add new' : 'Edit'} product by filling out the information
                                below</CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            {isCreatePage ?
                                <MultiImageInput key={resetImagesKey} onChangeImages={setImages} errors={errors}
                                                 maxImages={MAX_IMAGES}/>
                                :
                                <div className="flex space-x-4 overflow-x-auto py-2">
                                    {existingImages.map((item) => (
                                        <div key={item.id} className="relative min-w-[100px] max-w-[160px]">
                                            <img
                                                src={item.image}
                                                alt={`preview-${item.image}`}
                                                className="max-h-[150px] object-cover rounded-md border"
                                                loading="lazy"
                                            />
                                        </div>
                                    ))}
                                </div>
                            }

                            <div className='space-y-2'>
                                <StaredLabel label="Name"/>
                                <Input
                                    type="text"
                                    placeholder="Book"
                                    {...register('name')}
                                />
                                <InputError errors={errors} field="name"/>
                            </div>

                            <div className='space-y-2'>
                                <Label htmlFor="description">Description</Label>
                                <Textarea
                                    placeholder="Short description"
                                    {...register('description')}
                                />
                                <InputError errors={errors} field="description"/>
                            </div>

                            <div className='space-y-2'>
                                <StaredLabel label="Price"/>
                                <Input
                                    type="number"
                                    placeholder="100"
                                    {...register('price')}
                                    min={0}
                                    step="0.01"
                                />
                                <InputError errors={errors} field="price"/>
                            </div>

                            <div className="flex flex-col space-y-1.5">
                                <StaredLabel label="Category"/>
                                {isCreatePage ?
                                    <Select onValueChange={(value) => setValue('categoryId', value)} value={categoryId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select Category"/>
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
                                    <InputReadOnly
                                        value={categories?.find(cat => String(cat.id) === categoryId)?.name || ""}/>
                                }
                                <InputError errors={errors} field="category"/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex-col gap-2">
                            {isLoading.button ? (
                                <ButtonLoading/>
                            ) : (
                                <Button type="submit" className="w-full cursor-pointer"
                                        style={{backgroundColor: 'rgb(24,62,139)'}}>
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
