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
import {Button} from '@/components/ui/button.jsx';
import {Plus} from 'lucide-react';
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {keepPreviousData, useQuery} from "@tanstack/react-query";
import {TOAST_TYPE} from "@/utils/enums.js";

const Stocks = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams()
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams), [queryParams])
    const {page} = filters

    const [toastData, setToastData] = useState(toastInitialState);

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

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load stocks", TOAST_TYPE.ERROR);
        }
    }, [error, isError]);

    // Page validation (safe)
    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-6'>
                <div className="flex justify-between items-center mb-6">
                    <h1 className='text-2xl lg:text-2xl xl:text-3xl font-bold'>Stock Purchases</h1>
                    <Button onClick={() => navigate('/stocks/create')} className="bg-blue-900">
                        <Plus className="h-4 w-4 mr-2"/> New Purchase
                    </Button>
                </div>

                <div className='space-y-4'>
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

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    )
}

export default Stocks
