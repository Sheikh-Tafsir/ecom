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
    price: z.coerce.number().min(1, 'Price is required and must be positive'),
    categoryIds: z.array(z.string()).min(1, 'At least one category is required'),
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
            categoryIds: [],
        },
    });

    const categoryIds = watch('categoryIds');

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
                    categoryIds: temp.categorise.map(c => String(c.id)),
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

    const saveProduct = async (data) => {
        setIsLoading({...isLoading, button: true});

        try {
            if (isCreatePage) {
                const formData = new FormData();

                Object.entries(data).forEach(([key, val]) => {
                    if (key === 'categoryIds') {
                        val.forEach(id => formData.append('categoryIds', id));
                    } else {
                        formData.append(key, val);
                    }
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
                const payload = {
                    ...data,
                    categoryIds: data.categoryIds.map(Number)
                };
                await Axios.put(`/products/${id}`, payload);
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
                    <form onSubmit={handleSubmit(saveProduct)}>
                        <CardHeader>
                            <CardTitle>{isCreatePage ? 'Create' : 'Edit'} Product</CardTitle>
                            <CardDescription>{isCreatePage ? 'Add new' : 'Edit'} product by filling out the information
                                below</CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            {isCreatePage ?
                                <MultiImageInput key={resetImagesKey} onChangeImages={setImages} maxImages={MAX_IMAGES}
                                                 errors={errors} setError={setError}/>
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

                            <div className="flex flex-col space-y-2">
                                <StaredLabel label="Categories"/>
                                {isCreatePage ?
                                    <div className="grid grid-cols-2 gap-2 border p-2 rounded-md max-h-40 overflow-y-auto">
                                        {categories?.map((item) => (
                                            <div key={item.id} className="flex items-center space-x-2">
                                                <input
                                                    type="checkbox"
                                                    id={`cat-${item.id}`}
                                                    value={String(item.id)}
                                                    checked={categoryIds?.includes(String(item.id))}
                                                    onChange={(e) => {
                                                        const newCategories = e.target.checked
                                                            ? [...categoryIds, String(item.id)]
                                                            : categoryIds.filter(id => id !== String(item.id));
                                                        setValue('categoryIds', newCategories);
                                                    }}
                                                />
                                                <Label htmlFor={`cat-${item.id}`}>{item.name}</Label>
                                            </div>
                                        ))}
                                    </div>
                                    :
                                    <InputReadOnly
                                        value={categories?.filter(cat => categoryIds?.includes(String(cat.id))).map(c => c.name).join(", ") || ""}/>
                                }
                                <InputError errors={errors} field="categoryIds"/>
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
