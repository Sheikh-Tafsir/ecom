import {Loader2} from "lucide-react"

import {Button} from "@/components/ui/button"

export function ButtonLoading({css}) {
    return (
        <Button disabled className={!css ? 'w-[100%]' : css}>
            <Loader2 className="animate-spin"/>
            Please wait
        </Button>
    )
}