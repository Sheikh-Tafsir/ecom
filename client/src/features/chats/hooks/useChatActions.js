import {useCallback} from 'react';
import {useUserStore} from '@/store/useUserStore';
import {AuthenticatedAxios} from "@/services/http/Axios.js";
import {REGULAR_ACTION, TOAST_TYPE} from "@/constants/app.constants";
import {CONTENT_TYPE} from "@/constants/chat.constants";
import {toastify} from '@/common/toastify.js';
import {
    MESSAGE_SEND_EVENT,
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_UPDATE_REQUEST_EVENT
} from "@/services/realtime/socketEvents.js";

export const useChatActions = (id, userId, updateChatOnMessage, onNewChat) => {
    const socket = useUserStore(state => state.socket);

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
    }

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
        //console.info("Socket is connected in send message:", !!socket);
        if ((!id && !searchedUser) || !socket) return;

        let contentType = CONTENT_TYPE.TEXT;
        if (image) {
            content = await getImageLink(image);
            if (!content) {
                return;  // Upload failed — already toasted
            }

            contentType = CONTENT_TYPE.IMAGE;
        }

        const tempId = Date.now().toString() + Math.random().toString(36).slice(2);
        updateChatLocally(content, contentType, tempId);

        socket.emit(MESSAGE_SEND_EVENT, {
            ...(searchedUser?.id ? {receiverId: searchedUser.id} : {chatId: id}),
            content,
            contentType,
            tempId,
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                toastify(TOAST_TYPE.ERROR, acknowledgment.error);
                return;
            }

            // console.info('sent message:', acknowledgment);
            const sentMessage = acknowledgment.data;
            onNewChat(sentMessage.chatId);
        });
    }, [id, socket, updateChatLocally, onNewChat]);

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

    return {handleSendMessage, handleGroupManagementRequest, updateChatLocally};
};
