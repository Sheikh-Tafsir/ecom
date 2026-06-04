import {jwtDecode} from 'jwt-decode';

export const ACCESS_TOKEN = import.meta.env.VITE_API_PATH;

export const saveAccessToken = (token) => {
    localStorage.setItem(ACCESS_TOKEN, token);
    return jwtDecode(token);
}

export const getAccessToken = () => {
    try {
        return localStorage.getItem(ACCESS_TOKEN);
    } catch (err) {
        console.error('Invalid user data in localStorage:', err);
        return null;
    }
}

export const getAccessUser = () => {
    const token = getAccessToken();

    if (!token) {
        return null;
    }

    try {
        return jwtDecode(token);
    } catch {
        return null;
    }
};

export const isAccessTokenExpired = () => {
    const storedUser = getAccessUser();
    return !storedUser || Date.now() >= storedUser.exp * 1000;
}
