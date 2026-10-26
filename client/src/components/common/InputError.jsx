import {getError} from "@/utils/ErrorUtils";

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