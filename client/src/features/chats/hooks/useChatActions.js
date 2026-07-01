import {useCallback} from 'react';
import {useUserStore} from '@/store/useUserStore';
import {Axios} from "@/services/http/Axios.js";
import {CONTENT_TYPE, REGULAR_ACTION} from '@/utils/enums';
import {
    MESSAGE_SEND_EVENT,
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_UPDATE_REQUEST_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatActions = (id, userId, updateChatOnMessage, onNewChat, showToast) => {
    const socket = useUserStore(state => state.socket);

    const getImageLink = async (image) => {
        try {
            const formData = new FormData();
            formData.append('image', image);

            const response = await Axios.post(`/common/upload-image`, formData, {
                headers: {'Content-Type': 'multipart/form-data'},
                timeout: 15000,
            });

            return response.data?.data;
        } catch (err) {
            console.error("Image upload failed:", err);
        }
    }

    const updateChatLocally = useCallback((content, contentType, tempId) => {
        const createdAt = new Date().toISOString();

        const tempMessage = {
            id: tempId,
            content,
            contentType,
            chatId: Number(id),
            senderId: userId,
            createdAt,
            updatedAt: createdAt,
            isTemporary: true,
            tempId,
        };

        updateChatOnMessage(tempMessage);
    }, [id, userId, updateChatOnMessage]);

    const handleSendMessage = useCallback(async (content, image, searchedUser) => {
        console.info("Socket is connected in send message:", !!socket);
        if ((!id && !searchedUser) || !socket) return;

        let contentType = CONTENT_TYPE.TEXT;
        if (image) {
            content = await getImageLink(image);
            contentType = CONTENT_TYPE.IMAGE;
        }

        const tempId = Date.now().toString() + Math.random().toString(36).slice(2);
        updateChatLocally(content, contentType, tempId);

        socket.emit(MESSAGE_SEND_EVENT, {
            ...(searchedUser?.id ? {receiverId: searchedUser.id} : {chatId: Number(id)}),
            content,
            contentType,
            tempId,
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                console.error(acknowledgment.error);
                showToast(acknowledgment.error, 'error');
                return;
            }

            // console.info('sent message:', acknowledgment);
            const sentMessage = acknowledgment.data;
            onNewChat(sentMessage.chatId);
        });
    }, [id, socket, updateChatLocally, onNewChat, showToast]);

    const handleGroupManagementRequest = useCallback(async (users, action, onSuccess) => {
        if (!socket) return;

        socket.emit(action == REGULAR_ACTION.CREATE ? GROUP_CREATE_REQUEST_EVENT : GROUP_UPDATE_REQUEST_EVENT, {
            users,
            chatId: id
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                console.error(acknowledgment.error);
                showToast(acknowledgment.error, 'error');
                return;
            }

            onSuccess(acknowledgment);
        });
    }, [id, socket, showToast]);

    return {handleSendMessage, handleGroupManagementRequest, updateChatLocally};
};
