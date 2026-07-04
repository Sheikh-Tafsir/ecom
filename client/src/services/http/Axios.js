import axios from 'axios';

import {getAccessToken,isAccessTokenExpired, removeAccessToken, saveAccessToken} from '@/utils/AuthUtils';
import { notify } from '@/components/common/notification';
import { TOAST_TYPE } from '@/utils/enums';

const API_PATH = import.meta.env.VITE_API_PATH;

const AxiosNoInterceptor = axios.create({
    baseURL: API_PATH,
    withCredentials: true,
    timeout: 5000,
});

const Axios = axios.create({
    baseURL: API_PATH,
    withCredentials: true,
    timeout: 5000,
});

Axios.interceptors.request.use(
    async (config) => {
        const token = await getValidAccessToken();

        if (token) {
            config.headers = config.headers || {};
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

Axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error?.config;
        const response = error?.response;

        if (!response) {
            return Promise.reject(error);
        }

        const isRefreshRequest = originalRequest?.url?.includes("/auth/access-token/refresh");

        if (response.status == 401 && !originalRequest?._retry && !isRefreshRequest) {
            originalRequest._retry = true;

            try {
                const token = await getValidAccessToken();
                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${token}`;

                return Axios.request(originalRequest);
            } catch (err) {
                await logout();
                notify(TOAST_TYPE.INFO, "Session expired. Please log in again.");
                redirectAfterLogout();
                return Promise.reject(err);
            }
        }

        return Promise.reject(error);
    }
);

let isRefreshing = false;
let queue = [];

/**
 * Optional proactive refresh before request.
 * Enable if desired.
 */
const getValidAccessToken = async () => {
    let token = getAccessToken();

    if (!token) {
        return null;
    }

    if (!isAccessTokenExpired(token)) {
        return token;
    }

    if (isRefreshing) {
        return new Promise((resolve, reject) => {
            subscribeTokenRefresh(resolve, reject);
        });
    }

    isRefreshing = true;

    try {
        const newToken = await refreshAccessToken();

        onRefreshSuccess(newToken);

        return newToken;
    } catch (error) {
        onRefreshFailure(error);
        await logout();
        redirectAfterLogout();
        throw error;
    } finally {
        isRefreshing = false;
    }
};

/**
 * Add requests waiting for token refresh.
 */
const subscribeTokenRefresh = (resolve, reject) => {
    queue.push({resolve, reject});
};

/**
 * Resolve all waiting requests.
 */
const onRefreshSuccess = (token) => {
    queue.forEach(({resolve}) => resolve(token));
    queue = [];
};

/**
 * Reject all waiting requests.
 */
const onRefreshFailure = (error) => {
    queue.forEach(({reject}) => reject(error));
    queue = [];
};


const refreshAccessToken = async () => {
    try {
        const response = await AxiosNoInterceptor.post('/auth/access-token/refresh');
        const token = response.data.data;
        saveAccessToken(token)
        return token;
    } catch (error) {
        console.error(error);
        throw error;
    }
};

const logout = async () => {
    try {
        await AxiosNoInterceptor.post("/auth/logout");
    } catch (error) {
        console.error("Logout failed:", error);
    } finally {
        removeAccessToken();
    }
};

const redirectAfterLogout = () => {
    isRefreshing = false;
    queue = [];

    window.location.replace("/");
}

export {API_PATH, Axios, AxiosNoInterceptor, refreshAccessToken, logout};