import {useEffect, useCallback, useRef} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import {useUserStore} from '@/store/useUserStore';
import {AuthenticatedAxios} from "@/services/http/Axios.js";
import {
    MESSAGE_RECEIVE_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatSync = (id, userId) => {
    const queryClient = useQueryClient();
    const socket = useUserStore(state => state.socket);

    // Reconnect sync: track first connection and prevent concurrent syncs
    const hasConnectedRef = useRef(false);
    const syncInProgressRef = useRef(false);

    const updateChatOnMessage = useCallback((newMessage) => {
        // console.info("Received new message:", newMessage);
        if (!newMessage.content) return;

        queryClient.setQueryData(['chats'], (oldChats = []) => {
            if (!Array.isArray(oldChats)) {
                oldChats = [];
            }

            const currentChat = oldChats.find(chat => chat?.id == newMessage.chatId);
            if (!currentChat) {
                queryClient.invalidateQueries(['chats']);
                return oldChats;
            }

            const isOwnSentMessage = newMessage.senderId == userId;

            const updatedChat = {
                ...currentChat,
                lastMessage: newMessage.content,
                lastSent: newMessage.createdAt,
                lastSentId: newMessage.id,
                lastSenderId: newMessage.senderId,
                unreadMessage: isOwnSentMessage ? 0 : (currentChat.unreadMessage || 0) + 1,
            };

            const filteredChats = oldChats.filter(chat => chat?.id != newMessage.chatId);
            return [updatedChat, ...filteredChats];
        });

        if (newMessage.chatId == id) {
            queryClient.setQueryData(['selected_chat', id], (oldData) => {
                if (!oldData || !oldData.pages || oldData.pages.length === 0) return oldData;

                const pages = [...oldData.pages];
                const lastPageIndex = pages.length - 1;
                
                const firstPage = { ...pages[0] };
                const messages = firstPage.messages || [];

                let updatedMessages;
                const isOwnSentMessage = newMessage.senderId == userId && !newMessage?.isTemporary && newMessage.tempId;

                if (isOwnSentMessage) {
                    let found = false;
                    updatedMessages = messages.map(msg => {
                        if (msg.tempId == newMessage.tempId) {
                            found = true;
                            return newMessage;
                        }
                        return msg;
                    });
                    
                    if (!found) {
                        if (messages.some(m => m.id != null && m.id == newMessage.id)) return oldData;
                        updatedMessages = [...messages, newMessage];
                    }
                } else {
                    // Dedup by ID to handle sync + WS race conditions
                    if (messages.some(m => m.id != null && m.id == newMessage.id)) return oldData;
                    updatedMessages = [...messages, newMessage];
                }

                pages[0] = { ...firstPage, messages: updatedMessages };

                return {
                    ...oldData,
                    pages: pages,
                };
            });
        }
    }, [id, userId, queryClient]);

    useEffect(() => {
        if (!socket) return;

        //console.info("Socket is available in useChatSync");

        const handleReceiveMessage = (response) => {
            //console.info("Received new message:", response.data);
            updateChatOnMessage(response.data);
        };
        const handleGroupCreationResponse = () => queryClient.invalidateQueries(['chats']);
        const handleUpdateResponse = () => queryClient.invalidateQueries(['chats']);

        socket.on(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
        socket.on(GROUP_CREATE_RESPONSE_EVENT, handleGroupCreationResponse);
        socket.on(GROUP_UPDATE_RESPONSE_EVENT, handleUpdateResponse);

        return () => {
            socket.off(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
            socket.off(GROUP_CREATE_RESPONSE_EVENT, handleGroupCreationResponse);
            socket.off(GROUP_UPDATE_RESPONSE_EVENT, handleUpdateResponse);
        };
    }, [socket, updateChatOnMessage, queryClient]);

    // Reconnect synchronization: fetch missed data after WebSocket reconnects
    useEffect(() => {
        if (!socket) return;

        const handleConnect = async () => {
            if (!hasConnectedRef.current) {
                // First connection — not a reconnection
                hasConnectedRef.current = true;
                return;
            }

            // Prevent concurrent sync requests
            if (syncInProgressRef.current) return;
            syncInProgressRef.current = true;

            console.info('[ChatSync] Socket reconnected — syncing state');

            try {
                // 1. Refresh chat list (unread counts, last messages, ordering)
                queryClient.invalidateQueries(['chats']);

                // 2. If a chat is currently open, fetch missed messages
                if (id) {
                    const cachedData = queryClient.getQueryData(['selected_chat', id]);
                    if (!cachedData?.pages?.length) {
                        queryClient.invalidateQueries(['selected_chat', id]);
                        return;
                    }

                    // Find the latest message ID in cache (page 0, last element)
                    const firstPage = cachedData.pages[0];
                    const messages = firstPage.messages || [];
                    const lastMessage = messages[messages.length - 1];

                    if (!lastMessage?.id || lastMessage.isTemporary) {
                        queryClient.invalidateQueries(['selected_chat', id]);
                        return;
                    }

                    const response = await AuthenticatedAxios.get(`/chats/${id}`, {
                        params: {afterId: lastMessage.id}
                    });

                    const missedData = response.data.data;
                    const missedMessages = missedData?.messages || [];

                    if (missedMessages.length === 0) return;

                    if (missedData.pagination?.hasMore) {
                        // Too many missed messages — full refetch is cleaner
                        queryClient.invalidateQueries(['selected_chat', id]);
                        return;
                    }

                    // Append missed messages with ID-based deduplication
                    queryClient.setQueryData(['selected_chat', id], (oldData) => {
                        if (!oldData?.pages?.length) return oldData;

                        const pages = [...oldData.pages];
                        const page0 = {...pages[0]};
                        const existing = page0.messages || [];

                        const existingIds = new Set(existing.map(m => String(m.id)));
                        const deduped = missedMessages.filter(m => !existingIds.has(String(m.id)));

                        if (deduped.length === 0) return oldData;

                        page0.messages = [...existing, ...deduped];
                        pages[0] = page0;

                        return {...oldData, pages};
                    });
                }
            } catch (err) {
                console.error('[ChatSync] Reconnect sync failed:', err);
                // Fallback: invalidate to trigger fresh fetches
                if (id) queryClient.invalidateQueries(['selected_chat', id]);
            } finally {
                syncInProgressRef.current = false;
            }
        };

        socket.on('connect', handleConnect);

        return () => {
            socket.off('connect', handleConnect);
        };
    }, [socket, id, queryClient]);

    useEffect(() => {
        if (!id || !userId) return;

        const markMessagesAsSeen = async () => {
            try {
                queryClient.setQueryData(['chats'], (oldChats = []) => {
                    const currentChat = oldChats.find(chat => chat.id == Number(id));
                    if (!currentChat) {
                        queryClient.invalidateQueries(['chats']);
                        return oldChats;
                    }

                    return oldChats.map(chat =>
                        chat.id == Number(id) ? {...chat, unreadMessage: 0} : chat
                    );
                });

                await AuthenticatedAxios.post(`/chats/${id}/view`, {
                    lastSeen: new Date().toISOString()
                });
            } catch (error) {
                console.error("Failed to mark messages as seen:", error);
            }
        };

        markMessagesAsSeen();
    }, [id, userId, queryClient]);

    return {updateChatOnMessage};
};
