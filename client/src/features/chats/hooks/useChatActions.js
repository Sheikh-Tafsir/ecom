import {useState, useCallback} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import {useUserStore} from '@/store/useUserStore';
import {AuthenticatedAxios} from "@/services/http/Axios.js";
import {REGULAR_ACTION, TOAST_TYPE} from "@/constants/app.constants";
import {CONTENT_TYPE} from "@/constants/chat.constants";
import {toastify} from '@/common/toastify.js';
import {queryKeys} from "@/services/reactQuery/queryKeys";
import {
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_UPDATE_REQUEST_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatActions = (id, userId, updateChatOnMessage, onNewChat) => {
    const socket = useUserStore(state => state.socket);
    const queryClient = useQueryClient();
    const [isSending, setIsSending] = useState(false);

    const getImageLink = async (image) => {
        try {
            const formData = new FormData();
            formData.append('image', image);

            const response = await AuthenticatedAxios.post(`/uploads`, formData, {
                headers: {'Content-Type': 'multipart/form-data'},
                timeout: 15000,
            });

            const url = response.data?.data;

            if (!url) {
                toastify(TOAST_TYPE.ERROR, 'Image upload failed: no URL returned');
                return null;
            }
            return url;
        } catch (err) {
            toastify(TOAST_TYPE.ERROR, 'Image upload failed. Please try again.');
            return null;
        }
    };

    const updateChatLocally = useCallback((content, contentType, tempId) => {
        const createdAt = new Date().toISOString();

        const tempMessage = {
            id: tempId,
            content,
            contentType,
            chatId: id,
            senderId: userId,
            createdAt,
            updatedAt: createdAt,
            isTemporary: true,
            tempId,
        };

        updateChatOnMessage(tempMessage);
    }, [id, userId, updateChatOnMessage]);

    const handleSendMessage = useCallback(async (content, image, searchedUser) => {
        if (!id && !searchedUser) return;
        if (isSending) return;

        let contentType = CONTENT_TYPE.TEXT;
        let finalContent = content;

        if (image) {
            finalContent = await getImageLink(image);
            if (!finalContent) {
                return;  // Upload failed — already toasted
            }

            contentType = CONTENT_TYPE.IMAGE;
        }

        const trimmed = (finalContent || '').trim();
        if (!trimmed) return;

        const tempId = Date.now().toString() + Math.random().toString(36).slice(2);
        updateChatLocally(trimmed, contentType, tempId);

        setIsSending(true);
        try {
            const endpoint = id ? `/chats/${id}/messages` : `/chats/messages`;
            const payload = {
                content: trimmed,
                contentType,
                tempId,
                ...(searchedUser?.id ? {receiverId: searchedUser.id} : {chatId: id}),
            };

            const response = await AuthenticatedAxios.post(endpoint, payload);
            const sentMessage = response.data?.data;

            if (sentMessage) {
                // Reconcile optimistic message with persisted message
                updateChatOnMessage({
                    ...sentMessage,
                    tempId,
                    isTemporary: false,
                });

                // Update sidebar chat list preview and ordering
                queryClient.setQueryData(queryKeys.chats.all, (oldChats = []) => {
                    if (!Array.isArray(oldChats)) return oldChats;
                    const targetChatId = sentMessage.chatId;
                    const currentChat = oldChats.find(c => c?.id == targetChatId);
                    if (!currentChat) {
                        queryClient.invalidateQueries(queryKeys.chats.all);
                        return oldChats;
                    }
                    const updatedChat = {
                        ...currentChat,
                        lastMessage: sentMessage.content,
                        lastSent: sentMessage.createdAt,
                        lastSentId: sentMessage.id,
                        lastSenderId: userId,
                        unreadMessage: 0,
                    };
                    return [updatedChat, ...oldChats.filter(c => c?.id != targetChatId)];
                });

                // If starting a new DM with searchedUser, navigate to the newly created chat
                if (!id && searchedUser && sentMessage.chatId) {
                    onNewChat(sentMessage.chatId);
                }
            }
        } catch (err) {
            // Mark the optimistic message as failed
            if (id) {
                queryClient.setQueryData(queryKeys.chats.selected(id), (oldData) => {
                    if (!oldData?.pages?.length) return oldData;
                    const pages = [...oldData.pages];
                    const firstPage = {...pages[0]};
                    const messages = (firstPage.messages || []).map(msg => {
                        if (msg.tempId === tempId) {
                            return {...msg, isTemporary: false, failed: true};
                        }
                        return msg;
                    });
                    pages[0] = {...firstPage, messages};
                    return {...oldData, pages};
                });
            }

            const errorMsg = err.response?.data?.error?.global ||
                             err.response?.data?.message ||
                             err.message ||
                             'Failed to send message';
            toastify(TOAST_TYPE.ERROR, errorMsg);
        } finally {
            setIsSending(false);
        }
    }, [id, isSending, userId, updateChatLocally, updateChatOnMessage, onNewChat, queryClient]);

    const handleGroupManagementRequest = useCallback(async (users, action, onSuccess) => {
        if (!socket) return;

        socket.emit(action == REGULAR_ACTION.CREATE ? GROUP_CREATE_REQUEST_EVENT : GROUP_UPDATE_REQUEST_EVENT, {
            users,
            chatId: id
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                toastify(TOAST_TYPE.ERROR, acknowledgment.error);
                return;
            }

            onSuccess(acknowledgment);
        });
    }, [id, socket]);

    return {handleSendMessage, handleGroupManagementRequest, updateChatLocally, isSending};
};
