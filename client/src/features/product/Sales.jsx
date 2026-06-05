import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { format } from "date-fns";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Axios } from '@/services/http/Axios';
import PaginationButton from '@/components/common/PaginationButton';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import { FIRST_PAGE, getQueryString, REGULAR_DATE_FORMAT } from '@/utils';
import { Label } from '@/components/ui/label';
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ToastAlert } from '@/components/common/ToastAlert';
import { TOAST_TYPE } from '@/utils/enums';

const Sales = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = Object.fromEntries(searchParams.entries());

    const [isPageLoading, setIsPageLoading] = useState(true);
    const [sales, setSales] = useState([]);
    const [totalPages, setTotalPages] = useState(FIRST_PAGE);
    const [searchFilter, setSearchFilter] = useState({
        productId: '',
        fromDate: '',
        toDate: ''
    });
    const [errors, setErrors] = useState({});
    const [toastData, setToastData] = useState({ message: "", type: "", id: 0 });

    const page = parseInt(queryParams.page) || FIRST_PAGE;

    const handleError = (error) => {
        setErrors(error.response?.data || { global: error.message });
    };

    useEffect(() => {
        if (page <= 0 || totalPages < page) {
            navigate("/sales", { replace: true });
        };

        const fetchSales = async () => {
            setIsPageLoading(true);

            try {
                const response = await Axios.get('/sales', {
                    params: {
                        ...queryParams,
                    }
                });

                setSales(response.data.data.content);
                setTotalPages(response.data.data.totalPages);
            } catch (error) {
                console.error('Error getting user list', error);
                showToast("Could not get sales", TOAST_TYPE.ERROR);
                handleError(error);
            } finally {
                setIsPageLoading(false);
            }
        };

        fetchSales();
    }, [searchParams]);

    const handleSearch = (e) => {
        e.preventDefault();

        const navQueryParams = {
            ...queryParams,
            ...searchFilter,
        };

        navigate(getQueryString(navQueryParams), { replace: true });
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setSearchFilter((prev) => ({ ...prev, [name]: value }));
    };

    const showToast = (message, type) => {
        setToastData({ message, type, id: Date.now() }) // ensure uniqueness
    }

    return (
        <div className='container pb-8'>
            {isPageLoading && <PageLoadingOverlay />}

            <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6'>Sales</h1>

            <div className='grid lg:grid-cols-3 gap-8'>
                <div className='lg:col-span-2 space-y-4'>
                    <Table className="cursor-pointer bg-white w-[100%]">
                        <TableHeader>
                            <TableRow className="bg-blue-100 hover:bg-blue-200 transform transition-colors duration-200">
                                <TableHead className="text-black text-base">Name</TableHead>
                                <TableHead className="text-black text-base">Buying Price</TableHead>
                                <TableHead className="text-black text-base">Selling Price</TableHead>
                                <TableHead className="text-black text-base">Quantity</TableHead>
                                <TableHead className="text-black text-base">Profit</TableHead>
                                <TableHead className="text-black text-base">Date</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {sales.map((item) => (
                                <TableRow key={item.id}>
                                    <TableCell>{item.product.name}</TableCell>
                                    <TableCell>{item.buyingPrice}</TableCell>
                                    <TableCell>{item.sellingPrice}</TableCell>
                                    <TableCell>{item.quantity}</TableCell>
                                    <TableCell>{item.profit}</TableCell>
                                    <TableCell>{format(item.createdAt, REGULAR_DATE_FORMAT)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>

                    {sales?.length > 0 ?
                        <PaginationButton totalPages={totalPages} />
                        :
                        <div className='w-full flex bg-white p-4'>
                            <p className='mx-auto'>Nothing to show</p>
                        </div>
                    }
                </div>

                <Card className="g:col-span-1">
                    <form onSubmit={handleSearch}>
                        <CardHeader>
                            <CardTitle>Filter </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className='space-y-1'>
                                <Label>Product Id: </Label>
                                <Input
                                    name="productId"
                                    type="number"
                                    value={searchFilter.productId}
                                    onChange={handleInputChange}
                                />
                                {errors.productId && <p className='validation-error'>{errors.productId}</p>}
                            </div>
                            <div className='space-y-1'>
                                <Label>Date From: </Label>
                                <Input
                                    name="fromDate"
                                    type="date"
                                    value={searchFilter.fromDate}
                                    onChange={handleInputChange}
                                />
                                {errors.fromDate && <p className='validation-error'>{errors.fromDate}</p>}
                            </div>
                            <div className='space-y-1'>
                                <Label>Date To: </Label>
                                <Input
                                    name="toDate"
                                    type="date"
                                    value={searchFilter.toDate}
                                    onChange={handleInputChange}
                                />
                                {errors.toDate && <p className='validation-error'>{errors.toDate}</p>}
                            </div>
                        </CardContent>
                        <CardFooter>
                            <Button type="submit" className="w-full cursor-pointer bg-blue-600">
                                Search
                            </Button>
                        </CardFooter>
                    </form>
                </Card>
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </div>
    )
}

export default Sales