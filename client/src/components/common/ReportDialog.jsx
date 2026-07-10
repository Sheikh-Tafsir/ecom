import { useState } from "react";
import { Download } from "lucide-react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { APP_MODULE, TOAST_TYPE } from "@/utils/enums";
import { Axios } from "@/services/http/Axios";
import { notify } from "@/components/common/notification";
import InputError from "./InputError";
import { GLOBAL_ERROR, handleErrors } from "@/utils";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

const today = new Date();
today.setHours(23, 59, 59, 999);

const dateSchema = z.preprocess((arg) => {
    if (arg instanceof Date && isNaN(arg.getTime())) return null;
    return arg;
}, z.date().nullable().optional());

const ReportSchema = z.object({
    fromDate: dateSchema,
    toDate: dateSchema,
})
    .refine(
        (data) => !data.fromDate || data.fromDate <= today,
        {
            message: "From Date cannot be in the future",
            path: ["fromDate"],
        }
    )
    .refine(
        (data) => !data.toDate || data.toDate <= today,
        {
            message: "To Date cannot be in the future",
            path: ["toDate"],
        }
    )
    .refine(
        (data) =>
            !data.fromDate ||
            !data.toDate ||
            data.fromDate <= data.toDate,
        {
            message: "From Date cannot be greater than To Date",
            path: ["toDate"],
        }
    );

export function ReportDialog({ module = APP_MODULE.USER, trigger }) {
    const [open, setOpen] = useState(false);

    const {
        register,
        handleSubmit,
        setError,
        reset,
        formState: { errors, isSubmitting },
    } = useForm({
        resolver: zodResolver(ReportSchema),
        defaultValues: {
            fromDate: null,
            toDate: null,
        }
    });

    const handleDownload = async (data) => {
        try {
            const payload = {
                module,
                fromDate: data.fromDate ? data.fromDate.toISOString().split("T")[0] : null,
                toDate: data.toDate ? data.toDate.toISOString().split("T")[0] : null,
            };

            const response = await Axios.post("/reports", payload, {
                responseType: "blob",
            });

            const disposition = response.headers["content-disposition"];
            let filename = `report_${module.toLowerCase()}_${payload.fromDate || "start"}_to_${payload.toDate || "end"}.csv`;

            if (disposition && disposition.includes("filename=")) {
                const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                const matches = filenameRegex.exec(disposition);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, "");
                }
            }

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement("a");
            link.href = url;
            link.setAttribute("download", filename);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            reset();
            setOpen(false);
        } catch (error) {
            console.error("Report download failed:", error);

            if (error.response?.data instanceof Blob && error.response.data.type === "application/json") {
                const text = await error.response.data.text();
                const errorData = JSON.parse(text);
                // Wrap in a fake axios error structure so handleErrors can process it
                handleErrors({ response: { data: errorData } }, setError);
            }
            
            notify(TOAST_TYPE.ERROR, "Failed to download report");
        }
    };

    return (
        <Dialog open={open} onOpenChange={(val) => {
            setOpen(val);
            if (!val) reset();
        }}>
            <DialogTrigger asChild>
                {trigger || (
                    <Button variant="outline" size="sm" className="gap-2">
                        <Download className="h-4 w-4" />
                        Generate Report
                    </Button>
                )}
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>Generate {module} Report</DialogTitle>
                    <DialogDescription>
                        Select the date range for the report.
                    </DialogDescription>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                    <InputError errors={errors} field={GLOBAL_ERROR} />

                    <div className="grid gap-2">
                        <Label htmlFor="fromDate">From Date</Label>
                        <Input
                            id="fromDate"
                            type="date"
                            {...register("fromDate", {
                                valueAsDate: true,
                            })}
                        />
                        <InputError errors={errors} field={"fromDate"} />
                    </div>
                    <div className="grid gap-2">
                        <Label htmlFor="toDate">To Date</Label>
                        <Input
                            id="toDate"
                            type="date"
                            {...register("toDate", {
                                valueAsDate: true,
                            })}
                        />
                        <InputError errors={errors} field={"toDate"} />
                    </div>
                </div>
                <DialogFooter>
                    <Button onClick={handleSubmit(handleDownload)} disabled={isSubmitting}>
                        {isSubmitting ? "Generating..." : "Download CSV"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
