import React, { useState, useEffect, useRef, useMemo } from 'react';

import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Send, Info } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import ImageInputInplace from '@/components/common/ImageInputInplace';
import { CHAT_TYPE, CONTENT_TYPE } from '@/utils/enums';
import ParticipantDrawer from './ParticipantDrawer';
import ChatInfo from './ChatInfo';
import { useUserStore } from '@/store/useUserStore';

const ChatMessages = ({ onMessageSend, chat, handleUserSelectorDialogOpen }) => {
  const { user } = useUserStore();
  const messagesEndRef = useRef(null);
  const [newMessage, setNewMessage] = useState('');
  const [newImage, setNewImage] = useState(null);
  const [isParticipantDrawerOpen, setIsParticipantDrawerOpen] = useState(false);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView();
  }, [chat]);

  const participantsMap = useMemo(() => {
    const map = new Map();
    chat?.participants?.forEach(p => map.set(p.id, p));
    return map;
  }, [chat?.participants]);

  //send message
  const handleSendMessage = (e) => {
    e.preventDefault();

    if (!newMessage.trim() && !newImage) return;

    // Emit a 'private message' event to the server
    onMessageSend(newMessage, newImage);
    setNewMessage('');
    setNewImage(null);
  };

  return (
    <div className="h-[calc(90vh-2rem)] flex justify-between">
      <div className="flex flex-col w-[100%] xl:w-[69%]">
        {(chat && Object.keys(chat).length > 0) ?
          <>
            <div className="flex items-center gap-4 mb-4 p-2 bg-blue-600">
              <Avatar>
                <AvatarImage src="/placeholder.svg?height=40&width=40" alt="Chat Avatar" />
                <AvatarFallback>{chat?.name?.slice(0, 1)}</AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <h1 className="text-lg font-semibold text-white">{chat?.name}</h1>
                {/* <p className="text-sm text-muted-foreground text-gray-200">Online</p> */}
              </div>
              <Button variant="outline" size="icon"
                className="bg-transparent border-none text-white xl:hidden" onClick={() => setIsParticipantDrawerOpen(!isParticipantDrawerOpen)}>
                <Info />
              </Button>
              <ParticipantDrawer isOpen={isParticipantDrawerOpen}
                onClose={() => setIsParticipantDrawerOpen(false)} name={chat?.name}
                type={chat.type} participants={chat.participants}
                handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}
                connected={chat?.messages?.length > 0}
              />
            </div>

            <Card className="flex-1 mb-4 overflow-y-scroll">
              <ScrollArea className="p-4">
                <div className="flex flex-col gap-4">
                  {chat.id && chat?.messages?.length > 0 && chat?.messages.map((message, index) => (
                    <div
                      key={message.id || index}
                      className={`flex ${message.senderId === user.id ? 'justify-end' : 'justify-start'}`}
                    >
                      <div className='max-w-[80%]'>
                        {chat?.type == CHAT_TYPE.GROUP && message.senderId != user.id &&
                          <p className="text-xs mb-1">{participantsMap.get(message.senderId)?.name || 'Unknown'}</p>
                        }
                        <div
                          className={`rounded-lg px-4 py-2 w-fit text-sm 2xl:text-md
                        ${message.senderId == user.id ? (message.isTemporary ? 'text-primary-foreground bg-blue-500' : 'text-primary-foreground bg-blue-600') : 'bg-muted'}`}
                        >
                          {message.contentType == CONTENT_TYPE.TEXT ?
                            <p>{message.content}</p>
                            :
                            message.contentType == CONTENT_TYPE.IMAGE ?
                              <img src={message.content} className='max-w-[500px]' />
                              :
                              <></>
                          }
                        </div>
                      </div>
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </div>
              </ScrollArea>
            </Card>
          </>
          :
          <span className='h-[100%]'></span>
        }

        <form onSubmit={handleSendMessage} className="flex gap-2">
          <ImageInputInplace image={newImage} setImage={setNewImage} />
          <Input
            placeholder="Type a message..."
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            className="flex-1"
          />
          <Button type="submit" size="icon" className="bg-blue-600">
            <Send className="h-4 w-4" />
            <span className="sr-only">Send message</span>
          </Button>
        </form>
      </div>

      <div className='hidden xl:block w-0 xl:w-[30%]'>
        {(chat?.name || chat.type) &&
          <ChatInfo name={chat?.name} type={chat.type} participants={chat.participants}
            handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}
            connected={chat?.messages?.length > 0}
          />
        }
      </div>
    </div>
  )
}

export default React.memo(ChatMessages)