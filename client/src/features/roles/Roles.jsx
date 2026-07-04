import {useQuery, useMutation, useQueryClient} from "@tanstack/react-query";
import {Pencil, Plus, Trash2} from "lucide-react";
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
import {notify} from "@/components/common/notification";
import {AlertAction} from "@/components/common/AlertAction";

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
            notify(TOAST_TYPE.SUCCESS, "Role deleted successfully");
            queryClient.invalidateQueries({queryKey: ["roles"]});
        },
        onError: () => {
            notify(TOAST_TYPE.ERROR, "Failed to delete role");
        }
    });

    return (
        <>
            {isLoading && <PageLoadingOverlay/>}
            <div className="container min-h-[90vh] pt-6">
                <div className="flex justify-between items-center mb-6">
                    <h1 className="text-2xl font-bold">Role Management</h1>
                    <Button onClick={() => navigate("/roles/create")} className="bg-blue-600">
                        <Plus className="mr-2 h-4 w-4"/> Add Role
                    </Button>
                </div>

                <Table className="bg-white shadow-sm border rounded-lg">
                    <TableHeader>
                        <TableRow className="bg-gray-50">
                            <TableHead>Role Name</TableHead>
                            <TableHead>Permissions</TableHead>
                            <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {roles?.map((role) => (
                            <TableRow key={role.id}>
                                <TableCell className="font-medium">
                                    {role.name?.replace(ROLE_PREFIX, "")}
                                </TableCell>
                                <TableCell>
                                    <div className="flex flex-wrap gap-1">
                                        {role.permissions?.map(p => (
                                            <span key={p} className="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded">
                                                {p}
                                            </span>
                                        ))}
                                    </div>
                                </TableCell>
                                <TableCell className="text-right">
                                    <div className="flex justify-end gap-2">
                                        <Button
                                            variant="outline"
                                            size="icon"
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
                                <TableCell colSpan={3} className="text-center py-10 text-gray-500">
                                    No roles defined yet.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
        </>
    );
};

export default Roles;
