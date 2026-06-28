import {create} from "zustand";
import {
    getAccessUser,
    saveAccessToken
} from "@/utils/AuthUtils";
import { logout} from "@/services/http/Axios.js";

export const useUserStore = create((set, get) => ({
    user: getAccessUser(),

    init: () => set({user: getAccessUser()}),

    login: (token) => {
        set({user: saveAccessToken(token)});
    },

    logout: async () => {
        await logout()
        set({user: null});
        window.location.replace("/");
    },
}));
