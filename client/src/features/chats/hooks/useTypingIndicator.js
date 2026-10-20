import {useState, useEffect, useCallback, useRef} from 'react';
import {MESSAGE_RECEIVE_EVENT, TYPING_EVENT, TYPING_BROADCAST_EVENT} from '@/services/realtime/socketEvents';

export const useTypingIndicator = (socket) => {
    const [typingUsers, setTypingUsers] = useState({});
    const timeoutsRef = useRef({});
    const lastEmitRef = useRef({});

    // Listen for typing broadcasts
    useEffect(() => {
        if (!socket) return;

        const handleTyping = (data) => {
            if (!data?.chatId || !data?.userId) return;

            const key = `${data.chatId}:${data.userId}`;

            // Clear existing timeout for this user in this chat
            if (timeoutsRef.current[key]) {
                clearTimeout(timeoutsRef.current[key]);
            }

            // Add to typing users
            setTypingUsers(prev => ({
                ...prev,
                [data.chatId]: {
                    ...(prev[data.chatId] || {}),
                    [data.userId]: {userName: data.userName},
                },
            }));

            // Auto-clear after 3 seconds
            timeoutsRef.current[key] = setTimeout(() => {
                setTypingUsers(prev => {
                    const chatUsers = {...(prev[data.chatId] || {})};
                    delete chatUsers[data.userId];
                    if (Object.keys(chatUsers).length === 0) {
                        const next = {...prev};
                        delete next[data.chatId];
                        return next;
                    }
                    return {...prev, [data.chatId]: chatUsers};
                });
                delete timeoutsRef.current[key];
            }, 3000);
        };

        socket.on(TYPING_BROADCAST_EVENT, handleTyping);

        return () => {
            socket.off(TYPING_BROADCAST_EVENT, handleTyping);
            // Clean up all timeouts
            Object.values(timeoutsRef.current).forEach(clearTimeout);
            timeoutsRef.current = {};
        };
    }, [socket]);

    // Clear a specific user from typing (called when their message arrives)
    const clearTypingUser = useCallback((chatId, userId) => {
        const key = `${chatId}:${userId}`;
        if (timeoutsRef.current[key]) {
            clearTimeout(timeoutsRef.current[key]);
            delete timeoutsRef.current[key];
        }

        setTypingUsers(prev => {
            const chatUsers = {...(prev[chatId] || {})};
            delete chatUsers[userId];
            if (Object.keys(chatUsers).length === 0) {
                const next = {...prev};
                delete next[chatId];
                return next;
            }
            return {...prev, [chatId]: chatUsers};
        });
    }, []);

    // Auto-clear typing on message arrival
    useEffect(() => {
        if (!socket) return;

        const handleMessage = (response) => {
            const msg = response?.data;
            if (msg?.chatId && msg?.senderId) {
                clearTypingUser(msg.chatId, msg.senderId);
            }
        };

        socket.on(MESSAGE_RECEIVE_EVENT, handleMessage);
        return () => socket.off(MESSAGE_RECEIVE_EVENT, handleMessage);
    }, [socket, clearTypingUser]);

    // Throttled emit
    const emitTyping = useCallback((chatId) => {
        if (!socket || !chatId) return;

        const now = Date.now();
        const last = lastEmitRef.current[chatId] || 0;
        if (now - last < 2000) return; // 2s throttle

        lastEmitRef.current[chatId] = now;
        socket.emit(TYPING_EVENT, {chatId});
    }, [socket]);

    // Get typing users for a specific chat as an array
    const getTypingUsers = useCallback((chatId) => {
        const chatTyping = typingUsers[chatId];
        if (!chatTyping) return [];
        return Object.entries(chatTyping).map(([userId, info]) => ({
            userId,
            userName: info.userName,
        }));
    }, [typingUsers]);

    return {typingUsers, emitTyping, clearTypingUser, getTypingUsers};
};
