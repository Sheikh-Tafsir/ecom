import {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table.jsx"
import {Axios} from '@/services/http/Axios.js';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx';
import {
    toastInitialState,
    formatDate
} from '@/utils/index.js';
import {ToastAlert} from '@/components/common/ToastAlert.jsx';
import {useQuery} from "@tanstack/react-query";
import {TOAST_TYPE} from "@/utils/enums.js";

const StockDetails = () => {
    const {id} = useParams();

    const [toastData, setToastData] = useState(toastInitialState);

    const fetchStockItems = async () => {
        const response = await Axios.get(`/stocks/${id}`)
        return response.data.data
    }

    const {
        data: stock,
        isFetching: isPageLoading,
        isError,
        error
    } = useQuery({
        queryKey: ["stockItems", id],
        queryFn: fetchStockItems,
        enabled: !!id,
    });

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load stock items", TOAST_TYPE.ERROR);
        }
    }, [isError]);

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-6'>
                <div className="flex justify-between items-center mb-6">
                    <h1 className='text-2xl lg:text-2xl xl:text-3xl font-bold'>Stock Detail</h1>
                </div>

                <p><b>Total cost:</b> ${stock.totalCost}</p>
                <p><b>Date::</b> {formatDate(stock.createdAt)}</p>
                <br/>

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
                            {stock?.items?.map((stockItem) => (
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

export default StockDetails
