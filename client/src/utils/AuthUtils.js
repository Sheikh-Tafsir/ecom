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

export const getAccessUser = (token) => {
    token ??= getAccessToken();

    if (!token) {
        return null;
    }

    try {
        return jwtDecode(token);
    } catch {
        return null;
    }
};

export const isAccessTokenExpired = (token) => {
    const user = getAccessUser(token);
    return !user || Date.now() >= user.exp * 1000;
}

export const removeAccessToken = () => {
    localStorage.removeItem(ACCESS_TOKEN);
}

export const hasPermission = (user, permission) => {
    return user?.permissions?.includes(permission);
}
