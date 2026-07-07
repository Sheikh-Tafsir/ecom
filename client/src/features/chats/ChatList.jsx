import React, { useState } from 'react'
import { Link, useParams } from 'react-router-dom';

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { EllipsisVertical, Trash2, Users } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { getLastMessageTime } from '@/utils';
import { CHAT_TYPE } from '@/utils/enums';

const BUTTON_OPTIONS = ["All", "Unread", "Group"];

const ChatList = ({ chats, handleUserSelectorDialogOpen }) => {
  const { id } = useParams();
  const [selectedOption, setSelectedOption] = useState(BUTTON_OPTIONS[0]);

  const filteredChats = (Array.isArray(chats) ? chats : []).filter(chat => {
    if (selectedOption == BUTTON_OPTIONS[2]) {
      return chat.type == CHAT_TYPE.GROUP;
    } else if (selectedOption == BUTTON_OPTIONS[1]) {
      return chat.unreadMessage != 0;
    }

    return true;
  });

  return (
    <div className="flex flex-col h-full">
      <div className="flex gap-2 mb-4 px-2">
        {BUTTON_OPTIONS.map((option) => (
          <Button
            key={option}
            variant={selectedOption == option ? "default" : "ghost"}
            size="sm"
            onClick={() => setSelectedOption(option)}
            className={cn(
                "flex-1 text-xs transition-all duration-200",
                selectedOption == option ? "bg-blue-600 hover:bg-blue-700 text-white shadow-sm" : "text-slate-600 hover:bg-slate-100"
            )}
          >
            {option}
          </Button>
        ))}
      </div>
      <ScrollArea className="flex-1">
        <div className="flex flex-col gap-0 px-2 pb-4">
          {filteredChats.length > 0 ?
            filteredChats.map((chat) => (
              <div
                key={chat.id}
                className={cn(
                    "group flex items-center gap-3 p-3 mb-1 rounded-xl transition-all duration-200 cursor-pointer relative",
                    chat.id == id
                        ? "bg-blue-50 text-blue-900 shadow-sm"
                        : "hover:bg-slate-50 text-slate-700"
                )}
              >
                {chat.id == id && <div className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-blue-600 rounded-r-full" />}
                
                <Link
                  to={`/chats/${chat.id}`}
                  className='flex-1 flex gap-3 min-w-0'
                >
                  <div className="relative flex-shrink-0">
                    <Avatar className="h-12 w-12 border-2 border-white shadow-sm">
                      <AvatarImage src={chat?.image} alt={chat.name} />
                      <AvatarFallback className="bg-gradient-to-br from-blue-500 to-blue-600 text-white font-medium">
                        {chat?.name?.slice(0, 1)}
                      </AvatarFallback>
                    </Avatar>
                    {/* Mock online status for now */}
                    <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full" />
                  </div>

                  <div className="flex-1 min-w-0 py-0.5">
                    <div className="flex items-center justify-between mb-0.5">
                      <h3 className={cn(
                          "font-semibold truncate text-sm",
                          chat?.unreadMessage > 0 ? "text-slate-900" : "text-slate-700"
                      )}>
                        {chat?.name}
                      </h3>
                      <span className={cn(
                          "text-[10px] whitespace-nowrap ml-2 font-medium",
                          chat.id == id ? "text-blue-500" : "text-slate-400"
                      )}>
                        {getLastMessageTime(chat.lastSent)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between gap-2">
                      <p className={cn(
                          "text-xs truncate flex-1",
                          chat?.unreadMessage > 0 ? "text-slate-900 font-medium" : "text-slate-500"
                      )}>
                        {chat?.lastMessage || "No messages yet"}
                      </p>
                      
                      {chat?.unreadMessage > 0 && (
                        <span className="flex items-center justify-center bg-blue-600 text-white text-[10px] font-bold h-4 w-4 rounded-full">
                          {chat.unreadMessage}
                        </span>
                      )}
                    </div>
                  </div>
                </Link>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-8 w-8 rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                      <EllipsisVertical className="h-4 w-4 text-slate-400" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48">
                    {chat.type != CHAT_TYPE.GROUP &&
                      <DropdownMenuItem onClick={() => handleUserSelectorDialogOpen([], chat.otherUserId)}>
                        <Users className="mr-2 h-4 w-4" />
                        <span>Create group chat</span>
                      </DropdownMenuItem>
                    }
                    <DropdownMenuItem className="text-red-600">
                      <Trash2 className="mr-2 h-4 w-4" />
                      <span>Delete chat</span>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))
            :
            <div className='flex flex-col items-center justify-center py-10 px-4 text-center'>
              <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mb-3">
                <Users className="h-6 w-6 text-slate-400" />
              </div>
              <p className="text-sm text-slate-500 font-medium">No conversations found</p>
              <p className="text-xs text-slate-400 mt-1">Start a new chat to begin messaging</p>
            </div>
          }
        </div>
      </ScrollArea>
    </div>
  )
}

export default React.memo(ChatList)