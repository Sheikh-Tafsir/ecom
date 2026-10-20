import {useEffect, useCallback, useRef} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import {useUserStore} from '@/store/useUserStore';
import {AuthenticatedAxios} from "@/services/http/Axios.js";
import {
    MESSAGE_RECEIVE_EVENT,
    JOIN_CHAT_EVENT,
    LEAVE_CHAT_EVENT,
    CHAT_UPDATED_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatSync = (id, userId) => {
    const queryClient = useQueryClient();
    const socket = useUserStore(state => state.socket);

    // Reconnect sync: track first connection and prevent concurrent syncs
    const hasConnectedRef = useRef(false);
    const syncInProgressRef = useRef(false);

    // Track current joined chat room for cleanup on switch
    const joinedChatRef = useRef(null);

    // updateChatOnMessage handles ONLY the active message thread (selected_chat query).
    // Sidebar (chat list) updates are handled exclusively by handleChatUpdated via chat-updated event.
    // This prevents double unread increments when both events arrive for an active viewer.
    const updateChatOnMessage = useCallback((newMessage) => {
        if (!newMessage.content) return;
        if (newMessage.chatId != id) return; // Only process messages for the active chat

        queryClient.setQueryData(['selected_chat', id], (oldData) => {
            if (!oldData || !oldData.pages || oldData.pages.length === 0) return oldData;

            const pages = [...oldData.pages];
            
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
    }, [id, userId, queryClient]);

    // Join/leave active chat rooms on demand
    useEffect(() => {
        if (!socket) return;

        if (id) {
            socket.emit(JOIN_CHAT_EVENT, {chatId: id}, (res) => {
                if (res?.success) {
                    joinedChatRef.current = id;
                }
            });
        }

        return () => {
            // Leave the room when switching chats or unmounting
            if (joinedChatRef.current) {
                socket.emit(LEAVE_CHAT_EVENT, {chatId: joinedChatRef.current});
                joinedChatRef.current = null;
            }
        };
    }, [socket, id]);

    // Listen for full messages (active chat room) and sidebar updates (personal room)
    useEffect(() => {
        if (!socket) return;

        // Full message — only arrives when the user is in the chat room (active viewer)
        const handleReceiveMessage = (response) => {
            updateChatOnMessage(response.data);
        };

        // Lightweight sidebar update — arrives via personal user room for ALL participants.
        // Updates the chat list preview without the full message payload.
        const handleChatUpdated = (response) => {
            const update = response.data;
            if (!update?.chatId) return;

            const isViewingThisChat = update.chatId == id;

            queryClient.setQueryData(['chats'], (oldChats = []) => {
                if (!Array.isArray(oldChats)) {
                    oldChats = [];
                }

                const currentChat = oldChats.find(chat => chat?.id == update.chatId);
                if (!currentChat) {
                    // New chat or not in cache — refetch
                    queryClient.invalidateQueries(['chats']);
                    return oldChats;
                }

                const isOwnSentMessage = update.lastSenderId == userId;

                const updatedChat = {
                    ...currentChat,
                    lastMessage: update.lastMessage,
                    lastSent: update.lastSent,
                    lastSentId: update.lastSentId,
                    lastSenderId: update.lastSenderId,
                    // Do not increment if own message OR if currently viewing this chat
                    unreadMessage: (isOwnSentMessage || isViewingThisChat)
                        ? 0
                        : (currentChat.unreadMessage || 0) + 1,
                };

                const filteredChats = oldChats.filter(chat => chat?.id != update.chatId);
                return [updatedChat, ...filteredChats];
            });

            // If the sender sent a message via another tab/device and the chat is active,
            // reconcile using tempId to replace the optimistic message
            if (update.chatId == id && update.lastSenderId == userId && update.tempId) {
                queryClient.setQueryData(['selected_chat', id], (oldData) => {
                    if (!oldData?.pages?.length) return oldData;

                    const pages = [...oldData.pages];
                    const firstPage = {...pages[0]};
                    const messages = firstPage.messages || [];

                    const updatedMessages = messages.map(msg => {
                        if (msg.tempId && msg.tempId == update.tempId) {
                            return {...msg, id: update.lastSentId, isTemporary: false};
                        }
                        return msg;
                    });

                    pages[0] = {...firstPage, messages: updatedMessages};
                    return {...oldData, pages};
                });
            }
        };

        const handleGroupCreationResponse = () => queryClient.invalidateQueries(['chats']);
        const handleUpdateResponse = () => queryClient.invalidateQueries(['chats']);

        socket.on(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
        socket.on(CHAT_UPDATED_EVENT, handleChatUpdated);
        socket.on(GROUP_CREATE_RESPONSE_EVENT, handleGroupCreationResponse);
        socket.on(GROUP_UPDATE_RESPONSE_EVENT, handleUpdateResponse);

        return () => {
            socket.off(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
            socket.off(CHAT_UPDATED_EVENT, handleChatUpdated);
            socket.off(GROUP_CREATE_RESPONSE_EVENT, handleGroupCreationResponse);
            socket.off(GROUP_UPDATE_RESPONSE_EVENT, handleUpdateResponse);
        };
    }, [socket, id, userId, updateChatOnMessage, queryClient]);

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

                // 2. Rejoin the active chat room after reconnect
                if (id) {
                    socket.emit(JOIN_CHAT_EVENT, {chatId: id}, (res) => {
                        if (res?.success) {
                            joinedChatRef.current = id;
                        }
                    });
                }

                // 3. If a chat is currently open, fetch missed messages
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
                    const currentChat = oldChats.find(chat => String(chat.id) === String(id));
                    if (!currentChat) {
                        queryClient.invalidateQueries(['chats']);
                        return oldChats;
                    }

                    return oldChats.map(chat =>
                        String(chat.id) === String(id) ? {...chat, unreadMessage: 0} : chat
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
