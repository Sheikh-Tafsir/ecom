import {create} from "zustand";
import {
    getAccessUser,
    saveAccessToken
} from "@/utils/AuthUtils";
import { logout} from "@/services/http/Axios.js";

export const useUserStore = create((set, get) => ({
    user: getAccessUser(),

    init: () => set({user: getAccessUser()}),

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
        window.location.replace("/");
    },
}));
