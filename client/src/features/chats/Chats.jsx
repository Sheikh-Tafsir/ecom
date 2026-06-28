import {useState, useEffect, useCallback} from 'react';
import {useParams, useNavigate} from "react-router-dom";
import {useQueryClient} from '@tanstack/react-query';

import ChatList from './ChatList';
import ChatMessages from './ChatMessages';
import ChatSearch from './ChatSearch';
import {TOAST_TYPE} from '@/utils/enums';
import UserSelectorDialog from './UserSelectorDialog';

import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx";
import {useUserStore} from "@/store/useUserStore.js";

import {useChatData} from './hooks/useChatData';
import {useChatSync} from './hooks/useChatSync';
import {useChatActions} from './hooks/useChatActions';
import { notify } from '@/components/common/notification';

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

    const {
        chats,
        selectedChat,
        chatMapByParticipants,
        isChatsLoading,
        isSelectedChatLoading,
        isSelectedChatError
    } = useChatData(id);

    const {updateChatOnMessage} = useChatSync(id, user?.id);

    const handleNewChat = useCallback((chatId) => {
        setSearchedUser(null);
        setNewChat(null);
        navigate(`/chats/${chatId}`, {replace: true});
        queryClient.invalidateQueries(['selected_chat', chatId]);
    }, [navigate, queryClient]);

    const {handleSendMessage, handleGroupManagementRequest} = useChatActions(
        id,
        user?.id,
        updateChatOnMessage,
        handleNewChat,
    );

    useEffect(() => {
        if (isSelectedChatError) {
            navigate('/chats');
        }
    }, [isSelectedChatError, navigate]);

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
    }, [searchedUser, user, chatMapByParticipants, handleNewChat]);

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

    const onGroupManagementSuccess = (data) => {
        setIsUserSelectionDrawerOpen(false);
        notify(TOAST_TYPE.SUCCESS, data.message)
        if (!id) navigate(`/chats/${data.data}`);
    };

    const handleGroupAction = (users, action) => {
        handleGroupManagementRequest(users, action, onGroupManagementSuccess);
    };

    const onSendMessage = (content, image) => {
        handleSendMessage(content, image, searchedUser);
    };

    return (
        <>
            {(isChatsLoading || isSelectedChatLoading || !user?.id) && <PageLoadingOverlay/>}

            <div className="flex justify-between pb-8 pt-4 overflow-hidden">
                <div className='w-[23.3%] bg-white h-[calc(90vh-2rem)] px-2'>
                    <div className='p-2'>
                        <h1 className="text-2xl font-semibold text-black mt-1 mb-3">Messages</h1>
                        <ChatSearch setSearchedUser={setSearchedUser}/>
                    </div>

                    <ChatList chats={chats || []} handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}/>
                </div>
                <div className="w-[76%]">
                    <ChatMessages onSendMessage={onSendMessage} chat={newChat || selectedChat || {}}
                                  handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}/>
                </div>

                <UserSelectorDialog
                    isOpen={isUserSelectionDrawerOpen}
                    onClose={() => setIsUserSelectionDrawerOpen(false)}
                    preSelecteedUserIds={preSelectedUserIds}
                    avoidUserIds={avoidUserIds}
                    confirmUsersSelection={handleGroupAction}
                />
            </div>
        </>
    );
};

export default Chat;
