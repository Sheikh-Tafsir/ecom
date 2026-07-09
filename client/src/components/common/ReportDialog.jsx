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

export function ReportDialog({ module = APP_MODULE.USER, trigger }) {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        fromDate: "",
        toDate: "",
    });

    const handleChange = (name, value) => {
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const handleDownload = async () => {
        if (!module) {
            console.error("ReportDialog: module prop is missing!");
            notify(TOAST_TYPE.ERROR, "Internal error: module selection missing");
            return;
        }

        const today = new Date().toISOString().split("T")[0];

        if (form.fromDate > today) {
            notify(TOAST_TYPE.ERROR, "From Date cannot be in the future");
        }

        if (form.toDate > today) {
            notify(TOAST_TYPE.ERROR, "To Date cannot be in the future");
        }

        if (form.fromDate > form.toDate) {
            notify(TOAST_TYPE.ERROR, "From Date cannot be greater than To date");
            return;
        }

        setLoading(true);
        try {
            const response = await Axios.post("/reports", { ...form, module }, {
                responseType: "blob",
            });

            const disposition = response.headers["content-disposition"];
            let filename = `report_${module.toLowerCase()}_${form.fromDate}_to_${form.toDate}.csv`;

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
            
            setOpen(false);
            //notify(TOAST_TYPE.SUCCESS, "Report downloaded successfully");
        } catch (error) {
            console.error("Report download failed:", error);
            notify(TOAST_TYPE.ERROR, "Failed to download report");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
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
                    <div className="grid gap-2">
                        <Label htmlFor="fromDate">From Date</Label>
                        <Input
                            id="fromDate"
                            type="date"
                            value={form.fromDate}
                            onChange={(e) => handleChange("fromDate", e.target.value)}
                        />
                    </div>
                    <div className="grid gap-2">
                        <Label htmlFor="toDate">To Date</Label>
                        <Input
                            id="toDate"
                            type="date"
                            value={form.toDate}
                            onChange={(e) => handleChange("toDate", e.target.value)}
                        />
                    </div>
                </div>
                <DialogFooter>
                    <Button onClick={handleDownload} disabled={loading}>
                        {loading ? "Generating..." : "Download CSV"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
