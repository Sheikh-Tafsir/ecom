import React, { useState, useEffect, useRef, useMemo } from 'react';

import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Send, ChevronLeft } from 'lucide-react'
import { Link } from 'react-router-dom';
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import ImageInputInplace from '@/components/common/ImageInputInplace';
import { CHAT_TYPE, CONTENT_TYPE } from '@/utils/enums';
import ChatInfo from './ChatInfo';
import { useUserStore } from '@/store/useUserStore';

import { format, isSameDay, isYesterday } from 'date-fns';
import { cn } from "@/lib/utils";

const ChatMessages = ({ onSendMessage, chat, handleUserSelectorDialogOpen, isMobileView }) => {

  const { user } = useUserStore();
  const messagesEndRef = useRef(null);
  const [newMessage, setNewMessage] = useState('');
  const [newImage, setNewImage] = useState(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chat?.messages]);

  const participantsMap = useMemo(() => {
    const map = new Map();
    chat?.participants?.forEach(p => map.set(p.id, p));
    return map;
  }, [chat?.participants]);

  const handleSendMessage = (e) => {
    e.preventDefault();

    if (!newMessage.trim() && !newImage) return;

    onSendMessage(newMessage, newImage);
    setNewMessage('');
    setNewImage(null);
  };

  const renderDateSeparator = (date) => {
    const now = new Date();
    const messageDate = new Date(date);
    let label = format(messageDate, 'MMMM d, yyyy');
    
    if (isSameDay(messageDate, now)) label = 'Today';
    else if (isYesterday(messageDate, now)) label = 'Yesterday';

    return (
      <div className="flex justify-center my-6">
        <span className="bg-slate-100 text-slate-500 text-[10px] font-bold px-3 py-1 rounded-full uppercase tracking-wider shadow-sm">
          {label}
        </span>
      </div>
    );
  };

  const renderMessage = (message, index, allMessages) => {
    const isMe = message.senderId == user.id;
    const prevMessage = allMessages[index - 1];
    const isFirstInGroup = !prevMessage || prevMessage.senderId != message.senderId || !isSameDay(new Date(prevMessage.createdAt), new Date(message.createdAt));
    
    const sender = participantsMap.get(message.senderId);

    return (
      <div key={message.id || index} className="flex flex-col">
        {index == 0 || !isSameDay(new Date(allMessages[index-1].createdAt), new Date(message.createdAt)) ? 
          renderDateSeparator(message.createdAt) : null
        }
        <div className={cn(
          "flex items-end gap-2 mb-1",
          isMe ? "flex-row-reverse" : "flex-row"
        )}>
          {!isMe && (
            <div className="w-8 flex-shrink-0">
              {isFirstInGroup ? (
                <Avatar className="h-8 w-8">
                  <AvatarImage src={sender?.image} alt={sender?.name} />
                  <AvatarFallback className="text-[10px] bg-slate-200">{sender?.name?.slice(0, 1)}</AvatarFallback>
                </Avatar>
              ) : <div className="w-8" />}
            </div>
          )}
          
          <div className={cn(
            "max-w-[70%] flex flex-col",
            isMe ? "items-end" : "items-start"
          )}>
            {isFirstInGroup && !isMe && chat?.type == CHAT_TYPE.GROUP && (
              <span className="text-[10px] font-semibold text-slate-500 ml-1 mb-1">
                {sender?.name || 'Unknown'}
              </span>
            )}
            
            <div className={cn(
              "relative px-4 pt-2 pb-1 text-sm shadow-sm",
              isMe 
                ? "bg-blue-600 text-white rounded-2xl rounded-br-none" 
                : "bg-white text-slate-800 border border-slate-100 rounded-2xl rounded-bl-none"
            )}>
              {message.contentType == CONTENT_TYPE.TEXT ? (
                <p className="leading-relaxed whitespace-pre-wrap break-words">{message.content}</p>
              ) : message.contentType == CONTENT_TYPE.IMAGE ? (
                <div className="rounded-lg overflow-hidden my-1 border border-slate-100/10">
                  <img src={message.content} className="max-w-full h-auto object-cover max-h-80" alt="Sent image" />
                </div>
              ) : null}
              
              <div className={cn(
                "mt-[-5px] text-[9px] opacity-70 flex justify-end items-center",
                isMe ? "text-blue-100" : "text-slate-400"
              )}>
                {format(new Date(message.createdAt), 'HH:mm')}
                {isMe && message.isTemporary && (
                  <span className="w-2 h-2 rounded-full border border-blue-200 border-t-transparent animate-spin" />
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const isEmpty = !chat || Object.keys(chat).length == 0;

  return (
    <div className="h-[calc(90vh-2.5rem)] flex bg-slate-50 rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
      <div className="flex-1 flex flex-col min-w-0">
        {!isEmpty ? (
          <>
          <div className="flex items-center justify-between px-4 md:px-6 py-4 bg-white border-b border-slate-100 z-10">
            <div className="flex items-center gap-2 md:gap-4">
              {isMobileView && (
                <Link to="/chats" className="md:hidden p-1 mr-1 text-slate-500 hover:text-blue-600 transition-colors">
                  <ChevronLeft className="h-6 w-6" />
                </Link>
              )}
              <div className="relative">
                <Avatar className="h-10 w-10 border-2 border-slate-50 shadow-sm">
                  <AvatarImage src={chat?.image} alt={chat?.name} />
                  <AvatarFallback className="bg-blue-100 text-blue-600 font-semibold">{chat?.name?.slice(0, 1)}</AvatarFallback>
                </Avatar>
                <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900">{chat?.name}</h2>
                <div className="flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse" />
                  <p className="text-[11px] text-slate-500 font-medium">Online</p>
                </div>
              </div>
            </div>
          </div>

          <ScrollArea className="flex-1 p-6">
            <div className="flex flex-col">
              {chat?.messages?.length > 0 ? (
                chat.messages.map((message, index) => renderMessage(message, index, chat.messages))
              ) : (
                <div className="flex flex-col items-center justify-center py-20 opacity-40">
                  <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mb-4 shadow-sm">
                    <Send className="h-8 w-8 text-blue-600 transform rotate-45" />
                  </div>
                  <p className="text-sm font-medium">No messages yet</p>
                  <p className="text-xs mt-1">Send a greeting to start the conversation!</p>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </ScrollArea>

          <div className="p-4 bg-white border-t border-slate-100">
            <form onSubmit={handleSendMessage} className="flex items-end gap-3 bg-slate-50 p-2 rounded-2xl border border-slate-100 focus-within:border-blue-200 focus-within:ring-2 focus-within:ring-blue-100/50 transition-all duration-200">
              <div className="flex-shrink-0 mb-1 ml-1">
                <ImageInputInplace image={newImage} setImage={setNewImage} />
              </div>
              <textarea
                placeholder="Type your message..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key == 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage(e);
                  }
                }}
                className="flex-1 max-h-32 min-h-[40px] bg-transparent border-none focus:ring-0 text-sm resize-none px-1 text-slate-700 "
              />
              <Button 
                type="submit" 
                size="icon" 
                disabled={!newMessage.trim() && !newImage}
                className={cn(
                  "h-10 w-10 rounded-xl transition-all duration-200 flex-shrink-0 mb-1 mr-1",
                  (newMessage.trim() || newImage) ? "bg-blue-600 hover:bg-blue-700 shadow-md translate-y-0" : "bg-slate-200 text-slate-400 translate-y-1"
                )}
              >
                <Send className="h-4 w-4" />
              </Button>
            </form>
          </div>
        </>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center p-12 text-center bg-white">
          <div className="relative mb-8">
            <div className="absolute -inset-4 bg-blue-100/50 rounded-full blur-2xl" />
            <div className="relative w-24 h-24 bg-gradient-to-br from-blue-500 to-blue-700 rounded-3xl rotate-12 flex items-center justify-center shadow-xl">
              <Send className="h-12 w-12 text-white transform -rotate-12" />
            </div>
          </div>
          <h3 className="text-xl font-bold text-slate-900 mb-2">Your Messages</h3>
          <p className="text-slate-500 max-w-[280px] text-sm leading-relaxed mb-8">
            Select a conversation from the sidebar to start chatting with your friends and colleagues.
          </p>
          <Button 
            className="bg-blue-600 hover:bg-blue-700 rounded-full px-8 shadow-lg shadow-blue-200 transition-all hover:scale-105 active:scale-95"
            onClick={() => handleUserSelectorDialogOpen([], null)}
          >
            Start New Conversation
          </Button>
        </div>
      )}
      </div>

      {/* Side Chat Info - Shown on desktop screens */}
      <div className='hidden xl:block xl:w-[350px] 2xl:w-[400px] border-l border-slate-100 bg-white'>
        {!isEmpty && chat?.name && (
          <ChatInfo 
            name={chat?.name} 
            type={chat.type} 
            participants={chat.participants}
            handleUserSelectorDialogOpen={handleUserSelectorDialogOpen}
            connected={chat?.messages?.length > 0}
          />
        )}
      </div>
    </div>
  )
}

export default React.memo(ChatMessages)