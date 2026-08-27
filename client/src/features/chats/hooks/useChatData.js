import {useMemo} from 'react';
import {useQuery, useInfiniteQuery} from '@tanstack/react-query';
import {AuthenticatedAxios} from "@/services/http/Axios.js";

export const useChatData = (id) => {
    const fetchChatList = async () => {
        const response = await AuthenticatedAxios.get(`/chats`);
        return response.data.data?.chats || [];
    };

    const {data: chats = [], isLoading: isChatsLoading} = useQuery({
        queryKey: ['chats'],
        queryFn: fetchChatList,
    });

    const fetchSelectedChat = async ({ pageParam }) => {
        const params = {};
        if (pageParam) {
            params.cursorCreatedAt = pageParam.createdAt;
            params.cursorId = pageParam.id;
        }
        const response = await AuthenticatedAxios.get(`/chats/${id}`, { params });
        return response.data.data || {};
    };

    const {
        data: selectedChatData,
        isLoading: isSelectedChatLoading,
        isError: isSelectedChatError,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage
    } = useInfiniteQuery({
        queryKey: ['selected_chat', id],
        queryFn: fetchSelectedChat,
        enabled: !!id,
        getNextPageParam: (lastPage) => {
            if (lastPage.pagination?.hasMore && lastPage.messages?.length > 0) {
                // The messages are returned in chronological order for the page [oldest...latest]
                // So the first message in the array is the oldest one, which is our cursor for "older" messages.
                const oldestMessage = lastPage.messages[0];
                return {
                    createdAt: oldestMessage.createdAt,
                    id: oldestMessage.id
                };
            }
            return undefined;
        },
        initialPageParam: null,
    });

    const selectedChat = useMemo(() => {
        if (!selectedChatData) return {};
        
        const allPages = selectedChatData.pages;
        if (!allPages || allPages.length === 0) return {};

        // Merge messages from all pages. Pages are [LatestPage, OlderPage, ...]
        // We want messages in chronological order: [...OlderPage, LatestPage]
        const firstPage = allPages[0];
        const allMessages = [...allPages].reverse().flatMap(page => page.messages || []);

        return {
            ...firstPage,
            messages: allMessages
        };
    }, [selectedChatData]);

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
        isSelectedChatError,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage
    };
};
