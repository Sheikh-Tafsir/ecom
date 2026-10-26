import {cn} from "@/lib/utils";

const VARIANT_STYLES = {
    emerald: "bg-emerald-100 text-emerald-700",
    emeraldDark: "bg-emerald-200 text-emerald-800",
    amber: "bg-amber-100 text-amber-700",
    red: "bg-red-100 text-red-700",
    blue: "bg-blue-100 text-blue-700",
    blueSoft: "bg-blue-50 text-blue-700",
    indigo: "bg-indigo-100 text-indigo-700",
    orange: "bg-orange-100 text-orange-700",
    slate: "bg-slate-100 text-slate-500",
    slateDark: "bg-slate-200 text-slate-600",
};

/**
 * A reusable pill-shaped status badge.
 *
 * @param {Object} props
 * @param {"emerald"|"emeraldDark"|"amber"|"red"|"blue"|"blueSoft"|"indigo"|"orange"|"slate"|"slateDark"} props.variant - Color variant
 * @param {string} [props.className] - Additional classes
 * @param {React.ReactNode} props.children
 */
const StatusBadge = ({variant = "slate", className, children, ...rest}) => (
    <span
        className={cn(
            "inline-flex items-center px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest",
            VARIANT_STYLES[variant] || VARIANT_STYLES.slate,
            className
        )}
        {...rest}
    >
        {children}
    </span>
);

export default StatusBadge;
