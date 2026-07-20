import {useState, useEffect, useCallback} from 'react';
import {useParams, useNavigate} from "react-router-dom";
import {useQueryClient} from '@tanstack/react-query';

import { Button } from "@/components/ui/button";
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
import { toastify } from '@/common/toastify.js';
import { cn } from '@/lib/utils';

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
        toastify(TOAST_TYPE.SUCCESS, data.message)
        if (!id) navigate(`/chats/${data.data}`);
    };

    const handleGroupAction = (users, action) => {
        handleGroupManagementRequest(users, action, onGroupManagementSuccess);
    };

    const onSendMessage = (content, image) => {
        handleSendMessage(content, image, searchedUser);
    };

    return (
        <div className="h-[calc(100vh-120px)] min-h-[600px] flex bg-white rounded-3xl overflow-hidden border border-slate-200 shadow-sm mx-4 my-2 relative">
            {(isChatsLoading || isSelectedChatLoading || !user?.id) && <PageLoadingOverlay/>}

            {/* Sidebar - Hidden on mobile when a chat is selected */}
            <div className={cn(
                "w-full md:w-[300px] xl:w-[320px] 2xl:w-[380px] flex-shrink-0 flex flex-col border-r border-slate-100 bg-slate-50/50 transition-all duration-300",
                id ? "hidden md:flex" : "flex"
            )}>
                <div className="p-6 pb-2">
                    <div className="flex items-center justify-between mb-6">
                        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Messages</h1>
                        <Button 
                            variant="ghost" 
                            size="icon" 
                            className="rounded-full bg-white shadow-sm border border-slate-100 text-blue-600 hover:bg-blue-50"
                            onClick={() => setIsUserSelectionDrawerOpen(true)}
                        >
                            <span className="text-xl">+</span>
                        </Button>
                    </div>
                    <ChatSearch setSearchedUser={setSearchedUser}/>
                </div>

                <div className="flex-1 overflow-hidden">
                    <ChatList chats={chats || []} handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}/>
                </div>
            </div>

            {/* Main Chat Area - Hidden on mobile when no chat is selected */}
            <div className={cn(
                "flex-1 flex flex-col min-w-0 bg-white transition-all duration-300",
                !id && !newChat ? "hidden md:flex" : "flex"
            )}>
                <ChatMessages 
                    onSendMessage={onSendMessage} 
                    chat={newChat || selectedChat || {}}
                    handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}
                    isMobileView={!!(id || newChat)}
                />
            </div>

            <UserSelectorDialog
                isOpen={isUserSelectionDrawerOpen}
                onClose={() => setIsUserSelectionDrawerOpen(false)}
                preSelecteedUserIds={preSelectedUserIds}
                avoidUserIds={avoidUserIds}
                confirmUsersSelection={handleGroupAction}
            />
        </div>
    );
};

export default Chat;
