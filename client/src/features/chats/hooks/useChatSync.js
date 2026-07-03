import {useEffect, useCallback} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import {useUserStore} from '@/store/useUserStore';
import {Axios} from "@/services/http/Axios.js";
import {
    MESSAGE_RECEIVE_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatSync = (id, userId) => {
    const queryClient = useQueryClient();
    const socket = useUserStore(state => state.socket);

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
                lastSent: newMessage.createdAt,
                unreadMessage: !isOwnSentMessage ? (currentChat.unreadMessage || 0) + 1 : currentChat.unreadMessage,
            };

            const filteredChats = oldChats.filter(chat => chat?.id != newMessage.chatId);
            return [updatedChat, ...filteredChats];
        });

        if (newMessage.chatId == id) {
            queryClient.setQueryData(['selected_chat', id], (oldChat = {}) => {
                const messages = oldChat.messages || [];

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
                        updatedMessages = [...messages, newMessage];
                    }
                } else {
                    updatedMessages = [...messages, newMessage];
                }

                return {
                    ...oldChat,
                    messages: updatedMessages,
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

                await Axios.post(`/chats/${id}/view`, {
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
