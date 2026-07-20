import {useQuery, useMutation, useQueryClient} from "@tanstack/react-query";
import {Pencil, Plus, Trash2, Shield, ShieldCheck, ArrowRight} from "lucide-react";
import {useNavigate} from "react-router-dom";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {Button} from "@/components/ui/button";
import {Axios} from "@/services/http/Axios";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import {ROLE_PREFIX, TOAST_TYPE, ALERT_TYPE} from "@/utils/enums";
import {toastify} from "@/common/toastify.js";
import {AlertAction} from "@/components/common/AlertAction";
import { cn } from "@/lib/utils";

const fetchRoles = async () => {
    const response = await Axios.get("/roles");
    return response.data.data;
};

const Roles = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: roles, isPending: isLoading} = useQuery({
        queryKey: ["roles"],
        queryFn: fetchRoles,
    });

    const deleteRole = useMutation({
        mutationFn: async (id) => {
            await Axios.delete(`/roles/${id}`);
        },
        onSuccess: () => {
            toastify(TOAST_TYPE.SUCCESS, "Role deleted successfully");
            queryClient.invalidateQueries({queryKey: ["roles"]});
        },
        onError: () => {
            toastify(TOAST_TYPE.ERROR, "Failed to delete role");
        }
    });

    return (
        <div className="bg-slate-50 min-h-screen">
            {isLoading && <PageLoadingOverlay/>}
            
            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Role Management</h1>
                        <p className="text-slate-500 font-medium">Define and configure system roles and their associated permissions</p>
                    </div>
                    <Button 
                        onClick={() => navigate("/roles/create")} 
                        className="bg-blue-600 hover:bg-blue-700 text-white font-bold h-12 px-6 rounded-lg shadow-lg shadow-blue-200 transition-all active:scale-95 gap-2"
                    >
                        <Plus className="h-5 w-5"/> Add New Role
                    </Button>
                </div>

                <div className="bg-white rounded-lg border border-slate-100 shadow-sm overflow-hidden mb-10">
                    <Table className="bg-white">
                        <TableHeader>
                            <TableRow className="bg-slate-100 border-b border-slate-100 hover:bg-slate-50/50 transition-none">
                                <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Role Name</TableHead>
                                <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Permissions</TableHead>
                                <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {roles?.map((role) => (
                                <TableRow key={role.id} className="group hover:bg-slate-50/50 border-b border-slate-50 transition-colors">
                                    <TableCell className="px-6 py-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                                                <Shield className="w-5 h-5 text-blue-600" />
                                            </div>
                                            <span className="font-bold text-slate-700">
                                                {role.name?.replace(ROLE_PREFIX, "")}
                                            </span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="px-6 py-4">
                                        <div className="flex flex-wrap gap-1.5">
                                            {role.permissions?.map(p => (
                                                <span key={p} className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 uppercase tracking-tighter border border-emerald-100">
                                                    {p}
                                                </span>
                                            ))}
                                        </div>
                                    </TableCell>
                                    <TableCell className="px-6 py-4 text-right">
                                        <div className="flex justify-end gap-2">
                                            <Button
                                                variant="outline"
                                                size="icon"
                                                className="h-9 w-9 rounded-lg font-bold text-blue-600 hover:bg-blue-50 border-slate-200"
                                                onClick={() => navigate(`/roles/${role.id}/edit`, {state: {role}})}
                                            >
                                                <Pencil className="h-4 w-4"/>
                                            </Button>
                                            <AlertAction
                                                onConfirm={() => deleteRole.mutate(role.id)}
                                                type={ALERT_TYPE.DELETE}
                                                icon={true}
                                            />
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                            {roles?.length == 0 && (
                                <TableRow>
                                    <TableCell colSpan={3} className="py-20 text-center">
                                        <div className="flex flex-col items-center gap-2 opacity-40">
                                            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                <ShieldCheck className="w-6 h-6 text-slate-400" />
                                            </div>
                                            <p className="text-sm font-black uppercase tracking-widest">No roles defined yet</p>
                                            <p className="text-xs font-medium">Start by adding a new role to the system</p>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
            </div>
        </div>
    );
};

export default Roles;
