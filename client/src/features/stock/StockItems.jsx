import {useState, useEffect, useMemo} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.jsx"
import {Axios} from '@/services/http/Axios.js';
import PaginationButton from '@/components/common/PaginationButton.jsx';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {
    FIRST_PAGE,
    toastInitialState,
    redirectWhenInvalidPage,
    normalizeQuery, formatDate
} from '@/utils/index.js';
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {keepPreviousData, useQuery} from "@tanstack/react-query";
import {TOAST_TYPE} from "@/utils/enums.js";

const StockItems = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams), [queryParams])
    const {page} = filters

    const [toastData, setToastData] = useState(toastInitialState);

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

    const {
        data, isFetching: isPageLoading, isError, error
    } = useQuery({
        queryKey: ["stockItems", filters],
        queryFn: fetchStockItems,
        placeholderData: keepPreviousData,
        keepPreviousData: true,
    })

    const stockItems = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load stock items", TOAST_TYPE.ERROR);
        }
    }, [isError]);

    // Page validation (safe)
    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-6'>
                <div className="flex justify-between items-center mb-6">
                    <h1 className='text-2xl lg:text-2xl xl:text-3xl font-bold'>Stock Item Purchases</h1>
                </div>

                <div className='space-y-4'>
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
                            {stockItems.map((stockItem) => (
                                <TableRow key={stockItem.id}>
                                    <TableCell>#{stockItem.id}</TableCell>
                                    <TableCell>{stockItem.productName}</TableCell>
                                    <TableCell>{stockItem.quantity}</TableCell>
                                    <TableCell>${stockItem.purchasePrice}</TableCell>
                                    <TableCell>${stockItem.subtotal}</TableCell>
                                    <TableCell>{stockItem.remaining}</TableCell>
                                    <TableCell>{formatDate(stockItem.createdAt)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>

                    {stockItems?.length > 0 ?
                        <PaginationButton totalPages={totalPages}/>
                        :
                        <div className='w-full flex bg-white p-4 border rounded-md'>
                            <p className='mx-auto'>No stock records found</p>
                        </div>
                    }
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

export default StockItems
