import {Loader2} from "lucide-react"

import {Button} from "@/components/ui/button"

export function ButtonLoading({prop}) {
    const css = !prop ? '' : prop;

    return (
        <Button disabled className={css}>
            <Loader2 className="animate-spin"/>
            Please wait
        </Button>
    )
}