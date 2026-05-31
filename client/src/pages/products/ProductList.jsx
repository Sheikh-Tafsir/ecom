import { useState, useEffect, useMemo } from "react"
import { Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import ProductCard from "./ProductCard"
import { Axios } from "@/middleware/Axios.js"
import PaginationButton from "@/mycomponents/PaginationButton"
import { useNavigate, useSearchParams } from "react-router-dom"
import { FIRST_PAGE, getQueryString } from '../utils/NavigationUtils';
import { PRODUCT_SORTBY } from "@/utils/enums"
import { useQuery } from "@tanstack/react-query"
import PageLoadingOverlay from "@/mycomponents/pageLoadingOverlay/PageLoadingOverlay"
import { ToastAlert } from "@/mycomponents/ToastAlert"

export default function ProductList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  //const queryParams = Object.fromEntries(searchParams.entries());
  const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);
  const page = searchParams.get("page") || FIRST_PAGE;
  const [search, setSearch] = useState(searchParams.get("search") || '')
  const [category, setCategory] = useState(searchParams.get("category") || "All")
  const [sortBy, setSortBy] = useState(searchParams.get("sortBy") || PRODUCT_SORTBY.DATE_DESC.value)

  const [debouncedSearch, setDebouncedSearch] = useState(search);
  const [toastData, setToastData] = useState(toastInitialState);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 500);

    return () => clearTimeout(timer); // cleanup on re-render
  }, [search]);

  const fetchProducts = async ({ queryKey }) => {
    const [_key, params] = queryKey;
    const response = await Axios.get("/products", { queryParams });
    return response.data.data;
  };

  const fetchCategories = async () => {
    const response = await Axios.get("/categories");
    return response.data.data;
  };

  const {
    data: categories = [],
    isLoading: isCategoriesLoading,
    isError: isCategoriesError,
  } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategories,
  });

  // Products Query
  const {
    data: productData,
    isLoading: isProductsLoading,
    isError: isProductsError,
  } = useQuery({
    queryKey: ["products", queryParams],
    queryFn: fetchProducts,
    // keepPreviousData: true,
  });

  const products = productData?.content || [];
  const totalPages = productData?.totalPages || FIRST_PAGE;

  if (isCategoriesError || isProductsError) {
    console.error(isCategoriesError ? isCategoriesError : isProductsError);
    showToast("Failed to load products", TOAST_TYPE.ERROR);
  }

  useEffect(() => {
    const navQueryParams = { ...queryParams, search: debouncedSearch, category, sortBy };
    const newQuery = getQueryString(navQueryParams);
    if (newQuery !== window.location.search) {
      navigate(newQuery, { replace: true });
    }
  }, [debouncedSearch, category, sortBy]);

  useEffect(() => {
    const navQueryParams = {
      ...queryParams,
      search: debouncedSearch,
      category,
      sortBy,
    };

    navigate(getQueryString(navQueryParams), { replace: true });
  }, [debouncedSearch, category, sortBy, navigate]);

  const showToast = (message, type) => {
    setToastData({ message, type, id: Date.now() });
  };

  return (
    <>
      {(isCategoriesLoading || isProductsLoading) && <PageLoadingOverlay />}

      <div className="container pb-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-6">Products</h1>

          {/* Search and Filters */}
          <div className="flex flex-col md:flex-row gap-4 mb-6">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                placeholder="Search products..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-10"
              />
            </div>

            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger className="w-full md:w-48">
                <SelectValue placeholder="Category" />
              </SelectTrigger>
              <SelectContent>
                {categories.map((category, index) => (
                  <SelectItem key={index} value={category.name}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={sortBy} onValueChange={setSortBy}>
              <SelectTrigger className="w-full md:w-48">
                <SelectValue placeholder="Sort by" />
              </SelectTrigger>
              <SelectContent>
                {Object.values(PRODUCT_SORTBY).map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Results count */}
          <p className="text-gray-600 mb-4">
            Showing {products.length} of {products.length} products
          </p>
        </div>

        {/* Products Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {products?.map((product) => (
            <ProductCard key={product.id} product={product} showToast={showToast} />
          ))}
        </div>

        {products.length === 0 ?
          (
            <div className="text-center py-12">
              <p className="text-gray-500 text-lg">No products found matching your criteria.</p>
            </div>
          )
          :
          (
            totalPages === 1 ?
              null
              :
              <PaginationButton totalPages={totalPages} />
          )
        }
      </div>

      <ToastAlert
        key={toastData.id}
        message={toastData.message}
        type={toastData.type}
      />
    </>
  )
}