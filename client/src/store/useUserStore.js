import { create } from "zustand";
import {ACCESS_TOKEN, getAccessToken, getAccessUser, isAccessTokenExpired, saveAccessToken} from "@/utils/AuthUtils";
import {AxiosNoInterceptor} from "@/services/http/Axios.js";

export const useUserStore = create((set, get) => ({
    user: getAccessUser(),

    init: () => set({ user: getAccessUser() }),

    login: (token) => {
        set({ user: saveAccessToken(token) });
    },

    logout: async () => {
        try {
            await AxiosNoInterceptor.post("/auth/logout");
        } catch (error) {
            console.error("Logout failed:", error);
        } finally {
            set({ user: null });
            localStorage.removeItem(ACCESS_TOKEN);
            window.location.replace("/");
        }
    },

    isAuthenticated: () => {
        return !!get().user;
    },

    getAccessToken: () => getAccessToken(),
}));
