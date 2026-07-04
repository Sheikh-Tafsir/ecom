import {useEffect, useState} from "react";
import {Link, useLocation, useNavigate, useParams} from "react-router-dom";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import * as z from "zod";

import {Button} from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.jsx";
import {Input} from "@/components/ui/input.jsx";
import {Label} from "@/components/ui/label.jsx";
import {Textarea} from "@/components/ui/textarea";

import StaredLabel from "@/components/common/StaredLabel";
import MultiImageInput from "@/components/common/MultiImageInput";
import InputError from "@/components/common/InputError";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import {ButtonLoading} from "@/components/common/ButtonLoading";

import {Axios} from "@/services/http/Axios";
import {GLOBAL_ERROR, handleErrors} from "@/utils";
import {TOAST_TYPE} from "@/utils/enums";
import { notify } from "@/components/common/notification";
import { useQuery } from "@tanstack/react-query";

const MAX_IMAGES = 5;

const ProductSchema = z.object({
    name: z.string().min(1, "Name is required")
        .max(31, "Name cannot exceed 31 characters"),
    description: z.string().min(5, "Description is required")
        .max(1023, "Description cannot exceed 1023 characters"),
    price: z.coerce.number().positive().min(1, "Price must be greater than 0"),
    categoryIds: z.array(z.string()).min(1, "Select at least one category"),
    keptImageIds: z.array(z.number()).optional(),
});

const fetchCategories = async () => {
    const response = await Axios.get("/categories")
    return response.data.data
}

const fetchProduct = async (id) => {
  const response = await Axios.get(`/products/${id}`);
  return response.data.data;
};

