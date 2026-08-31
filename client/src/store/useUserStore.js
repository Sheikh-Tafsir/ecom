import {create} from "zustand";
import {
    hasSessionHint,
    saveAccessToken,
    removeAccessToken
} from "@/utils/AuthUtils";
import {logout, refreshAccessToken} from "@/services/http/Axios.js";

export const useUserStore = create((set, get) => ({
    user: null,
    isLoading: true,

    init: async () => {
        if (!hasSessionHint()) {
            set({user: null, isLoading: false});
            return;
        }

        try {
            const token = await refreshAccessToken();
            set({user: saveAccessToken(token), isLoading: false});
        } catch (err) {
            console.error("Initial token refresh failed:", err);
            if (err.response?.status === 401 || err.response?.status === 403) {
                removeAccessToken();
            }
            set({user: null, isLoading: false});
        }
    },

    socket: null,

    setSocket: (socket) => set({socket}),

    login: (token) => {
        set({user: saveAccessToken(token)});
    },

    logout: async () => {
        const {socket, setSocket} = get();
        if (socket) {
            socket.disconnect();
            setSocket(null);
        }

        await logout()
        set({user: null});
    },
}));
