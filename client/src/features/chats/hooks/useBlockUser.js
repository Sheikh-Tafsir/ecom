import {useState, useEffect, useCallback} from 'react';
import {AuthenticatedAxios} from '@/services/http/Axios.js';
import {CHAT_TYPE} from '@/constants/chat.constants';
import {USER_BLOCKED_EVENT} from '@/services/realtime/socketEvents';
import {useUserStore} from '@/store/useUserStore.js';

export const useBlockUser = (chat, userId) => {
    const [blockStatus, setBlockStatus] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const socket = useUserStore(state => state.socket);

    // Derive otherUserId for DM chats
    const otherUserId = chat?.type === CHAT_TYPE.DIRECT && chat?.participants
        ? chat.participants.find(p => String(p.id) !== String(userId))?.id || null
        : null;

    // Fetch block status when chat changes (DM only)
    useEffect(() => {
        if (!otherUserId) {
            setBlockStatus(null);
            return;
        }

        let cancelled = false;

        const fetchStatus = async () => {
            try {
                const res = await AuthenticatedAxios.get(`/chats/users/${otherUserId}/block-status`);
                if (!cancelled) {
                    setBlockStatus(res.data?.data || null);
                }
            } catch {
                if (!cancelled) setBlockStatus(null);
            }
        };

        fetchStatus();
        return () => { cancelled = true; };
    }, [otherUserId]);

    // Listen for real-time block/unblock events from the other user
    useEffect(() => {
        if (!socket || !otherUserId) return;

        const handleBlockEvent = (data) => {
            if (!data?.userId) return;

            if (data.userId === otherUserId) {
                // Event from the other user (they blocked/unblocked us)
                if (data.blocked) {
                    setBlockStatus(prev => ({...prev, blockedByOther: true, blocked: true}));
                } else {
                    setBlockStatus(prev => ({
                        ...prev,
                        blockedByOther: false,
                        blocked: prev?.blockedByMe || false,
                    }));
                }
            } else if (data.userId === userId) {
                // Event from ourselves (multi-tab sync — we blocked/unblocked on another tab)
                if (data.blocked) {
                    setBlockStatus(prev => ({...prev, blockedByMe: true, blocked: true}));
                } else {
                    setBlockStatus(prev => ({
                        ...prev,
                        blockedByMe: false,
                        blocked: prev?.blockedByOther || false,
                    }));
                }
            }
        };

        socket.on(USER_BLOCKED_EVENT, handleBlockEvent);
        return () => socket.off(USER_BLOCKED_EVENT, handleBlockEvent);
    }, [socket, otherUserId]);

    const toggleBlock = useCallback(async () => {
        if (!otherUserId || isLoading) return;

        setIsLoading(true);
        try {
            if (blockStatus?.blockedByMe) {
                await AuthenticatedAxios.delete(`/chats/users/${otherUserId}/block`);
                setBlockStatus(prev => ({...prev, blockedByMe: false, blocked: prev?.blockedByOther || false}));
            } else {
                await AuthenticatedAxios.post(`/chats/users/${otherUserId}/block`);
                setBlockStatus(prev => ({...prev, blockedByMe: true, blocked: true}));
            }
        } catch (err) {
            console.error('Block/unblock failed:', err);
        } finally {
            setIsLoading(false);
        }
    }, [otherUserId, blockStatus, isLoading]);

    return {blockStatus, toggleBlock, isLoading};
};
