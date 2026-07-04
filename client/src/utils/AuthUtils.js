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

export const getUserPermissions = (user) => {
    const permissions = user?.permissions;
    if (!permissions) return [];

    if (Array.isArray(permissions)) {
        return permissions;
    }

    if (typeof permissions === "string") {
        return permissions.split(/[\s,]+/).map(p => p.trim()).filter(Boolean);
    }

    return [];
};

export const hasPermission = (user, allowedPermissions) => {
    const permissions = getUserPermissions(user);

    if (Array.isArray(allowedPermissions)) {
        return allowedPermissions.some(p => permissions.includes(p));
    }

    return permissions.includes(allowedPermissions);
};
