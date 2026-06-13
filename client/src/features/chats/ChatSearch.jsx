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
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";

const ChatSearch = ({ setSearchedUser }) => {
    const [searchTerm, setSearchTerm] = useState("");

    const fetchUsers = async () => {
        const response = await Axios.get(`/users`, {
            params: {
                name: searchTerm,
                size: 20
            }
        });
        return response.data.data?.content || [];
    };

    const { data: users = [], isLoading } = useQuery({
        queryKey: ['searchUers', searchTerm],
        queryFn: fetchUsers,
        enabled: searchTerm.length > 0,
    })

    const handleUserSelect = (user) => {
        setSearchedUser(user);
        setSearchTerm("");
    }

    const showList = searchTerm.length > 0

    return (
        <div className="w-full relative border rounded-md mb-3">
            <Command shouldFilter={false} className="bg-gradient-to-br">
                <CommandInput
                    placeholder="Search people..."
                    value={searchTerm}
                    onValueChange={setSearchTerm}
                    
                />
                <div className={cn("absolute top-full left-0 w-full bg-white border rounded-md mt-1 z-10", showList ? "block" : "hidden")}>
                    <CommandList>
                        <CommandEmpty>
                            {isLoading ?
                                <div className="p-2 text-center text-sm text-muted-foreground">Searching...</div>
                                :
                                "No users found."
                            }
                        </CommandEmpty>
                        <CommandGroup>
                            {!isLoading && users.length > 0 && users.map((user) => (
                                <CommandItem
                                    key={user.id}
                                    onSelect={() => {
                                        handleUserSelect(user);
                                    }}
                                >
                                    {user.name}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </div>
            </Command>
        </div>
    )
}

export default React.memo(ChatSearch);


