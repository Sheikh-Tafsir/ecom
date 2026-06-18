import {io} from 'socket.io-client';
import { logout, refreshAccessToken } from '../http/Axios';
import { getAccessToken, isAccessTokenExpired } from '@/utils';

const API_PATH = import.meta.env.VITE_API_PATH;

let socket = null;


export const connectSocket = async () => {
    if (socket?.connected) return socket;

    const token = await getValidAccessToken();

    if (!token) return null;

    socket = io(API_PATH, {
        auth: { token },
        transports: ['websocket'],
    });

    socket.on('connect', () => {
        //console.log("Connected to socket server");
    });

    socket.on("connect_error", (err) => {
        console.error("Socket connection error:", err);
    });

    return socket;
};

export const disconnectSocket = () => {
    if (socket?.connected) {
        socket.disconnect();
    }
};

const getValidAccessToken = async () => {
    let token = getAccessToken();

    if (!token) {
        return null;
    }

    if (!isAccessTokenExpired()) {
        return token;
    }

    try {
        return await refreshAccessToken();
    } catch (error) {
        await logout();
        throw error;
    }
};

export const getSocket = () => socket;
