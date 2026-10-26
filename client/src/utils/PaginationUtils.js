export const FIRST_PAGE = 1
export const ALL_SELECTED = "__all__";

const DEFAULT_PAGE_SIZE = 24
const DEFAULT_SORT_BY = "createdAt"
const DEFAULT_SORT_ORDER = "DESC"
const DEFAULT_SORT = DEFAULT_SORT_BY + "," + DEFAULT_SORT_ORDER
const MAX_PAGE_SIZE = DEFAULT_PAGE_SIZE;

/**
 * Clean query string builder (UI only)
 * - no pagination conversion
 * - no business logic
 */
export const getQueryString = (params = {}) => {
    const cleaned = Object.fromEntries(
        Object.entries(params).filter(
            ([_, v]) => v !== undefined && v !== null && v !== ""
        )
    )

    const qs = new URLSearchParams(cleaned).toString()
    return qs ? `?${qs}` : ""
}

const parsePage = (page) => {
    return Math.max(1, Number(page) || 1)
}

const parseSize = (size) => {
    const s = Number(size);

    if (!Number.isFinite(s) || s <= 0) {
        return DEFAULT_PAGE_SIZE;
    }

    return Math.min(MAX_PAGE_SIZE, s);
};

const parseSort = (sort = DEFAULT_SORT, allowedSortFields = []) => {
    const fields = allowedSortFields instanceof Set
        ? allowedSortFields
        : new Set(allowedSortFields);

    if (fields.size === 0) {
        return DEFAULT_SORT;
    }

    const [field, order] = sort.split(",")

    return fields.has(field)
        ? `${field},${order == "ASC" ? "ASC" : DEFAULT_SORT_ORDER}`
        : DEFAULT_SORT;
}

export const normalizeQuery = (q = {}, allowedSortFields = []) => {
    const page = parsePage(q.page)
    const size = parseSize(q.size)
    const sort = parseSort(q.sort, allowedSortFields)

    const { page: _p, size: _s, sort: _so, ...rest } = q;

    return {
        page,
        size,
        sort,
        ...rest,
    };
}

const isInvalidPageNo = (page, totalPages) => {
    return page < FIRST_PAGE || page > totalPages;
}

export const redirectWhenInvalidPage = ({page, totalPages, navigate, queryParams}) => {
    if (!totalPages || !queryParams.page) return;

    if (isInvalidPageNo(page, totalPages)) {
        navigate(
            getQueryString({
                ...queryParams,
                page: FIRST_PAGE,
            }),
            {replace: true}
        );
    }
};


export const updateQueryWhenParamChange = ({queryParams, newParams, navigate}) => {
    const cleanedParams = Object.fromEntries(
        Object.entries({
            ...queryParams,
            ...newParams,
            page: FIRST_PAGE,
        }).map(([key, value]) => {
            if (value == ALL_SELECTED || value == "" || value == null) {
                return [key, undefined];
            }
            return [key, value];
        })
    );

    navigate(getQueryString(cleanedParams), {replace: true});
};

export const getSelectValue = (value) => value ?? ALL_SELECTED;