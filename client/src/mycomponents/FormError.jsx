import {getError} from "@/utils/ErrorUtils.js";

const FormError = ({errors, field}) => {
    return (
        <>
            <p className="validation-error">
                {getError(errors, field)}
            </p>
        </>
    )
}

export default FormError