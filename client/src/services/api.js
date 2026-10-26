import {AuthenticatedAxios, PublicAxios} from "@/services/http/Axios";

/**
 * Factory for list-page query functions.
 *
 * @param {string}   endpoint  - API path (e.g. "/sales")
 * @param {Object}   [options]
 * @param {function} [options.mapParams] - (params) => extra query params to merge
 * @param {boolean}  [options.isPublic]  - use PublicAxios instead of AuthenticatedAxios
 * @returns {function} react-query queryFn
 */
export const createListFetcher = (endpoint, {mapParams, isPublic} = {}) => {
    const axios = isPublic ? PublicAxios : AuthenticatedAxios;

    return async ({queryKey}) => {
        const [, params] = queryKey;

        const response = await axios.get(endpoint, {
            params: {
                page: params.page - 1,
                sort: params.sort,
                size: params.size,
                ...mapParams?.(params),
            },
        });

        return response.data.data;
    };
};
