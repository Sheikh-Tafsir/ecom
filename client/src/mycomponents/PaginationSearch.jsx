import React, { useCallback, useMemo, useState, useEffect } from "react";
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search } from "lucide-react"

import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { FIRST_PAGE, getQueryString } from '../utils/PaginationUtils';

const PaginationSearch = ({ moduleName = "items" }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryParams = useMemo(
        () => Object.fromEntries(searchParams.entries()),
        [searchParams]
    );

    const [search, setSearch] = useState(queryParams.search || '');
    const [debouncedSearch, setDebouncedSearch] = useState(search);

    useEffect(() => {
        const timeout = setTimeout(() => {
            setDebouncedSearch(search);
        }, 400); // 400ms debounce delay (ERP UX standard)
        return () => clearTimeout(timeout);
    }, [search]);

    const handleSearch = useCallback(
        (e) => {
            e?.preventDefault();

            const navQueryParams = {
                ...queryParams,
                search: debouncedSearch.trim(),
                page: FIRST_PAGE,
            };
            navigate(getQueryString(navQueryParams));
        },
        [debouncedSearch, navigate, queryParams]
    );

    /** Trigger search automatically after debounce */
    useEffect(() => {
        if (debouncedSearch !== queryParams.search) {
            handleSearch();
        }
    }, [debouncedSearch, queryParams.search, handleSearch]);

    return (
        <form className="flex w-full md:w-1/2 mb-6 mx-auto" onSubmit={handleSearch}>
            <Input
                type="text"
                placeholder={`Search ${moduleName}...`}
                className="px-4 py-2 border border-gray-300 -md w-full"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />
            <Button className="rounded-l-none bg-blue-600 hover:bg-blue-700">
                <Search className="w-4 h-4"/>
            </Button>
        </form>
    )
}

export default React.memo(PaginationSearch);