import React, { useState } from 'react'
import { Link, useParams } from 'react-router-dom';

import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
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
    <>
      <div className="flex gap-2 mb-5 justify-center">
        {BUTTON_OPTIONS.map((option) => (
          <Button
            key={option}
            variant={selectedOption == option ? "default" : "outline"}
            onClick={() => setSelectedOption(option)}
            className={selectedOption == option ? "bg-blue-900" : ""}
          >
            {option}
          </Button>
        ))}
      </div>
      <Card>
        <ScrollArea>
          <div className="flex flex-col gap-2 p-1 max-h-[calc(80vh-8rem)]">
            {filteredChats.length > 0 ?
              filteredChats.map((chat, index) => (
                <div
                  key={index}
                  className={`flex justify-between gap-2 p-4 transition-colors ${chat.id == id
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 hover:bg-blue-500 hover:text-white'
                    }`}
                >
                  <Link
                    to={`/chats/${chat.id}`}
                    className='flex gap-2 cursor-pointer w-full'
                  >
                    <Avatar>
                      <AvatarImage src={chat?.image} alt={chat.name} />
                      <AvatarFallback>{chat?.name?.slice(0, 1)}</AvatarFallback>
                    </Avatar>
                    <div className="flex-1">
                      <p className={`${chat?.unreadMessage > 0 ? 'font-bold' : ''} h-6 overflow-hidden`}>{chat?.name}</p>
                      <div className="flex items-center justify-between w-full">
                        {chat?.unreadMessage > 0 && (
                          <span className="truncate text-xs">{chat.unreadMessage} new messages</span>
                        )}
                        <span className={`text-xs text-muted-foreground whitespace-nowrap ml-auto ${chat.id == id ? 'text-white' : ''}`}>
                          {getLastMessageTime(chat.lastSent)}
                        </span>
                      </div>
                    </div>
                  </Link>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild className="max-h-[50%]">
                      <Button variant="outline" size="icon" className="rounded bg-transparent border-none hover:bg-gray-100">
                        <EllipsisVertical />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      {chat.type != CHAT_TYPE.GROUP &&
                        <DropdownMenuItem asChild className="py-3 px-4 w-full justify-start">
                          <Button variant="ghost" className="w-full" onClick={() => handleUserSelectorDialogOpen([], chat.otherUserId)}>
                            <Users className="h-4 w-4" />
                            Create group chat with {chat?.shortName}
                          </Button>
                        </DropdownMenuItem>
                      }
                      <DropdownMenuItem asChild className="py-3 px-4 w-full justify-start">
                        <Button variant="ghost" className="w-full text-red-600 cursor-pointer">
                          <Trash2 />
                          Delete
                        </Button>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              ))
              :
              <div className='flex mx-auto'>
                <p>No chats</p>
              </div>
            }
          </div>
        </ScrollArea>
      </Card>
    </>
  )
}

export default React.memo(ChatList)