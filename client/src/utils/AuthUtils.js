import {jwtDecode} from 'jwt-decode';

let accessToken = null;

const SESSION_HINT = "ecom_has_session";

export const saveAccessToken = (token) => {
    accessToken = token;
    if (token) {
        localStorage.setItem(SESSION_HINT, "true");
    }
    return token ? jwtDecode(token) : null;
}

export const hasSessionHint = () => {
    return localStorage.getItem(SESSION_HINT) == "true";
}

export const getAccessToken = () => {
    return accessToken;
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

export const removeAccessToken = () => {
    accessToken = null;
    localStorage.removeItem(SESSION_HINT);
}

export const removeCart = () => {
    localStorage.removeItem(import.meta.env.VITE_LOCAL_STORAGE_CART_KEY);
}

export const getUserPermissions = (user) => {
    const permissions = user?.permissions;
    if (!permissions) return [];

    if (Array.isArray(permissions)) {
        return permissions;
    }

    if (typeof permissions == "string") {
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
