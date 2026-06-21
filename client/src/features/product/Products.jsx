import {useEffect, useState, useMemo} from "react"
import {Search} from "lucide-react"
import {Input} from "@/components/ui/input"
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"

import ProductCard from "./ProductCard"
import PaginationButton from "@/components/common/PaginationButton"
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay"
import {ToastAlert} from "@/components/common/ToastAlert"

import {Axios} from "@/services/http/Axios"
import {useNavigate, useSearchParams} from "react-router-dom"
import {
    getQueryString, getSelectValue, toastInitialState, redirectWhenInvalidPage, updateQueryWhenParamChange
} from "@/utils"
import {PRODUCT_SORTBY} from "@/utils/enums"

import {useQuery, keepPreviousData} from "@tanstack/react-query"
import {normalizeQuery} from "@/features/product/ProductPaginationUtils.js"

export default function Products() {
    const navigate = useNavigate()

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams), [queryParams])
    const {page, sort, search, category} = filters

    const [searchInput, setSearchInput] = useState(search)
    const [toastData, setToastData] = useState(toastInitialState);

    useEffect(() => {
        setSearchInput(search)
    }, [search])

    useEffect(() => {
        if (!searchInput && !search) return;

        const timer = setTimeout(() => {
            navigate(getQueryString({
                ...queryParams, search: searchInput || undefined, page: 1,
            }), {replace: true})
        }, 400)

        return () => clearTimeout(timer)
    }, [searchInput, navigate])

    // Fetch products
    const fetchProducts = async ({queryKey}) => {
        const [, params] = queryKey
        const response = await Axios.get("/products", {
            params: {
                page: params.page - 1, // already normalized (safe)
                sort: params.sort,
                size: params.size,
                name: params.search || undefined,
                category: params.category || undefined,
            },
        })

        return response.data.data
    }

    // Fetch categories
    const fetchCategories = async () => {
        const res = await Axios.get("/categories")
        return res.data.data
    }

    // Queries
    const {
        data: categories = [], isFetching: categoriesLoading
    } = useQuery({
        queryKey: ["categories"], queryFn: fetchCategories, staleTime: 60 * 60 * 1000, keepPreviousData: true,
    })

    const {data: productData, isFetching: productsLoading} = useQuery({
        queryKey: ["products", filters],
        queryFn: fetchProducts, placeholderData: keepPreviousData,
    })

    const products = productData?.content || []
    const totalPages = productData?.totalPages || 0

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    const updateQuery = (newParams) => {
        updateQueryWhenParamChange({queryParams, newParams, navigate})
    }

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()})
    }

    return (<>
        {(categoriesLoading || productsLoading) && (<PageLoadingOverlay/>)}

        <div className="container pb-8 pt-8">
            <div className="mb-8">
                <div className="flex">
                    <h1 className="text-3xl font-bold m-auto mb-6">Products</h1>
                </div>

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
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {products.map((p) => (<ProductCard
                    key={p.id}
                    product={p}
                    showToast={showToast}
                />))}
            </div>

            {products.length === 0
                && (<div className="text-center py-12 text-gray-500">
                    No products found matching your criteria.
                </div>)
            }

            <PaginationButton totalPages={totalPages}/>
        </div>

        <ToastAlert
            key={toastData.id}
            message={toastData.message}
            type={toastData.type}
        />
    </>)
}