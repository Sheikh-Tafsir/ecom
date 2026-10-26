import {useEffect, useMemo} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery, keepPreviousData} from "@tanstack/react-query";

import {
    normalizeQuery,
    redirectWhenInvalidPage,
    updateQueryWhenParamChange,
    FIRST_PAGE,
} from "@/utils/PaginationUtils";
import {TOAST_TYPE} from "@/constants/app.constants";
import {toastify} from "@/common/toastify.js";

/**
 * Centralised hook for list pages with pagination, filtering, and URL-driven state.
 *
 * @param {Object} options
 * @param {function} options.queryKey       - (filters) => react-query key array
 * @param {function} options.queryFn        - react-query fetch function
 * @param {Set|Array} [options.allowedSortFields=[]] - valid sort fields
 * @param {function} [options.extraFilters] - (queryParams) => extra filter fields to merge
 * @param {string}   [options.errorMessage] - toast message on query error
 * @param {function} [options.onError]      - optional callback for custom error handling (receives error, e.g. to call handleErrors)
 *
 * @returns {{
 *   items: Array,
 *   totalPages: number,
 *   totalElements: number,
 *   filters: Object,
 *   queryParams: Object,
 *   query: import("@tanstack/react-query").UseQueryResult,
 *   updateQuery: function,
 *   navigate: function,
 * }}
 */
export const useListPage = ({
    queryKey,
    queryFn,
    allowedSortFields = [],
    extraFilters,
    errorMessage = "Failed to load data",
    onError,
}) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryParams = useMemo(
        () => Object.fromEntries(searchParams.entries()),
        [searchParams]
    );

    const filters = useMemo(() => {
        const base = normalizeQuery(queryParams, allowedSortFields);
        const extra = extraFilters ? extraFilters(queryParams) : {};
        return {...base, ...extra};
    }, [queryParams]);

    const query = useQuery({
        queryKey: queryKey(filters),
        queryFn,
        placeholderData: keepPreviousData,
    });

    const {page} = filters;
    const totalPages = query.data?.totalPages ?? FIRST_PAGE;
    const totalElements = query.data?.totalElements ?? 0;
    const items = query.data?.content ?? [];

    // Redirect when page is out of range
    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams});
    }, [page, totalPages, navigate, queryParams]);

    // Toast on error
    useEffect(() => {
        if (!query.isError) return;

        toastify(TOAST_TYPE.ERROR, errorMessage);
        onError?.(query.error);
    }, [query.isError, query.error]);

    const updateQuery = (newParams) => {
        updateQueryWhenParamChange({queryParams, newParams, navigate});
    };

    return {
        items,
        totalPages,
        totalElements,
        filters,
        queryParams,
        query,
        updateQuery,
        navigate,
    };
};
