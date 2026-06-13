import {useEffect, useCallback} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import socket from "@/services/realtime/socket.js";
import {Axios} from "@/services/http/Axios.js";
import {isNull} from "@/utils/index.js";
import {
    MESSAGE_RECEIVE_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatSync = (id, userId) => {
    const queryClient = useQueryClient();

    const updateChatOnMessage = useCallback((newMessage) => {
        if (isNull(newMessage.content)) return;

        queryClient.setQueryData(['chats'], (oldChats = []) => {
            const currentChat = oldChats.find(chat => chat.id === newMessage.chatId);
            if (!currentChat) {
                queryClient.invalidateQueries(['chats']);
                return oldChats;
            }

            const isOwnSentMessage = newMessage.senderId === userId;

            const updatedChat = {
                ...currentChat,
                lastSent: newMessage.createdAt,
                unreadMessage: !isOwnSentMessage ? (currentChat.unreadMessage || 0) + 1 : currentChat.unreadMessage,
            };

            const filteredChats = oldChats.filter(chat => chat.id !== newMessage.chatId);
            return [updatedChat, ...filteredChats];
        });

        if (newMessage.chatId === Number(id)) {
            queryClient.setQueryData(['selected_chat', id], (oldChat = {}) => {
                const messages = oldChat.messages || [];

                let updatedMessages;
                const isOwnSentMessage = newMessage.senderId === userId && !newMessage?.isTemporary && newMessage.tempId;

                if (isOwnSentMessage) {
                    updatedMessages = messages.map(msg =>
                        msg.tempId === newMessage.tempId ? newMessage : msg
                    );
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

        const handleReceiveMessage = (response) => updateChatOnMessage(response.data);
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
    }, [updateChatOnMessage, queryClient]);

    useEffect(() => {
        if (!id || isNull(userId)) return;

        const markMessagesAsSeen = async () => {
            try {
                queryClient.setQueryData(['chats'], (oldChats = []) => {
                    const currentChat = oldChats.find(chat => chat.id === Number(id));
                    if (!currentChat) {
                        queryClient.invalidateQueries(['chats']);
                        return oldChats;
                    }

                    return oldChats.map(chat =>
                        chat.id === Number(id) ? {...chat, unreadMessage: 0} : chat
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
