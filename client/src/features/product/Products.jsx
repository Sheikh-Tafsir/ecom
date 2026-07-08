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
import { Button } from "@/components/ui/button"

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
        <div className="min-h-screen">
            {(isCategoriesLoading || isProductsLoading) && (<PageLoadingOverlay/>)}

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 md:py-16">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-12">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Explore Products</h1>
                        <p className="text-slate-500 font-medium">Discover premium tools and resources for your journey</p>
                    </div>

                    <div className="flex items-center bg-white px-4 py-2 rounded-2xl border border-slate-100 shadow-sm h-fit">
                        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest border-r border-slate-100 pr-3 mr-3">Results</span>
                        <span className="text-sm font-black text-blue-600">{productData?.totalElements || 0}</span>
                    </div>
                </div>

                {/* Discovery Toolbar */}
                <div className="grid lg:grid-cols-4 gap-6 mb-12">
                    {/* Search */}
                    <div className="lg:col-span-2 relative group">
                        <div className="absolute inset-y-0 left-4 flex items-center pointer-events-none">
                            <Search className="h-5 w-5 text-slate-400 group-focus-within:text-blue-500 transition-colors"/>
                        </div>
                        <Input
                            placeholder="Search for premium products..."
                            value={searchInput}
                            onChange={(e) => setSearchInput(e.target.value)}
                            className="h-14 pl-12 rounded-lg bg-white border-slate-100 shadow-sm focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-medium"
                        />
                    </div>

                    {/* Category */}
                    <Select
                        value={getSelectValue(category)}
                        onValueChange={(val) => updateQuery({category: val})}
                    >
                        <SelectTrigger className="h-14 rounded-lg bg-white border-slate-100 shadow-sm focus:ring-4 focus:ring-blue-50 font-bold text-slate-700">
                            <div className="flex items-center gap-2">
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Cat:</span>
                                <SelectValue placeholder="All Categories"/>
                            </div>
                        </SelectTrigger>

                        <SelectContent className="rounded-lg border-slate-100 shadow-xl p-2">
                            <SelectItem value="__all__" className="rounded-lg py-2.5 font-semibold">All Categories</SelectItem>
                            {categories.map((c) => (
                                <SelectItem key={c.id} value={c.name} className="rounded-lg py-2.5">
                                    {c.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    {/* Sort */}
                    <Select
                        value={sort}
                        onValueChange={(val) => updateQuery({sort: val})}
                    >
                        <SelectTrigger className="h-14 rounded-lg bg-white border-slate-100 shadow-sm focus:ring-4 focus:ring-blue-50 font-bold text-slate-700">
                            <div className="flex items-center gap-2">
                                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Sort:</span>
                                <SelectValue placeholder="Latest"/>
                            </div>
                        </SelectTrigger>

                        <SelectContent className="rounded-lg border-slate-100 shadow-xl p-2">
                            {Object.values(PRODUCT_SORTBY).map((opt) => (
                                <SelectItem key={opt.value} value={opt.value} className="rounded-lg py-2.5 font-semibold">
                                    {opt.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {/* Grid */}
                {products.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8 mb-16 animate-in fade-in slide-in-from-bottom-8 duration-700">
                        {products.map((p) => (
                            <ProductCard
                                key={p.id}
                                product={p}
                            />
                        ))}
                    </div>
                ) : (
                    <div className="bg-white rounded-[3rem] border border-slate-100 shadow-sm py-32 flex flex-col items-center justify-center text-center px-4 animate-in fade-in zoom-in duration-500 mb-16">
                        <div className="relative mb-6">
                            <div className="absolute -inset-4 bg-slate-100/50 rounded-full blur-2xl" />
                            <div className="relative w-24 h-24 bg-slate-50 rounded-lg rotate-12 flex items-center justify-center border border-slate-100 shadow-sm">
                                <Search className="h-10 w-10 text-slate-300 transform -rotate-12" />
                            </div>
                        </div>
                        <h3 className="text-2xl font-black text-slate-900 mb-2 tracking-tight">No products found</h3>
                        <p className="text-slate-500 max-w-[320px] font-medium leading-relaxed mb-8">
                            We couldn't find anything matching "{searchInput}". Try using more general keywords or clearing your filters.
                        </p>
                        <Button 
                            variant="outline" 
                            className="rounded-lg h-12 px-6 font-bold text-slate-600 border-slate-200 hover:bg-slate-50"
                            onClick={() => {
                                setSearchInput("");
                                updateQuery({category: "__all__", sort: "createdAt,DESC"});
                            }}
                        >
                            Reset all filters
                        </Button>
                    </div>
                )}

                <div className="flex justify-center border-t border-slate-200 pt-10">
                    <PaginationButton totalPages={totalPages}/>
                </div>
            </div>
        </div>
    )
    }