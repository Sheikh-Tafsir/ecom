import React, { useEffect, useState } from "react"
import { Search, X } from "lucide-react"
import { useQuery } from '@tanstack/react-query';

import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Axios } from "@/services/http/Axios";
import { ButtonLoading } from "@/components/common/ButtonLoading";
import { REGULAR_ACTION } from "@/utils/enums";

const UserSelectorDialog = ({ isOpen, onClose, preSelecteedUserIds, avoidUserIds, confirmUsersSelection }) => {
    const [open, setOpen] = useState(isOpen);
    const [searchTerm, setSearchTerm] = useState("")
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [errorMessage, setErrorMessage] = useState("");
    const [isButtonLoading, setIsButtonLoading] = useState(false);

    const fetchUsers = async () => {
        const response = await Axios.get(`/users`, {
            params: {
                name: searchTerm,
                size: 50
            }
        });
        return response.data.data?.content || [];
    };

    const { data: users = [], isLoading } = useQuery({
        queryKey: ['searchUsers', searchTerm],
        queryFn: fetchUsers,
        enabled: open,
    })

    useEffect(() => {
        if (preSelecteedUserIds.length == 0 || users.length == 0) return;

        const usersToPreselect = users.filter((user) =>
            preSelecteedUserIds.includes(user.id) && !avoidUserIds?.includes(user.id)
        );
        setSelectedUsers(usersToPreselect);
    }, [preSelecteedUserIds, users]);

    const clearValues = () => {
        setOpen(false);
        setSearchTerm("");
        setSelectedUsers([]);
        setErrorMessage("");
        setIsButtonLoading(false);
    }

    useEffect(() => {
        setOpen(isOpen);

        if (!isOpen) {
            // Delay clearValues slightly to ensure the Dialog closes cleanly
            setTimeout(() => {
                clearValues();
            }, 100); // small delay helps avoid UI flicker
        }
    }, [isOpen]);

    const filteredUsers = users.filter(
        (user) => {
            const matchesSearch =
                user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.email.toLowerCase().includes(searchTerm.toLowerCase());

            const isAvoided = avoidUserIds?.includes(user.id);
            return matchesSearch && !isAvoided;
        }
    );

    // Handle user selection
    const handleUserToggle = (user) => {
        setSelectedUsers((prev) => {
            const isSelected = prev.some((u) => u.id == user.id)
            if (isSelected) {
                return prev.filter((u) => u.id !== user.id)
            } else {
                return [...prev, user]
            }
        })
    }

    // Remove selected user
    const removeSelectedUser = (userId) => {
        setSelectedUsers((prev) => prev.filter((u) => u.id !== userId))
    }

    // Clear all selections
    const clearAllSelections = () => {
        setSelectedUsers([])
    }

    // Handle confirm selection
    const handleConfirm = async () => {
        if (selectedUsers.length + avoidUserIds.length < 2) {
            setErrorMessage("Atleast choose 2 person");
            return;
        }
        setIsButtonLoading(true);

        const users = selectedUsers.map(user => ({
            id: user.id,
            name: user.name
        }));

        confirmUsersSelection(users, avoidUserIds.length == 0 ? REGULAR_ACTION.CREATE : REGULAR_ACTION.UPDATE);
    }

    const handleCancel = () => {
        onClose();
    }

    const handleOpenChange = (val) => {
        setOpen(val);
        if (!val) onClose();
    }

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Select Users</DialogTitle>
                    <DialogDescription>Search and select users from the list below.</DialogDescription>
                </DialogHeader>

                {isLoading && <div className="flex justify-center p-4">Loading...</div>}
                <div className="space-y-4">
                    {/* Search Input */}
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                            placeholder="Search users..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-10"
                        />
                    </div>
                    <p className="text-red-500 text-sm">{errorMessage}</p>

                    {/* Selected Users Display */}
                    {selectedUsers.length > 0 && (
                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <span className="text-sm font-medium">Selected ({selectedUsers.length})</span>
                                <Button variant="ghost" size="sm" onClick={clearAllSelections} className="h-auto p-1 text-xs">
                                    Clear all
                                </Button>
                            </div>
                            <div className="flex flex-wrap gap-1">
                                {selectedUsers.map((user) => (
                                    <Badge key={user.id} variant="secondary" className="gap-1">
                                        {user.name}
                                        <X
                                            className="h-3 w-3 cursor-pointer hover:text-destructive"
                                            onClick={() => removeSelectedUser(user.id)}
                                        />
                                    </Badge>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* User List */}
                    <div className="max-h-60 overflow-y-auto space-y-2">
                        {filteredUsers.length == 0 ? (
                            <div className="text-center py-4 text-muted-foreground">No users found</div>
                        ) : (
                            filteredUsers.map((user) => {
                                const isSelected = selectedUsers.some((u) => u.id == user.id)
                                return (
                                    <div
                                        key={user.id}
                                        className="flex items-center space-x-3 p-2 rounded-lg hover:bg-muted cursor-pointer"
                                        onClick={() => handleUserToggle(user)}
                                    >
                                        <Checkbox checked={isSelected} onCheckedChange={() => handleUserToggle(user)} />
                                        <Avatar className="h-8 w-8">
                                            <AvatarImage src={user.avatar || "/placeholder.svg"} alt={user.name} />
                                            <AvatarFallback>
                                                {user.name
                                                    .split(" ")
                                                    .map((n) => n[0])
                                                    .join("")}
                                            </AvatarFallback>
                                        </Avatar>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-sm font-medium truncate">{user.name}</p>
                                            <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                                        </div>
                                    </div>
                                )
                            })
                        )}
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => handleCancel()}>
                        Cancel
                    </Button>
                    {isButtonLoading ?
                        <ButtonLoading />
                        :
                        <Button onClick={handleConfirm} disabled={selectedUsers.length == 0}>
                            Confirm ({selectedUsers.length})
                        </Button>
                    }
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}

export default React.memo(UserSelectorDialog)