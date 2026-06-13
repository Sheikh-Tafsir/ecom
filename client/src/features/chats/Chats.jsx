import {useState, useEffect, useCallback, useMemo} from 'react';
import {useParams, useNavigate} from "react-router-dom"
import {useQuery, useQueryClient} from '@tanstack/react-query';

import ChatList from './ChatList';
import ChatMessages from './ChatMessages';
import ChatSearch from './ChatSearch';
import {CONTENT_TYPE, REGULAR_ACTION, TOAST_TYPE} from '@/utils/enums';
import UserSelectorDialog from './UserSelectorDialog';

import {
    GROUP_CREATE_REQUEST,
    GROUP_CREATE_RESPONSE, GROUP_UPDATE_REQUEST,
    GROUP_UPDATE_RESPONSE,
    MESSAGE_RECEIVE_EVENT,
    MESSAGE_SEND_EVENT
} from "@/services/realtime/socketEvents.js";
import {Axios} from "@/services/http/Axios.js";
import socket from "@/services/realtime/socket.js";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx";
import {isNull} from "@/utils/index.js";
import {ToastAlert} from "@/components/common/ToastAlert.jsx";
import {useUserStore} from "@/store/useUserStore.js";

const Chat = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const {user} = useUserStore();
    const queryClient = useQueryClient();

    const [searchedUser, setSearchedUser] = useState(null);
    const [newChat, setNewChat] = useState(null);
    const [isUserSelectionDrawerOpen, setIsUserSelectionDrawerOpen] = useState(false);
    const [preSelectedUserIds, setPreSelectedUserIds] = useState([]);
    const [avoidUserIds, setAvoidUserIds] = useState([]);
    const [toastData, setToastData] = useState({message: "", type: "", id: 0});

    const fetchChatList = async () => {
        const response = await Axios.get(`/chats`)
        return response.data.data || [];
    }

    const {data: chats = [], isLoading: isChatsLoading} = useQuery({
        queryKey: ['chats'],
        queryFn: fetchChatList,
    })

    const fetchSelectedChat = async () => {
        //if (!id) return [];
        const response = await Axios.get(`/chats/${id}`);
        //console.log(response.data.data);
        return response.data.data || {};
    }

    const {data: selectedChat = {}, isLoading: isSelectedChatLoading, isError: isSelectedChatError} = useQuery({
        queryKey: ['selected_chat', id],
        queryFn: fetchSelectedChat,
        enabled: !!id,
    })

    const chatMapByParticipants = useMemo(() => {
        const map = new Map();

        chats.forEach(chat => {
            if (!chat?.Participants || chat.Participants.length !== 2) return;

            const ids = chat.Participants.map(participant => participant.userId).sort((a, b) => a - b); // sort to ensure order
            const key = `${ids[0]}_${ids[1]}`;
            map.set(key, chat);
        });

        return map;
    }, [chats]);

    useEffect(() => {
        if (isSelectedChatError) {
            navigate('/chats');
        }
    }, [isSelectedChatError, navigate]);

    useEffect(() => {
        if (!socket) return;

        // Listen for incoming messages
        const handleReceiveMessage = (response) => {
            console.log('received message:', response);
            updateChatOnMessage(response.data);
        }

        const handleGroupCreationResponse = (response) => {
            console.log('group created:', response);
            queryClient.invalidateQueries(['chats']);
        }

        const handleUpdateResponse = (response) => {
            console.log('group updated:', response);
            queryClient.invalidateQueries(['chats']);
        }

        socket.on(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
        socket.on(GROUP_CREATE_RESPONSE, handleGroupCreationResponse);
        socket.on(GROUP_UPDATE_RESPONSE, handleUpdateResponse);

        return () => {
            socket.off(MESSAGE_RECEIVE_EVENT, handleReceiveMessage);
            socket.off(GROUP_CREATE_RESPONSE, handleGroupCreationResponse);
            socket.off(GROUP_UPDATE_RESPONSE, handleUpdateResponse);
        };
    }, [id, user?.id, queryClient]);

    useEffect(() => {
        if (!id || isNull(user?.id) || isSelectedChatLoading || isSelectedChatError) return;

        setSearchedUser(null);
        setNewChat(null);

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
    }, [id, user?.id, isSelectedChatLoading, isSelectedChatError, queryClient]);

    const updateChatOnMessage = (newMessage) => {
        if (isNull(newMessage.content)) return;

        queryClient.setQueryData(['chats'], (oldChats = []) => {
            const currentChat = oldChats.find(chat => chat.id === newMessage.chatId);
            if (!currentChat) {
                queryClient.invalidateQueries(['chats']);
                return oldChats;
            }

            const isOwnSentMessage = newMessage.senderId === user.id;

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
                const isOwnSentMessage = newMessage.senderId === user.id && !newMessage?.isTemporary && newMessage.tempId;

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
    }

    const handleNewChat = (chatId) => {
        if (!searchedUser) return;

        setSearchedUser(null);
        setNewChat(null);

        navigate(`/chats/${chatId}`, {replace: true});
        queryClient.invalidateQueries(['selected_chat', chatId]);
    }

    useEffect(() => {
        if (!searchedUser || !user?.id) return;

        const ids = [user.id, searchedUser.id].sort((a, b) => a - b);
        const key = `${ids[0]}_${ids[1]}`;
        const existingChat = chatMapByParticipants.get(key);

        if (!existingChat?.id) {
            setNewChat({
                id: Date.now().toString() + Math.random().toString(36).slice(2),
                name: searchedUser.name,
            });

            return;
        }

        handleNewChat(existingChat.id);
    }, [searchedUser, user, chatMapByParticipants]);

    const getImageLink = async (image) => {
        try {
            const formData = new FormData();
            formData.append('image', image);

            const response = await Axios.post(`/common/upload-image`, formData, {
                headers: {'Content-Type': 'multipart/form-data'},
            });
            //console.log(response);

            return response.data?.data; // or whatever your API returns
        } catch (err) {
            console.error("Image upload failed:", err);
            // Optionally handle failed image upload
        }
    }

    const updateChatLocally = (content, contentType, tempId) => {
        const createdAt = new Date().toISOString();

        const tempMessage = {
            id: tempId,
            content,
            contentType,
            chatId: Number(id),
            senderId: user.id,
            createdAt,
            updatedAt: createdAt,
            isTemporary: true,
            tempId,
        };

        updateChatOnMessage(tempMessage);
    }

    const handleSendMessage = useCallback(async (content, image) => {
        if (!id && !searchedUser) return;

        let contentType = CONTENT_TYPE.TEXT;
        if (image) {
            content = await getImageLink(image);
            //console.log(message);
            contentType = CONTENT_TYPE.IMAGE;
        }

        const tempId = Date.now().toString() + Math.random().toString(36).slice(2);
        updateChatLocally(content, contentType, tempId);

        // Emit a 'private message' event to the server
        socket.emit(MESSAGE_SEND_EVENT, {
            ...(searchedUser?.id ? {receiverId: searchedUser.id} : {chatId: Number(id)}),
            content,
            contentType,
            tempId,
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                console.log(acknowledgment.error);
                showToast(acknowledgment.error, TOAST_TYPE.ERROR);
                return;
            }

            console.log('sent message:', acknowledgment);
            const sentMessage = acknowledgment.data;
            handleNewChat(sentMessage.chatId);
        });
    }, [searchedUser, id, queryClient, user, navigate]);

    const handleUserSelectorDialogOpen = (alreadyParticipants, preSelectedUserId) => {
        if (preSelectedUserId) {
            setPreSelectedUserIds([preSelectedUserId]);
        }
        if (alreadyParticipants?.length) {
            const ids = alreadyParticipants.map((participant) => participant.userId);
            setAvoidUserIds(ids);
        }

        setIsUserSelectionDrawerOpen(true);
    }

    const handleGroupManagementRequest = (users, action) => {
        socket.emit(action === REGULAR_ACTION.CREATE ? GROUP_CREATE_REQUEST : GROUP_UPDATE_REQUEST, {
            users,
            chatId: id
        }, (acknowledgment) => {
            if (acknowledgment.error) {
                console.error(acknowledgment.error);
                showToast(acknowledgment.error, TOAST_TYPE.ERROR);
                return;
            }

            //console.log(acknowledgment.data);
            setIsUserSelectionDrawerOpen(false);
            showToast(acknowledgment.data.message, TOAST_TYPE.SUCCESS);
            if (action === REGULAR_ACTION.CREATE) navigate(`/chats/${acknowledgment.data.data}`)
        });
    };

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()}) // ensure uniqueness
    }

    return (
        <>
            {(isChatsLoading || isSelectedChatLoading || !user?.id) && <PageLoadingOverlay/>}

            <div className="px-8 flex justify-between pb-8 mt-[-20px] overflow-hidden">
                <div className='w-[23.3%] bg-white h-[calc(90vh-2rem)] px-2'>
                    <div className='p-2'>
                        <h1 className="text-2xl font-semibold text-black mt-1 mb-3">Messages</h1>
                        <ChatSearch setSearchedUser={setSearchedUser}/>
                    </div>

                    <ChatList chats={chats || []} handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}/>
                </div>
                <div className="w-[76%]">
                    <ChatMessages onMessageSend={handleSendMessage} chat={newChat || selectedChat || {}}
                                  handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}/>
                </div>

                <UserSelectorDialog
                    isOpen={isUserSelectionDrawerOpen}
                    onClose={() => setIsUserSelectionDrawerOpen(false)}
                    preSelecteedUserIds={preSelectedUserIds}
                    avoidUserIds={avoidUserIds}
                    confirmUsersSelection={handleGroupManagementRequest}
                />
            </div>

            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    );
};

export default Chat