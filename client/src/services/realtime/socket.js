import {io} from 'socket.io-client';
import { logout, refreshAccessToken } from '../http/Axios';
import { getAccessToken, isAccessTokenExpired } from '@/utils';

const API_PATH = import.meta.env.VITE_API_PATH;
const WEB_SOCKET_ON = import.meta.env.VITE_WEB_SOCKET_ON;

let socket = null;
let connectionPromise = null;

export const connectSocket = async () => {
    if (socket?.connected) return socket;
    if (connectionPromise) return connectionPromise;

    connectionPromise = (async () => {
        try {
            const token = await getValidAccessToken();

            if (!token) {
                connectionPromise = null;
                return null;
            }

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
        } catch (error) {
            connectionPromise = null;
            throw error;
        }
    })();

    return connectionPromise;
};

export const disconnectSocket = () => {
    if (socket) {
        socket.disconnect();
        socket = null;
    }
    connectionPromise = null;
};

const getValidAccessToken = async () => {
    let token = getAccessToken();

    if (!token) {
        return null;
    }

    if (!isAccessTokenExpired(token)) {
        return token;
    }

    try {
        return await refreshAccessToken();
    } catch (error) {
        await logout();
        throw error;
    }
};

export const isSocketOn = () => {
    return WEB_SOCKET_ON == true;
}
