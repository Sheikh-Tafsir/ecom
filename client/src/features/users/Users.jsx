import {useEffect, useMemo, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {Pencil} from "lucide-react";

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {Button} from "@/components/ui/button";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {Axios} from "@/services/http/Axios";
import {AlertAction} from "@/components/common/AlertAction";
import PaginationButton from "@/components/common/PaginationButton";
import PaginationSearch from "@/components/common/PaginationSearch";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import {ToastAlert} from "@/components/common/ToastAlert";
import {formatDate} from "@/utils/DateUtils";
import {handleErrors} from "@/utils/ErrorUtils";
import {ALERT_TYPE, TOAST_TYPE, USER_ROLE, USER_STATUS} from "@/utils/enums";
import {
    FIRST_PAGE, checkAllSelected,
    redirectWhenInvalidPage, updateQueryWhenParamChange, getSelectValue
} from "@/utils/PaginationUtils.js";
import {isAdmin, toastInitialState} from "@/utils";
import {normalizeQuery} from "@/features/users/UserPaginationUtils.js";


const Users = () => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])
    const filters = useMemo(() => normalizeQuery(queryParams), [queryParams])
    const {page, role, status} = filters

    const [toastData, setToastData] = useState(toastInitialState);
    const [selectedUser, setSelectedUser] = useState(null);

    const fetchUsers = async ({queryKey}) => {
        const [, params] = queryKey

        const response = await Axios.get("/users", {
            params: {
                page: params.page - 1, // already normalized (safe)
                sort: params.sort,
                size: params.size,
                name: params.search || undefined,
                role: checkAllSelected(params.role),
                status: checkAllSelected(params.status),
            },
        })

        return response.data.data
    };

    const {
        data, isFetching: isPageLoading, isError, error, refetch,
    } = useQuery({
        queryKey: ["users", filters],
        queryFn: fetchUsers,
        keepPreviousData: true,
    });

    const users = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    useEffect(() => {
        if (isError) {
            console.error(error);
            showToast("Failed to load users", TOAST_TYPE.ERROR);
        }
    }, [error, isError]);

    const handleEditUser = (user) => {
        navigate(`/users/${user.id}`, {state: {user}});
    };

    const changeUserStatus = async (id, status) => {
        try {
            await Axios.put(`/users/${id}`, {status});
            showToast(`User ${status}`, TOAST_TYPE.SUCCESS);
            await refetch(); // Refresh list after update
        } catch (error) {
            console.error("Error changing user status:", error);
            handleErrors(error, null, null);
            showToast("Failed to change user status", TOAST_TYPE.ERROR);
        }
    };

    const deleteUser = async () => {
        if (!selectedUser) return;
        await changeUserStatus(selectedUser.id, USER_STATUS.SUSPENDED);
    };

    const updateQuery = (newParams) => {
        updateQueryWhenParamChange({queryParams, newParams, navigate})
    };

    // Page validation (safe)
    useEffect(() => {
        redirectWhenInvalidPage({page, totalPages, navigate, queryParams})
    }, [page, totalPages, navigate, queryParams])

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container min-h-[90vh] pt-6">
                <h1 className="text-center text-2xl lg:text-2xl xl:text-3xl mb-[10px] font-bold">Users</h1>

                <PaginationSearch moduleName="Users"/>

                <Table className="cursor-pointer bg-white w-full">
                    <TableHeader>
                        <TableRow className="bg-blue-100 hover:bg-blue-200 transition-colors duration-200">
                            <TableHead className="text-black text-base">Name</TableHead>
                            <TableHead className="text-black text-base">Email</TableHead>
                            <TableHead className="text-black text-base">
                                <Select
                                    value={getSelectValue(role)}
                                    onValueChange={(val) => updateQuery({role: val})}
                                >
                                    <SelectTrigger className="my-auto w-[150px]">
                                        <SelectValue placeholder="Role"/>
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="__all__">All</SelectItem>
                                        {Object.values(USER_ROLE).map((item) => (
                                            <SelectItem key={item} value={item}>
                                                {item}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </TableHead>
                            <TableHead className="text-black text-base">Created</TableHead>
                            <TableHead className="text-black text-base">
                                <Select
                                    value={getSelectValue(status)}
                                    onValueChange={(val) => updateQuery({status: val})}
                                >
                                    <SelectTrigger className="my-auto">
                                        <SelectValue placeholder="Status"/>
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="__all__">All</SelectItem>
                                        {Object.values(USER_STATUS).map((item) => (
                                            <SelectItem key={item} value={item}>
                                                {item}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </TableHead>
                            <TableHead className="text-black text-base">Actions</TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {users.length > 0 ? (
                            users.map((user) => (
                                <TableRow key={user.id} onClick={() => setSelectedUser(user)}>
                                    <TableCell>{user.name}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell
                                        className={
                                            isAdmin(user)
                                                ? "text-blue-600"
                                                : user.role === USER_ROLE.USER
                                                    ? "text-green-600"
                                                    : "text-gray-700"
                                        }
                                    >
                                        {user.roleValues.join(", ")}
                                    </TableCell>
                                    <TableCell>{formatDate(user.createdAt)}</TableCell>
                                    <TableCell
                                        className={
                                            user.status === USER_STATUS.ACTIVE ? "text-green-600" : "text-red-600"
                                        }
                                    >
                                        {user.status}
                                    </TableCell>
                                    <TableCell className="flex gap-2">
                                        <Button
                                            variant="outline"
                                            size="icon"
                                            className="hover:bg-blue-600 text-blue-600 hover:text-white"
                                            onClick={() => handleEditUser(user)}
                                        >
                                            <Pencil className="h-4 w-4"/>
                                        </Button>
                                        <AlertAction onConfirm={deleteUser} type={ALERT_TYPE.DELETE} icon={true}/>
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={6} className="text-center text-gray-500">
                                    No users found
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                <PaginationButton totalPages={totalPages}/>
            </div>

            <ToastAlert key={toastData.id} message={toastData.message} type={toastData.type}/>
        </>
    );
};

export default Users;
