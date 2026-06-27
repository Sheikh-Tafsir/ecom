import {useState, useEffect, useMemo, useCallback} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {keepPreviousData, useQuery} from "@tanstack/react-query";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.jsx"
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Button} from '@/components/ui/button.jsx';
import {Axios} from '@/services/http/Axios.js';
import PaginationButton from '@/components/common/PaginationButton.jsx';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {
    FIRST_PAGE,
    toastInitialState,
    redirectWhenInvalidPage,
    normalizeQuery, formatDate,
    getQueryString
} from '@/utils/index.js';
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {TOAST_TYPE} from "@/utils/enums.js";
import InputError from "@/components/common/InputError";
import StaredLabel from "@/components/common/StaredLabel";
import {notify} from '@/components/common/notification';

const fetchStockItems = async ({queryKey}) => {
    const [, params] = queryKey

    const response = await Axios.get("/stocks/items", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            size: params.size,
        },
    })

    return response.data.data
}

const StockItems = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams), [queryParams])
    const {page, productName, fromDate, toDate} = filters;

    const [form, setForm] = useState({
        productName: "",
        fromDate: "",
        toDate: "",
    });

    const {
        data, isFetching: isPageLoading, isError, error
    } = useQuery({
        queryKey: ["stockItems", filters],
        queryFn: fetchStockItems,
        placeholderData: keepPreviousData,
    })

    const stockItems = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams]);

    useEffect(() => {
        setForm({
            productName,
            fromDate,
            toDate,
        });
    }, [productName, fromDate, toDate]);

    const handleChange = useCallback((e) => {
        const {name, value} = e.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    }, []);

    const handleFilter = useCallback(
        (e) => {
            e.preventDefault();

            navigate(
                getQueryString({
                    ...queryParams,
                    productName: form.productName || undefined,
                    fromDate: form.fromDate || undefined,
                    toDate: form.toDate || undefined,
                    page: FIRST_PAGE,
                }),
                {replace: true}
            );
        },
        [navigate, queryParams, form]
    );

    useEffect(() => {
        if (isError) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Failed to load stock items")
        }
    }, [error, isError]);

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-6'>
                <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6 font-semibold'>
                    Stock Item Purchases
                </h1>

                <div className='grid lg:grid-cols-4 gap-8'>
                    <Card className='lg:col-span-1 space-y-4'>
                        <form onSubmit={handleFilter}>
                            <CardHeader>
                                <CardTitle>Filter</CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-4">
                                {/* Product Name */}
                                <div className="space-y-1">
                                    <Label>Product Name</Label>
                                    <Input
                                        value={productName}
                                        onChange={handleChange}
                                    />
                                    <InputError field="productName"/>
                                </div>

                                {/* From Date */}
                                <div className="space-y-1">
                                    <StaredLabel
                                        label="From Date"
                                        field="fromDate"
                                    />
                                    <Input
                                        type="date"
                                        value={fromDate}
                                        onChange={handleChange}
                                    />
                                    <InputError field="fromDate"/>
                                </div>

                                {/* To Date */}
                                <div className="space-y-1">
                                    <StaredLabel
                                        label="To Date"
                                        field="toDate"
                                    />
                                    <Input
                                        type="date"
                                        value={toDate}
                                        onChange={handleChange}
                                    />
                                    <InputError field="toDate"/>
                                </div>
                            </CardContent>

                            <CardFooter>
                                <Button className="w-full bg-blue-600">
                                    Search
                                </Button>
                            </CardFooter>
                        </form>
                    </Card>

                    <div className='lg:col-span-3 space-y-4'>
                        <Table className="cursor-pointer bg-white w-[100%]">
                            <TableHeader>
                                <TableRow
                                    className="bg-blue-100 hover:bg-blue-200 transform transition-colors duration-200">
                                    <TableHead className="text-black text-base w-[80px]">ID</TableHead>
                                    <TableHead className="text-black text-base">Products</TableHead>
                                    <TableHead className="text-black text-base">Quantity</TableHead>
                                    <TableHead className="text-black text-base">Price</TableHead>
                                    <TableHead className="text-black text-base">Sub Total</TableHead>
                                    <TableHead className="text-black text-base">Remaining</TableHead>
                                    <TableHead className="text-black text-base">Date</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {stockItems.length > 0 ?
                                    stockItems.map((stockItem) => (
                                        <TableRow key={stockItem.id}>
                                            <TableCell>#{stockItem.id}</TableCell>
                                            <TableCell>{stockItem.productName}</TableCell>
                                            <TableCell>{stockItem.quantity}</TableCell>
                                            <TableCell>${stockItem.purchasePrice}</TableCell>
                                            <TableCell>${stockItem.subtotal}</TableCell>
                                            <TableCell>{stockItem.remaining}</TableCell>
                                            <TableCell>{formatDate(stockItem.createdAt)}</TableCell>
                                        </TableRow>
                                    ))
                                    :
                                    <TableRow>
                                        <TableCell colSpan={7} className="text-center">
                                            No sales found.
                                        </TableCell>
                                    </TableRow>
                                    
                                }
                            </TableBody>
                        </Table>

                        <PaginationButton totalPages={totalPages}/>
                    </div>
                </div>
            </div>
        </>
    )
}

export default StockItems
