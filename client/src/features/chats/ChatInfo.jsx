import { UserRoundPlus } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { CHAT_MEMBER_TYPE, CHAT_TYPE } from "@/utils/enums"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { cn } from "@/lib/utils"
import { ScrollArea } from "@/components/ui/scroll-area"

const ChatInfo = ({ name, image, type, participants, handleUserSelectorDialogOpen, connected }) => {
    return (
        <div className="flex flex-col h-full bg-white animate-in slide-in-from-right duration-300">
            <div className="p-8 flex flex-col items-center border-b border-slate-50">
                <div className="relative mb-4 group">
                    <Avatar className="h-24 w-24 border-4 border-slate-50 shadow-md transition-transform duration-300 group-hover:scale-105">
                        <AvatarImage src={image} alt={name} />
                        <AvatarFallback className="text-4xl bg-gradient-to-br from-blue-500 to-blue-600 text-white font-bold">
                            {name?.slice(0, 1)}
                        </AvatarFallback>
                    </Avatar>
                    <div className="absolute bottom-1 right-1 w-5 h-5 bg-green-500 border-4 border-white rounded-full shadow-sm" />
                </div>
                <h3 className="text-lg font-bold text-slate-900 mb-1">{name}</h3>
                <p className="text-xs font-medium text-slate-500 flex items-center gap-1.5">
                    {type == CHAT_TYPE.GROUP ? "Group Chat" : "Direct Message"}
                    <span className="w-1 h-1 bg-slate-300 rounded-full" />
                    {connected ? "Active" : "Away"}
                </p>
            </div>

            <ScrollArea className="flex-1">
                <div className="p-6">
                    {type == CHAT_TYPE.GROUP ? (
                        <div className="space-y-6">
                            <div className="flex items-center justify-between">
                                <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Members ({participants?.length || 0})</h4>
                                <Button 
                                    variant="ghost" 
                                    size="sm" 
                                    onClick={() => handleUserSelectorDialogOpen(participants, null)} 
                                    className="h-8 text-blue-600 hover:text-blue-700 hover:bg-blue-50 font-semibold text-xs rounded-lg"
                                >
                                    <UserRoundPlus className="h-3.5 w-3.5 mr-1.5" />
                                    Add
                                </Button>
                            </div>
                            
                            <div className="space-y-1">
                                {participants && participants.map((participant) => (
                                    <div className="flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 transition-colors group" key={participant.id}>
                                        <div className="flex items-center gap-3">
                                            <Avatar className="h-9 w-9 border border-white shadow-sm">
                                                <AvatarImage src={participant.image} alt={participant.name} />
                                                <AvatarFallback className="bg-slate-100 text-slate-600 text-xs font-medium">{participant?.name?.slice(0, 1)}</AvatarFallback>
                                            </Avatar>
                                            <div className="flex flex-col">
                                                <span className="text-sm font-semibold text-slate-800">{participant?.name}</span>
                                                <span className="text-[10px] text-slate-500">Active now</span>
                                            </div>
                                        </div>
                                        <Badge className={cn(
                                            "h-5 text-[9px] font-bold px-2 rounded-md shadow-sm border-none",
                                            participant.role == CHAT_MEMBER_TYPE.ADMIN 
                                                ? "bg-amber-100 text-amber-700" 
                                                : "bg-blue-100 text-blue-700"
                                        )}>
                                            {participant.role}
                                        </Badge>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-6">
                            <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                                <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-3">About</h4>
                                <p className="text-xs text-slate-600 leading-relaxed">
                                    This is a secure direct conversation with {name}. Messages are end-to-end encrypted and visible only to you and them.
                                </p>
                            </div>
                            
                            <div className="space-y-2">
                                <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1">Settings</h4>
                                <Button variant="ghost" className="w-full justify-start text-xs font-medium text-slate-700 hover:bg-slate-50 rounded-xl px-2 h-10">
                                    <span className="w-8 flex justify-center text-slate-400">🔔</span>
                                    Mute Notifications
                                </Button>
                                <Button variant="ghost" className="w-full justify-start text-xs font-medium text-red-600 hover:bg-red-50 rounded-xl px-2 h-10">
                                    <span className="w-8 flex justify-center">🚫</span>
                                    Block User
                                </Button>
                            </div>
                        </div>
                    )}
                </div>
            </ScrollArea>
        </div>
    )
}

export default ChatInfo