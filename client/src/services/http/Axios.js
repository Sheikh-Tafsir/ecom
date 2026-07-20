import axios from 'axios';

import {getAccessToken, removeAccessToken, removeCart, saveAccessToken} from '@/utils/AuthUtils';
import {toastify} from '@/common/toastify.js';
import {TOAST_TYPE} from '@/utils/enums';

const API_PATH = import.meta.env.VITE_API_PATH;
const TIMEOUT = 5000;

const AuthAxios = axios.create({
    baseURL: API_PATH,
    withCredentials: true,
    timeout: TIMEOUT,
});

const Axios = axios.create({
    baseURL: API_PATH,
    withCredentials: true,
    timeout: TIMEOUT,
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
            if (!navigator.onLine) {
                toastify(TOAST_TYPE.ERROR, "You appear to be offline");
            } else {
                toastify(TOAST_TYPE.ERROR, "Cannot reach the server. Please try again after some time");
            }

            return Promise.reject(error);
        }

        if (response.status == 401) {
            if (!originalRequest?._retry) {
                originalRequest._retry = true;

                let token;

                try {
                    token = await refreshAccessToken();
                } catch (err) {
                    console.error(err)

                    if (err.response?.status == 401) {
                        toastify(TOAST_TYPE.INFO, "Session expired. Please log in again.");
                        await logout();
                    } else {
                        toastify(TOAST_TYPE.ERROR, "An error occurred. Please try again after some time.");
                    }

                    return Promise.reject(err);
                }

                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${token}`;

                return Axios.request(originalRequest);
            }

            console.error("Authorization error even after refreshing access token", error);

            toastify(TOAST_TYPE.INFO, "Session expired. Please log in again.");
            await logout();
        }

        return Promise.reject(error);
    }
);

let isRefreshing = false;
let refreshSubscribers = [];

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