import {Label} from "@/components/ui/label";
import InputError from "@/components/common/InputError";
import {cn} from "@/lib/utils";

const FormField = ({
    label,
    field,
    errors,
    required = false,
    className,
    labelClassName,
    htmlFor,
    helperText,
    children,
}) => {
    const id = htmlFor ?? field;

    return (
        <div className={cn("space-y-2", className)}>
            <Label htmlFor={id} className={labelClassName}>
                {label}
                {required && <span className="text-red-600 ml-0.5">*</span>}
            </Label>
            {children}
            {helperText && (
                <p className="text-[10px] text-slate-400 font-bold uppercase">
                    {helperText}
                </p>
            )}
            <InputError errors={errors} field={field} />
        </div>
    );
};

export default FormField;
