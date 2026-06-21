import {useState, useEffect, useMemo, useCallback} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {keepPreviousData, useQuery} from "@tanstack/react-query";
import {Plus} from 'lucide-react';

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.jsx"
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Axios} from '@/services/http/Axios.js';
import PaginationButton from '@/components/common/PaginationButton.jsx';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {
    FIRST_PAGE,
    toastInitialState,
    redirectWhenInvalidPage,
    formatDate,
    normalizeQuery
} from '@/utils/index.js';
import {Button} from '@/components/ui/button.jsx';
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {TOAST_TYPE} from "@/utils/enums.js";
import InputError from "@/components/common/InputError";
import StaredLabel from "@/components/common/StaredLabel";

const fetchStocks = async ({queryKey}) => {
    const [, params] = queryKey

    const response = await Axios.get("/stocks", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            size: params.size,
        },
    })

    return response.data.data
}

const Stocks = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams, []), [queryParams])
    const { page, productName, fromDate, toDate } = filters;

    const [toastData, setToastData] = useState(toastInitialState);
    const [form, setForm] = useState({
        productName: "",
        fromDate: "",
        toDate: "",
    });

    const {
        data, isFetching: isPageLoading, isError, error
    } = useQuery({
        queryKey: ["stocks", filters],
        queryFn: fetchStocks,
        placeholderData: keepPreviousData,
        keepPreviousData: true,
    })

    const stocks = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load stocks", TOAST_TYPE.ERROR);
        }
    }, [error, isError]);

     useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams]);

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    useEffect(() => {
        setForm({
            productName,
            fromDate,
            toDate,
        });
    }, [productName, fromDate, toDate]);

    const handleChange = useCallback((e) => {
        const { name, value } = e.target;

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
                { replace: true }
            );
        },
        [navigate, queryParams, form]
    );

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-8'>
                <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6 font-semibold'>
                    Stock Purchases
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
                                    <InputError field="productName" />
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
                                    <InputError field="fromDate" />
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
                                    <InputError field="toDate" />
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
                        {/* <Button onClick={() => navigate('/stocks/create')} className="bg-blue-900">
                            <Plus className="h-4 w-4 mr-2"/> New Purchase
                        </Button> */}

                        <Table className="cursor-pointer bg-white w-[100%]">
                            <TableHeader>
                                <TableRow
                                    className="bg-blue-100 hover:bg-blue-200 transform transition-colors duration-200">
                                    <TableHead className="text-black text-base w-[80px]">ID</TableHead>
                                    <TableHead className="text-black text-base">Total Cost</TableHead>
                                    <TableHead className="text-black text-base">Date</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {stocks.map((stock) => (
                                    <TableRow key={stock.id} onClick={() => navigate(`/stocks/${stock.id}`)}>
                                        <TableCell>#{stock.id}</TableCell>
                                        <TableCell>${stock.totalCost}</TableCell>
                                        <TableCell>{formatDate(stock.createdAt)}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>

                        {stocks?.length > 0 ?
                            <PaginationButton totalPages={totalPages}/>
                            :
                            <div className='w-full flex bg-white p-4 border rounded-md'>
                                <p className='mx-auto'>No stock records found</p>
                            </div>
                        }
                    </div>
                </div>
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    )
}

export default Stocks
