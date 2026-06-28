import {useEffect, useState, useMemo, useCallback} from "react"
import {Search} from "lucide-react"
import {useQuery, keepPreviousData} from "@tanstack/react-query"
import {useNavigate, useSearchParams} from "react-router-dom"

import {Input} from "@/components/ui/input"
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"

import ProductCard from "./ProductCard"
import PaginationButton from "@/components/common/PaginationButton"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import {Axios} from "@/services/http/Axios"
import {getSelectValue} from "@/utils"
import {
    FIRST_PAGE,
    getQueryString,
    normalizeQuery,
    redirectWhenInvalidPage,
    updateQueryWhenParamChange
} from '@/utils/PaginationUtils';
import {PRODUCT_SORTBY, TOAST_TYPE} from "@/utils/enums"
import {notify} from "@/components/common/notification"

const ALLOWED_SORT_FIELDS = new Set([
    "createdAt",
    "price",
    "name",
    "rating",
])

const fetchProducts = async ({queryKey}) => {
    const [, params] = queryKey;

    const response = await Axios.get("/products", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            size: params.size,
            name: params.search || undefined,
            category: params.category || undefined,
        },
    })

    return response.data.data
}

const fetchCategories = async () => {
    const response = await Axios.get("/categories")
    return response.data.data
}

export default function Products() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, ALLOWED_SORT_FIELDS),
            search: queryParams.search || "",
            category: queryParams.category || "",
        }),
        [queryParams]
    );
    const {page, sort, search, category} = filters

    const [searchInput, setSearchInput] = useState(search)

    useEffect(() => {
        setSearchInput(search)
    }, [search])

    useEffect(() => {
        const trimmed = searchInput.trim();
        if (trimmed == search) return;

        const timer = setTimeout(() => {
            const newQuery = getQueryString({
                ...queryParams,
                search: trimmed || undefined,
                page: FIRST_PAGE,
            });

            navigate(newQuery, {replace: true});
        }, 400);

        return () => clearTimeout(timer)
    }, [searchInput, search, queryParams, navigate])


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
        data: productData,
        isPending: isProductsLoading,
        isError: isProductsError,
        error: productsError,
    } = useQuery({
        queryKey: ["products", filters],
        queryFn: fetchProducts,
        placeholderData: keepPreviousData,
    })

    const products = productData?.content ?? []
    const totalPages = productData?.totalPages ?? FIRST_PAGE

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    const updateQuery = useCallback(
        (newParams) => {
            updateQueryWhenParamChange({
                queryParams,
                newParams,
                navigate,
            });
        },
        [queryParams, navigate]
    );

    useEffect(() => {
        if (!isCategoriesError) return;

        console.error(categoriesError);
        notify(TOAST_TYPE.ERROR, "Failed to show categories");
    }, [isCategoriesError, categoriesError]);

    useEffect(() => {
        if (!isProductsError) return;

        console.error(productsError);
        notify(TOAST_TYPE.ERROR, "Failed to show products");
    }, [isProductsError, productsError]);

    return (
        <>
            {(isCategoriesLoading || isProductsLoading) && (<PageLoadingOverlay/>)}

            <div className="container pb-8 pt-8">
                <div className="mb-8">
                    <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6 font-semibold'>Products</h1>

                    {/* Filters */}
                    <div className="flex flex-col md:flex-row gap-4 mb-6">
                        {/* Search */}
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-3 text-gray-400 h-4 w-4"/>
                            <Input
                                placeholder="Search products..."
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                className="pl-10"
                            />
                        </div>

                        {/* Category */}
                        <Select
                            value={getSelectValue(category)}
                            onValueChange={(val) => updateQuery({category: val})}
                        >
                            <SelectTrigger className="w-full md:w-48">
                                <SelectValue placeholder="All Categories"/>
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="__all__">All</SelectItem>

                                {categories.map((c) => (<SelectItem key={c.id} value={c.name}>
                                    {c.name}
                                </SelectItem>))}
                            </SelectContent>
                        </Select>

                        {/* Sort */}
                        <Select
                            value={sort}
                            onValueChange={(val) => updateQuery({sort: val})}
                        >
                            <SelectTrigger className="w-full md:w-48">
                                <SelectValue placeholder="Sort by"/>
                            </SelectTrigger>

                            <SelectContent>
                                {Object.values(PRODUCT_SORTBY).map((opt) => (
                                    <SelectItem key={opt.value} value={opt.value}>
                                        {opt.label}
                                    </SelectItem>))}
                            </SelectContent>
                        </Select>
                    </div>

                    <p className="text-gray-600 mb-4">
                        Showing {products.length} products
                    </p>
                </div>

                {/* Grid */}
                {products.length > 0 ?
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                            {products.map((p) => (
                                <ProductCard
                                    key={p.id}
                                    product={p}
                                />
                            ))}

                    </div>
                    :
                    <div className="text-center py-12 text-gray-500">
                        No products found matching your criteria.
                    </div>
                }

                <PaginationButton totalPages={totalPages}/>
            </div>
        </>)
}