const ProductCreate = () => {
    const {id} = useParams();

    const navigate = useNavigate();
    const location = useLocation();

    const isCreatePage = location.pathname.includes("/create");

    const [newImages, setNewImages] = useState([]);
    const [existingImages, setExistingImages] = useState([]);
    const [resetImagesKey, setResetImagesKey] = useState(Date.now());

    const {
        register,
        handleSubmit,
        reset,
        setValue,
        watch,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(ProductSchema),
        defaultValues: {
            name: "",
            description: "",
            price: "",
            categoryIds: [],
            keptImageIds: [],
        },
    });

    const categoryIds = watch("categoryIds");

    const {
        data: categories = [], 
        isFetching: isCategoriesLoading,
        isError: isCategoriesError,
        error: categoriesError,
    } = useQuery({
        queryKey: ["categories"], 
        queryFn: fetchCategories, 
    })

    const {
        data: product,
        isLoading: isProductLoading,
        isError: isProductError,
        error: productError,
    } = useQuery({
        queryKey: ["product", id],
        queryFn: () => fetchProduct(id),
        enabled: !isCreatePage && !!id,
    });

    useEffect(() => {
        if (!product) return;

        setExistingImages(product.images);

        reset({
            name: product.name,
            description: product.description,
            price: product.price,
            categoryIds: product.categories?.map(c => String(c.id)) || [],
            keptImageIds: product.images?.map(img => img.id) || [],
        });
    }, [product, reset]);


    const saveProduct = async (data) => {
        try {
            const formData = new FormData();

            Object.entries(data).forEach(([key, value]) => {
                if (Array.isArray(value)) {
                    value.forEach((item) => formData.append(key, item));
                } else {
                    formData.append(key, value ?? "");
                }
            });

            newImages.forEach((file) => {
                formData.append("images", file);
            });

            if (isCreatePage) {
               const response = await Axios.post("/products", formData, {
                    headers: {'Content-Type': 'multipart/form-data'},
                    timeout: 15000,
                });

                showToast("Successfully created", TOAST_TYPE.SUCCESS);
                
                reset();
                setExistingImages([]);
                setNewImages([]);
                setResetImagesKey(Date.now());

                setTimeout(() => {
                    navigate(`/products/${response.data.data}`);
                }, 500);
            } else {
                await Axios.put(`/products/${id}`, formData, {
                    headers: {'Content-Type': 'multipart/form-data'},
                    timeout: 15000,
                });

                notify(TOAST_TYPE.SUCCESS, "Product updated successfully")
            }
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    };

    useEffect(() => {
        if (!isCategoriesError) return;

        console.error(categoriesError);
        notify(TOAST_TYPE.ERROR, "Failed to show categories");
    }, [isCategoriesError, categoriesError]);

    useEffect(() => {
        if (!isProductError) return;

        console.error(productError);
        notify(TOAST_TYPE.ERROR, "Failed to show product");
    }, [isProductError, productError]);

    return (
        <>
            {(isCategoriesLoading || isProductLoading) && <PageLoadingOverlay/>}

            <div className="container lg:flex min-h-screen py-8">
                <Card className="mx-auto my-auto w-[450px] lg:w-[550px]">
                    <form onSubmit={handleSubmit(saveProduct)}>
                        <CardHeader>
                            <CardTitle>
                                {isCreatePage ? "Create" : "Edit"} Product
                            </CardTitle>

                            <CardDescription>
                                {isCreatePage ? "Add new" : "Edit"} product by filling out the information below
                            </CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                            <MultiImageInput
                                key={resetImagesKey}
                                existingImages={existingImages}
                                onExistingImagesChange={(images) => {
                                    setExistingImages(images);

                                    setValue(
                                        "keptImageIds",
                                        images.map((img) => img.id)
                                    );
                                }}
                                onImagesChange={setNewImages}
                                maxImages={MAX_IMAGES}
                                errors={errors}
                                setError={setError}
                                isRequired={true}
                            />
                            <InputError errors={errors} field="keptImageIds"/>

                            <div className="space-y-2">
                                <StaredLabel label="Name"/>
                                <Input {...register("name")} />
                                <InputError errors={errors} field="name"/>
                            </div>

                            <div className="space-y-2">
                                <StaredLabel label="Description"/>
                                <Textarea {...register("description")} />
                                <InputError errors={errors} field="description"/>
                            </div>

                            <div className="space-y-2">
                                <StaredLabel label="Price"/>
                                <Input
                                    type="number"
                                    step="0.5"
                                    min="0"
                                    {...register("price")}
                                />
                                <InputError errors={errors} field="price"/>
                            </div>

                            <div className="space-y-2">
                                <StaredLabel label="Categories"/>

                                <div className="grid grid-cols-2 gap-2 border rounded-md p-2">
                                    {categories.map((category) => (
                                        <div
                                            key={category.id}
                                            className="flex items-center gap-2"
                                        >
                                            <input
                                                type="checkbox"
                                                checked={categoryIds.includes(
                                                    String(category.id)
                                                )}
                                                onChange={(e) => {
                                                    const updatedCategoryIds = e.target.checked
                                                        ? [
                                                            ...categoryIds,
                                                            String(category.id),
                                                        ]
                                                        : categoryIds.filter(
                                                            (id) =>
                                                                id !== String(category.id)
                                                        );

                                                    setValue(
                                                        "categoryIds",
                                                        updatedCategoryIds,
                                                        {shouldValidate: true}
                                                    );
                                                }}
                                                className="h-3"
                                            />

                                            <Label>{category.name}</Label>
                                        </div>
                                    ))}
                                </div>

                                <InputError errors={errors} field="categories"/>
                                <InputError errors={errors} field="categoryIds"/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex flex-col gap-2">
                            {isSubmitting ? (
                                <ButtonLoading/>
                            ) : (
                                <Button
                                    type="submit"
                                    className="w-full"
                                >
                                    Save
                                </Button>
                            )}

                            <Link
                                to="/products"
                                className="text-xs text-blue-600"
                            >
                                Back to list
                            </Link>
                        </CardFooter>
                    </form>
                </Card>
            </div>
        </>
    );
};

export default ProductCreate;