import {io} from 'socket.io-client';
import { logout, refreshAccessToken } from '../http/Axios';
import { getAccessToken } from '@/utils';
import { notify } from '@/components/common/notification';
import { TOAST_TYPE } from '@/utils/enums';

const API_PATH = import.meta.env.VITE_API_PATH;
const WEB_SOCKET_ON = import.meta.env.VITE_WEB_SOCKET_ON;

let socket = null;
let connectionPromise = null;

export const connectSocket = async () => {
    if (!isSocketOn) return

    if (socket?.connected) return socket;
    if (connectionPromise) return connectionPromise;

    connectionPromise = (async () => {
        try {
            const token = getAccessToken();

            if (!token) {
                connectionPromise = null;
                return null;
            }

            socket = io(API_PATH, {
                auth: { token },
                transports: ['websocket'],
            });

            let hasRetried = false;

            socket.on('connect', () => {
                //console.log("Connected to socket server");
                hasRetried = false;
            });

            socket.once("connect_error", async (err) => {
                console.error("Socket connection error:", err);

                if (err.message == "Unauthorized" && !hasRetried) {
                    hasRetried = true;

                    try {
                        await refreshAccessToken();

                        socket.auth = {
                            token: getAccessToken(),
                        };

                        socket.connect();
                    } catch {
                        notify(TOAST_TYPE.INFO, "Session expired. Please log in again.");
                        await logout();
                    }
                }
            });

            socket.on("disconnect", () => {
                connectionPromise = null;
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

export const isSocketOn = () => {
    return WEB_SOCKET_ON == "true";
}
