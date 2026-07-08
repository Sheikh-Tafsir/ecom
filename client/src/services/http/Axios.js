import axios from 'axios';

import {getAccessToken, removeAccessToken, removeCart, saveAccessToken} from '@/utils/AuthUtils';
import { notify } from '@/components/common/notification';
import { TOAST_TYPE } from '@/utils/enums';

const API_PATH = import.meta.env.VITE_API_PATH;

const AuthAxios = axios.create({
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
        const token = getAccessToken();

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

        if (response.status == 401 && !originalRequest?._retry) {
            console.log("First 401");
            originalRequest._retry = true;

            try {
                console.log("Refreshing...");
                const token = await refreshAccessToken();
                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${token}`;

                return Axios.request(originalRequest);
            } catch (err) {
                notify(TOAST_TYPE.INFO, "Session expired. Please log in again.");

                await logout();

                return Promise.reject(err);
            }
        }

        return Promise.reject(error);
    }
);

let isRefreshing = false;
let refreshSubscribers = [];

const refreshAccessToken = async () => {
    if (isRefreshing) {
        return new Promise((resolve, reject) => {
            addRefreshSubscriber(resolve, reject);
        });
    }

    isRefreshing = true;

    try {

        const response = await AuthAxios.post("/auth/access-token/refresh");
        const token = response.data.data;
        
        saveAccessToken(token);

        resolveRefreshSubscribers(token);

        return token;
    } catch (error) {
        console.error("Token refresh failed:", error);
        rejectRefreshSubscribers(error);

        throw error;
    } finally {
        isRefreshing = false;
    }
};

/**
 * Add requests waiting for token refresh.
 */
const addRefreshSubscriber = (resolve, reject) => {
    refreshSubscribers.push({resolve, reject});
};

/**
 * Resolve all waiting requests.
 */
const resolveRefreshSubscribers = (token) => {
    refreshSubscribers.forEach(({resolve}) => resolve(token));
    refreshSubscribers = [];
};

/**
 * Reject all waiting requests.
 */
const rejectRefreshSubscribers = (error) => {
    refreshSubscribers.forEach(({reject}) => reject(error));
    refreshSubscribers = [];
};

const logout = async () => {
    try {
        await AuthAxios.post("/auth/logout");
    } catch (error) {
        console.error("Logout failed:", error);
    } finally {
        removeAccessToken();
        removeCart();

        isRefreshing = false;
        refreshSubscribers = [];

        window.location.replace("/");
    }
};

export {API_PATH, Axios, AuthAxios, refreshAccessToken, logout};