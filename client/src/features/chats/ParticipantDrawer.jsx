import React from "react"
import { X, UserRoundPlus } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { CHAT_MEMBER_TYPE, CHAT_TYPE } from "@/utils/enums"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"

const ParticipantDrawer = ({ isOpen, onClose, name, image, type, participants, handleUserSelectorDialogOpen }) => {
  return (
    <div className="relative">
      {isOpen && <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />}

      <div
        className={`fixed top-0 right-0 h-full w-80 bg-background border-l shadow-lg transform transition-transform duration-300 ease-in-out z-50 ${isOpen ? "translate-x-0" : "translate-x-full"
          }`}
      >
        <div className="flex flex-col h-full px-6 pt-4">
          <div className="flex justify-end">
            <Button variant="outline" size="icon" onClick={onClose}>
              <X />
            </Button>
          </div>

          <div className="flex flex-col items-center mb-6">
            <Avatar className="h-20 w-20 mb-4">
              <AvatarImage src={image} alt={name} />
              <AvatarFallback className="text-4xl">{name?.slice(0, 1)}</AvatarFallback>
            </Avatar>
            <p className="text-center">{name}</p>
          </div>

          <div className="flex-1">
            <div className="space-y-2">
              {type == CHAT_TYPE.GROUP ?
                <>
                  <p className="text-sm font-medium text-muted-foreground my-auto">Chat Members</p>
                  {participants && participants.map((participant) => (
                    <div className="flex py-2 justify-between" key={participant.id}>
                      <div className="flex">
                        <Avatar>
                          <AvatarImage src={participant.image} alt={participant.name} />
                          <AvatarFallback>{participant?.name?.slice(0, 1)}</AvatarFallback>
                        </Avatar>
                        <p className="my-auto ml-2">{participant?.name}</p>
                      </div>
                      <Badge className={participant.role != CHAT_MEMBER_TYPE.ADMIN ? 'bg-blue-500 h-8' : 'bg-red-500 h-8'}>{participant.role}</Badge>
                    </div>
                  ))}

                  <Button variant="ghost" onClick={() => handleUserSelectorDialogOpen(participants, null)} className="w-full justify-start p-0">
                    <UserRoundPlus />
                    Add People
                  </Button>
                </>
                :
                <p className="text-sm font-medium text-muted-foreground my-auto">Connected</p>
              }
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 border-t">
            <div className="text-xs text-muted-foreground text-center">Chat type: {type}</div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default React.memo(ParticipantDrawer)