import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { format } from "date-fns";
import { useQuery, keepPreviousData } from "@tanstack/react-query";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

import { Axios } from "@/services/http/Axios";
import PaginationButton from "@/components/common/PaginationButton";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";

import {
    REGULAR_DATE_FORMAT,
    toastInitialState,
} from "@/utils";
import {FIRST_PAGE, getQueryString, normalizeQuery, redirectWhenInvalidPage} from '@/utils/PaginationUtils';

import { Label } from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ToastAlert } from "@/components/common/ToastAlert";
import InputError from "@/components/common/InputError";
import StaredLabel from "@/components/common/StaredLabel";
import { TOAST_TYPE } from "@/utils/enums";

const fetchSales = async ({ queryKey }) => {
    const [, params] = queryKey;

    const response = await Axios.get("/sales", {
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

export default function Sales() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()),[searchParams]);

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, []),
            productName: queryParams.productName || "",
            fromDate: queryParams.fromDate || "",
            toDate: queryParams.toDate || "",
        }),
        [queryParams]
    );
    const { page, productName, fromDate, toDate } = filters;

    const [form, setForm] = useState({
        productName: "",
        fromDate: "",
        toDate: "",
    });
    const [toastData, setToastData] = useState(toastInitialState);

        const {
        data,
        isFetching: isPageLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["sales", filters],
        queryFn: fetchSales,
        placeholderData: keepPreviousData,
    });

    const sales = data?.content ?? [];
    const totalPages = data?.totalPages ?? FIRST_PAGE;

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load sales", TOAST_TYPE.ERROR);
        }
    }, [error, isError]);

    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    const showToast = useCallback((message, type) => {
        setToastData({
            message,
            type,
            id: Date.now(),
        });
    }, []);

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
            {isPageLoading && <PageLoadingOverlay />}

            <div className="container pb-8 pt-8">
                <h1 className='text-center text-2xl lg:text-2xl xl:text-3xl mb-6 font-semibold'>Sales</h1>

                <div className="grid lg:grid-cols-4 gap-8">
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

                    <div className="lg:col-span-3 space-y-4">
                        <Table className="bg-white w-full">
                            <TableHeader>
                                <TableRow className="bg-blue-100">
                                    <TableHead>Name</TableHead>
                                    <TableHead>Buying Price</TableHead>
                                    <TableHead>Selling Price</TableHead>
                                    <TableHead>Quantity</TableHead>
                                    <TableHead>Profit</TableHead>
                                    <TableHead>Date</TableHead>
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {sales.map((item) => (
                                    <TableRow key={item.id}>
                                        <TableCell>
                                            {item.product.name}
                                        </TableCell>
                                        <TableCell>
                                            {item.buyingPrice}
                                        </TableCell>
                                        <TableCell>
                                            {item.sellingPrice}
                                        </TableCell>
                                        <TableCell>
                                            {item.quantity}
                                        </TableCell>
                                        <TableCell>
                                            {item.profit}
                                        </TableCell>
                                        <TableCell>
                                            {format(
                                                new Date(item.createdAt),
                                                REGULAR_DATE_FORMAT
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>

                        {sales.length > 0 ? (
                            <PaginationButton totalPages={totalPages} />
                        ) : (
                            <div className="bg-white p-4 text-center">
                                Nothing to show
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    );
}