import {parsePage, parseSize, parseSort} from "@/utils/index.js";

const ALLOWED_SORT_FIELDS = new Set([
    "createdAt",
    "price",
    "name",
    "rating",
])

export const normalizeQuery = (q = {}) => {
    const page = parsePage(q.page)
    const size = parseSize(q.size)
    const sort = parseSort(q.sort, ALLOWED_SORT_FIELDS)

    return {
        page,
        size,
        sort,
        search: q.search || "",
        category: q.category || "",
    }
}