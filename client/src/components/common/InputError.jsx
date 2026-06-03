import {getError} from "@/utils/ErrorUtils.js";

const InputError = ({errors, field}) => {
    return (
        <>
            <p className="validation-error">
                {getError(errors, field)}
            </p>
        </>
    )
}

export default InputError