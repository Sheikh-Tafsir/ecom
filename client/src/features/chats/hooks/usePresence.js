import {useState, useEffect, useCallback, useRef} from 'react';
import {USER_PRESENCE_EVENT, PRESENCE_QUERY_EVENT} from '@/services/realtime/socketEvents';

export const usePresence = (socket, chats) => {
    const [presenceMap, setPresenceMap] = useState({});
    const queriedRef = useRef(false);

    // On mount/chats change: query presence for all DM contacts
    useEffect(() => {
        if (!socket || !chats || chats.length === 0) return;
        if (queriedRef.current) return;

        const contactIds = chats
            .filter(c => c.otherUserId)
            .map(c => c.otherUserId);

        // Also collect group participant IDs
        chats.forEach(c => {
            if (!c.otherUserId && c.participants) {
                c.participants.forEach(p => {
                    if (p.userId || p.id) contactIds.push(p.userId || p.id);
                });
            }
        });

        const uniqueIds = [...new Set(contactIds)];
        if (uniqueIds.length === 0) return;

        queriedRef.current = true;
        socket.emit(PRESENCE_QUERY_EVENT, {userIds: uniqueIds}, (response) => {
            if (response && typeof response === 'object') {
                setPresenceMap(prev => ({...prev, ...response}));
            }
        });
    }, [socket, chats]);

    // Listen for real-time presence updates
    useEffect(() => {
        if (!socket) return;

        const handlePresence = (data) => {
            if (!data?.userId) return;
            setPresenceMap(prev => ({
                ...prev,
                [data.userId]: {
                    online: data.online,
                    lastSeen: data.lastSeen || prev[data.userId]?.lastSeen || null,
                },
            }));
        };

        socket.on(USER_PRESENCE_EVENT, handlePresence);
        return () => socket.off(USER_PRESENCE_EVENT, handlePresence);
    }, [socket]);

    const mergePresence = useCallback((presenceData) => {
        if (!presenceData || typeof presenceData !== 'object') return;
        setPresenceMap(prev => ({...prev, ...presenceData}));
    }, []);

    const isOnline = useCallback((userId) => {
        return presenceMap[userId]?.online === true;
    }, [presenceMap]);

    const lastSeenAt = useCallback((userId) => {
        return presenceMap[userId]?.lastSeen || null;
    }, [presenceMap]);

    return {presenceMap, isOnline, lastSeenAt, mergePresence};
};
