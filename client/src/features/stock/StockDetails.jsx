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
import {formatDate} from '@/utils/index.js';
import {useQuery} from "@tanstack/react-query";
import {TOAST_TYPE} from "@/utils/enums.js";
import {notify} from '@/components/common/notification';

const fetchStocks = async () => {
    const response = await Axios.get(`/stocks/${id}`)
    return response.data.data
}

const StockDetails = () => {
    const {id} = useParams();

    const {
        data: stock,
        isFetching: isPageLoading,
        isError,
        error
    } = useQuery({
        enabled: !!id,
        queryKey: ["stock", id],
        queryFn: fetchStocks,
    });

    useEffect(() => {
        if (isError) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Failed to load stock items")
        }
    }, [error, isError]);

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-8'>
                <div className="flex justify-between items-center mb-6">
                    <h1 className='text-2xl lg:text-2xl xl:text-3xl font-bold mx-auto'>Stock Detail</h1>
                </div>

                <p><b>Total cost:</b> ${stock?.totalCost || '0.00'}</p>
                <p><b>Date::</b> {formatDate(stock?.createdAt)}</p>
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
        </>
    )
}

export default StockDetails
