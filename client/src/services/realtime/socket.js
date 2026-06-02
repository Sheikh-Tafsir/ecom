import { io } from 'socket.io-client';

const API_PATH = import.meta.env.VITE_API_PATH;
const socket = io(API_PATH, { 
  autoConnect: false,
});

socket.on('connect', () => {
  //console.log("Connected to socket server");
});

socket.on("connect_error", (err) => {
  //console.error("Socket connection error:", err.message);
});

export const connectSocket = (token) => {
  if (socket.connected) return;
  socket.auth = { token };
  socket.connect();
};

export const disconnectSocket = () => {
  if (socket.connected) socket.disconnect();
};

export default socket;
