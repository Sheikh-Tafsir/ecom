import {useMemo} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Axios} from "@/services/http/Axios.js";
import { isEmptyArray } from '@/utils';

export const useChatData = (id) => {
    const fetchChatList = async () => {
        const response = await Axios.get(`/chats`);
        return response.data.data || [];
    };

    const {data: chats = [], isLoading: isChatsLoading} = useQuery({
        queryKey: ['chats'],
        queryFn: fetchChatList,
    });

    const fetchSelectedChat = async () => {
        const response = await Axios.get(`/chats/${id}`);
        return response.data.data || {};
    };

    const {data: selectedChat = {}, isLoading: isSelectedChatLoading, isError: isSelectedChatError} = useQuery({
        queryKey: ['selected_chat', id],
        queryFn: fetchSelectedChat,
        enabled: !!id,
    });

    const chatMapByParticipants = useMemo(() => {
        const map = new Map();

        if (Array.isArray(chats)) {
            chats.forEach(chat => {
                if (!chat?.Participants || chat.Participants.length !== 2) return;

                const ids = chat.Participants.map(participant => participant.userId).sort((a, b) => a - b);
                const key = `${ids[0]}_${ids[1]}`;
                map.set(key, chat);
            });
        }

        return map;
    }, [chats]);

    return {
        chats,
        selectedChat,
        chatMapByParticipants,
        isChatsLoading,
        isSelectedChatLoading,
        isSelectedChatError
    };
};
