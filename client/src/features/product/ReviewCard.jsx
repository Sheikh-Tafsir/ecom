import React from "react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Star } from "lucide-react"
import { formatDate } from "@/utils"

const ReviewCard = ({review}) => {

  return (
    <Card className="w-full mb-4">
      <CardHeader className="pb-3 ">
        <div className="flex items-center gap-4">
          <Avatar className="h-10 w-10">
            <AvatarImage src={review?.userImage || "/placeholder.svg"} alt={review?.userName} />
            <AvatarFallback>
              {review?.userName}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <h3 className="font-semibold text-sm">{review?.userName}</h3>
            <div className="flex items-center gap-1 mt-1">
              {[...Array(5)].map((_, i) => (
                <Star
                  key={i}
                  className={`h-4 w-4 ${
                    i < review?.rating ? "fill-yellow-400 text-yellow-400" : "fill-gray-200 text-gray-200"
                  }`}
                />
              ))}
              <span className="text-xs text-muted-foreground ml-2">{formatDate(review?.createdAt)}</span>
            </div>
          </div>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <p className="text-muted-foreground leading-relaxed">{review?.comment}</p>
      </CardContent>
    </Card>
  )
}

export default React.memo(ReviewCard);