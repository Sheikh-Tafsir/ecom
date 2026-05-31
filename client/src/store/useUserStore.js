import { create } from "zustand";
import { getAccessToken, getAccessUser, isAccessTokenExpired, saveAccessToken } from "../utils/AuthUtils";
import { logout } from "../pages/auth/AuthService";

export const useUserStore = create((set, get) => ({
    user: null,

    init: () => set({ user: getAccessUser() }),

    login: (token) => {
        set({ user: saveAccessToken(token) });
    },

    logout: async () => {
        await logout();
        set({ user: null });
    },

    isAuthenticated: () => {
        return !!get().user;
    },

    getAccessToken: () => getAccessToken(),

    isTokenExpired: () => {
        return isAccessTokenExpired();
    },
}));
