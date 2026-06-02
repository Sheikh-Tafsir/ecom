import {AxiosNoInterceptor} from '@/services/http/Axios.js';

export const logout = async () => {
    try {
        await AxiosNoInterceptor.post("/auth/logout");
    } catch (error) {
        console.error("Logout failed:", error);
    } finally {
        localStorage.removeItem("visoredAccessToken");
        window.location.replace("/");
    }
};