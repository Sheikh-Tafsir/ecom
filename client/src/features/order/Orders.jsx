import React, {useEffect, useMemo, useState, useCallback} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery, keepPreviousData} from "@tanstack/react-query";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import {Axios} from '@/services/http/Axios';
import PaginationButton from '@/components/common/PaginationButton';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import {formatDateAndTime, hasPermission} from '@/utils';
import {FIRST_PAGE, getQueryString, normalizeQuery, redirectWhenInvalidPage} from '@/utils/PaginationUtils';
import {Label} from '@/components/ui/label';
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Input} from '@/components/ui/input';
import {Button} from '@/components/ui/button';
import {ORDER_STATUS, PERMISSION, TOAST_TYPE} from '@/utils/enums';
import InputError from "@/components/common/InputError";
import StaredLabel from "@/components/common/StaredLabel";
import {notify} from "@/components/common/notification";
import {queryClient} from "@/services/queryClient";
import {useUserStore} from "@/store/useUserStore";

const fetchOrders = async ({queryKey}) => {
    const [, params] = queryKey;

    const response = await Axios.get("/orders", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            productName: params.productName || undefined,
            fromDate: params.fromDate || undefined,
            toDate: params.toDate || undefined,
        },
    });

    return response.data.data;
};

const Orders = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, []),
            productName: queryParams.productName || "",
            fromDate: queryParams.fromDate || "",
            toDate: queryParams.toDate || "",
        }),
        [queryParams]
    );
    const {page, productName, fromDate, toDate} = filters;

    const {user} = useUserStore();

    const [form, setForm] = useState({
        productName: "",
        fromDate: "",
        toDate: "",
    });

    const {
        data,
        isPending: isPageLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["orders", filters],
        queryFn: fetchOrders,
        placeholderData: keepPreviousData,
    });

    const orders = data?.content ?? [];
    const totalPages = data?.totalPages ?? FIRST_PAGE;

    useEffect(() => {
        if (isError) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Failed to show orders")
        }
    }, [error, isError]);

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

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

    const changeOrderStatus = async (id, status) => {
        try {
            await Axios.patch(`/orders/${id}/status`, {
                status
            });

            await queryClient.invalidateQueries({queryKey: ["orders"]});
            notify(TOAST_TYPE.SUCCESS, `Updated order with ID ${id} status changed to ${status}`)
        } catch (error) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, error.response.data.errors.global[0])
        }
    }

    const cancelOrder = async (id) => {
        try {
            await Axios.patch(`/orders/${id}/cancel`);

            await queryClient.invalidateQueries({queryKey: ["orders"]});
            notify(TOAST_TYPE.SUCCESS, `Cancelled order with ID ${id}`)
        } catch (error) {
            console.error(error);
            notify(TOAST_TYPE.ERROR, error.response.data.errors.global[0])
        }
    }

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className='container pb-8 pt-8'>
                <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6 font-semibold'>Orders</h1>

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
                                    <TableHead className="text-black text-base">Name</TableHead>
                                    <TableHead className="text-black text-base">Total Price</TableHead>
                                    <TableHead className="text-black text-base">Date</TableHead>
                                    <TableHead className="text-black text-base">Status</TableHead>
                                    <TableHead className="text-black text-base">Action</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {orders.length > 0 ?
                                    orders.map((item) => (
                                        <TableRow key={item.id}>
                                            <TableCell>{item.userName}</TableCell>
                                            <TableCell>{item.totalPrice}</TableCell>
                                            <TableCell>{formatDateAndTime(item.createdAt)}</TableCell>
                                            <TableCell>{item.status}</TableCell>
                                            <TableCell className="flex gap-2">
                                                <Button className="text-blue-600 hover:text-white hover:bg-blue-600"
                                                        onClick={() => navigate(`/orders/${item.id}`)} variant="outline"
                                                >
                                                    View
                                                </Button>
                                                {ORDER_STATUS.PENDING == item.status && (
                                                    hasPermission(user, PERMISSION.ADMIN) || hasPermission(user, PERMISSION.SUPER_ADMIN)) ? (
                                                    <>
                                                        <Button
                                                            className="text-green-600 hover:text-white hover:bg-green-600"
                                                            onClick={() => changeOrderStatus(item.id, ORDER_STATUS.ACCEPTED)}
                                                            variant="outline"
                                                        >
                                                            Accept
                                                        </Button>
                                                        <Button
                                                            className="text-red-600 hover:text-white hover:bg-red-600"
                                                            onClick={() => changeOrderStatus(item.id, ORDER_STATUS.REJECTED)}
                                                            variant="outline"
                                                        >
                                                            Reject
                                                        </Button>
                                                    </>
                                                ) : (
                                                    <Button
                                                        className="text-red-600 hover:text-white hover:bg-red-600"
                                                        onClick={() => cancelOrder(item.id)}
                                                        variant="outline"
                                                    >
                                                        Cancel
                                                    </Button>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    ))
                                    :
                                    <TableRow>
                                        <TableCell colSpan={5} className="text-center">
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

export default Orders