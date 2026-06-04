import {parsePage, parseSize, parseSort} from "@/utils/index.js";

const ALLOWED_SORT_FIELDS = new Set([
    "createdAt",
    "name",
])

export const normalizeQuery = (q = {}) => {
    const page = parsePage(q.page)
    const size = parseSize(q.size)
    const sort = parseSort(q.sort, ALLOWED_SORT_FIELDS)

    return {
        page,
        size,
        sort,
        role: q.role,
        status: q.status,
    }
}