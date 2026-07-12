import React, { useState } from "react"
import { useQuery } from '@tanstack/react-query';

import { cn } from "@/lib/utils"
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command"
import { Axios } from "@/services/http/Axios";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"

const ChatSearch = ({ setSearchedUser }) => {
    const [searchTerm, setSearchTerm] = useState("");

    const fetchUsers = async () => {
        const response = await Axios.get(`/users/search`, {
            params: {
                name: searchTerm,
            }
        });

        return response.data?.data || [];
    };

    const { data: users = [], isLoading } = useQuery({
        queryKey: ['searchUsers', searchTerm],
        queryFn: fetchUsers,
        enabled: searchTerm.length > 0,
    })

    const handleUserSelect = (user) => {
        setSearchedUser(user);
        setSearchTerm("");
    }

    const showList = searchTerm.length > 0

    return (
        <div className="w-full relative mb-4">
            <div className="relative group">
                <Command shouldFilter={false} className="bg-white border border-slate-200 rounded-2xl shadow-sm group-focus-within:border-blue-300 group-focus-within:ring-4 group-focus-within:ring-blue-50 transition-all duration-200">
                    <CommandInput
                        placeholder="Search for people..."
                        value={searchTerm}
                        onValueChange={setSearchTerm}
                        className="h-11 text-sm border-none focus:ring-0"
                    />
                    <div className={cn(
                        "absolute top-full left-0 w-full bg-white border border-slate-100 rounded-2xl mt-2 shadow-xl z-50 overflow-hidden transition-all duration-200 origin-top",
                        showList ? "scale-100 opacity-100" : "scale-95 opacity-0 pointer-events-none"
                    )}>
                        <CommandList className="max-h-[300px]">
                            <CommandEmpty className="py-6 text-center">
                                {isLoading ?
                                    <div className="flex flex-col items-center gap-2">
                                        <div className="w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
                                        <span className="text-xs text-slate-500 font-medium">Searching people...</span>
                                    </div>
                                    :
                                    <div className="flex flex-col items-center gap-1">
                                        <span className="text-sm font-semibold text-slate-700">No one found</span>
                                        <span className="text-xs text-slate-500">Try a different name</span>
                                    </div>
                                }
                            </CommandEmpty>
                            <CommandGroup heading={<span className="px-2 text-[10px] font-bold text-slate-400 uppercase tracking-wider">People</span>}>
                                {!isLoading && users.length > 0 && users.map((user) => (
                                    <CommandItem
                                        key={user.id}
                                        onSelect={() => {
                                            handleUserSelect(user);
                                        }}
                                        className="flex items-center gap-3 p-3 mx-1 my-1 rounded-xl cursor-pointer hover:bg-slate-50 aria-selected:bg-blue-50 transition-colors"
                                    >
                                        <Avatar className="h-9 w-9 border border-slate-100">
                                            <AvatarImage src={user.image} alt={user.name} />
                                            <AvatarFallback className="bg-slate-100 text-slate-600 text-xs">{user.name?.slice(0, 1)}</AvatarFallback>
                                        </Avatar>
                                        <div className="flex flex-col">
                                            <span className="text-sm font-semibold text-slate-900">{user.name}</span>
                                            <span className="text-[10px] text-slate-500">Click to start chatting</span>
                                        </div>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        </CommandList>
                    </div>
                </Command>
            </div>
        </div>
    )
}

export default React.memo(ChatSearch);


