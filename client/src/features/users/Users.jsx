import {useEffect, useMemo, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {keepPreviousData, useQuery} from "@tanstack/react-query";
import {Pencil, User as UserIcon, Shield, Filter} from "lucide-react";

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
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card';
import {Label} from '@/components/ui/label';
import {Axios} from "@/services/http/Axios";
import {AlertAction} from "@/components/common/AlertAction";
import PaginationButton from "@/components/common/PaginationButton";
import PaginationSearch from "@/components/common/PaginationSearch";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import {formatDate} from "@/utils/DateUtils";
import {ALERT_TYPE, APP_MODULE, PERMISSION, ROLE_PREFIX, TOAST_TYPE, USER_ROLE, USER_STATUS} from "@/utils/enums";
import {
    FIRST_PAGE,
    checkAllSelected,
    redirectWhenInvalidPage,
    updateQueryWhenParamChange,
    getSelectValue,
    normalizeQuery
} from "@/utils/PaginationUtils.js";
import {hasPermission} from "@/utils";
import {toastify} from "@/common/toastify.js";
import { cn } from "@/lib/utils";

import {ReportDialog} from "@/components/common/ReportDialog";
import {useUserStore} from "@/store/useUserStore";

const ALLOWED_SORT_FIELDS = new Set([
    "createdAt",
    "name",
])

const fetchUsers = async ({queryKey}) => {
    const [, params] = queryKey

    const response = await Axios.get("/users", {
        params: {
            page: params.page - 1,
            sort: params.sort,
            size: params.size,
            name: params.search || undefined,
            role: checkAllSelected(params.role),
            status: checkAllSelected(params.status),
        },
    })

    return response.data.data
};

const Users = () => {
    const navigate = useNavigate();
    const {user: currentUser} = useUserStore();

    const [searchParams] = useSearchParams();
    const queryParams = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams])

    const filters = useMemo(
        () => ({
            ...normalizeQuery(queryParams, ALLOWED_SORT_FIELDS),
            role: queryParams.role || "",
            status: queryParams.status || "",
            search: queryParams.search || "",
        }),
        [queryParams]
    );
    const {page, role, status} = filters
    const [selectedUser, setSelectedUser] = useState(null);

    const {
        data, isPending: isPageLoading, isError, error, refetch,
    } = useQuery({
        queryKey: ["users", filters],
        queryFn: fetchUsers,
        placeholderData: keepPreviousData,
    });

    const users = data?.content || [];
    const totalPages = data?.totalPages || FIRST_PAGE;

    const handleEditUser = (user) => {
        navigate(`/users/${user.id}`, {state: {user}});
    };

    const changeUserStatus = async (id, status) => {
        try {
            await Axios.put(`/users/${id}`, {status});
            toastify(TOAST_TYPE.SUCCESS, `User status updated to ${status}`)
            await refetch();
        } catch (error) {
            console.error("Error changing user status:", error);
            toastify(TOAST_TYPE.ERROR, "Failed to change user status")
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

    useEffect(() => {
        if (isError) {
            console.error(error);
            toastify(TOAST_TYPE.ERROR, "Failed to load users");
        }
    }, [error, isError]);

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-5 md:py-8">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">User Management</h1>
                        <p className="text-slate-500 font-medium">Oversee and manage system users and their permissions</p>
                    </div>
                    {(hasPermission(currentUser, PERMISSION.ADMIN_ACCESS) || hasPermission(currentUser, PERMISSION.SUPER_ADMIN_ACCESS)) && <ReportDialog module={APP_MODULE.USER} />}
                </div>

                <PaginationSearch moduleName="Users" />
                
                <div className='grid lg:grid-cols-4 gap-10 items-start'>
                    {/* Filter Sidebar */}
                    <Card className='lg:col-span-1 border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden sticky top-24'>
                        <CardHeader className="bg-slate-100 border-b border-slate-100 pb-4">
                            <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                <span className="p-1.5 bg-blue-600 rounded-lg text-white">
                                    <Filter className="w-4 h-4" />
                                </span>
                                Filter Users
                            </CardTitle>
                        </CardHeader>

                        <CardContent className="space-y-6 pt-6">
                            {/* Role Filter */}
                            <div className="space-y-2">
                                <Label className="text-base font-semibold uppercase tracking-widest ml-1">Role</Label>
                                <Select
                                    value={getSelectValue(role)}
                                    onValueChange={(val) => updateQuery({role: val})}
                                >
                                    <SelectTrigger className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all">
                                        <SelectValue placeholder="Select Role"/>
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="__all__">All Roles</SelectItem>
                                        {Object.entries(USER_ROLE).map(([key, value]) => (
                                            <SelectItem key={key} value={value}>
                                                {key}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            {/* Status Filter */}
                            <div className="space-y-2">
                                <Label className="text-base font-semibold uppercase tracking-widest ml-1">Status</Label>
                                <Select
                                    value={getSelectValue(status)}
                                    onValueChange={(val) => updateQuery({status: val})}
                                >
                                    <SelectTrigger className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all">
                                        <SelectValue placeholder="Select Status"/>
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="__all__">All Statuses</SelectItem>
                                        {Object.entries(USER_STATUS).map(([key, value]) => (
                                            <SelectItem key={key} value={value}>
                                                {key}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </CardContent>
                    </Card>

                    <div className='lg:col-span-3 space-y-6'>
                        <div className="bg-white rounded-lg border border-slate-100 shadow-sm overflow-hidden">
                            <Table className="bg-white">
                                <TableHeader>
                                    <TableRow className="bg-slate-100 border-b border-slate-100 hover:bg-slate-50/50 transition-none">
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">User</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Roles</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4">Joined At</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-center">Status</TableHead>
                                        <TableHead className="text-md font-semibold uppercase tracking-widest px-6 py-4 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {users.length > 0 ?
                                        users.map((user) => (
                                            <TableRow key={user.id} className="group hover:bg-slate-50/50 border-b border-slate-50 transition-colors" onClick={() => setSelectedUser(user)}>
                                                <TableCell className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-xs font-black text-blue-600">
                                                            {user.name?.slice(0, 1)}
                                                        </div>
                                                        <div className="flex flex-col">
                                                            <span className="font-bold text-slate-700">{user.name}</span>
                                                            <span className="text-xs text-slate-400 font-medium">{user.email}</span>
                                                        </div>
                                                    </div>
                                                </TableCell>
                                                <TableCell className="px-6 py-4">
                                                    <div className="flex flex-wrap gap-1">
                                                        {user.roles?.map(role => (
                                                            <span key={role} className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600 uppercase tracking-tighter">
                                                                {role.replace(ROLE_PREFIX, "")}
                                                            </span>
                                                        ))}
                                                    </div>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-slate-500 font-semibold text-xs">
                                                    {formatDate(user.createdAt)}
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-center">
                                                    <span className={cn(
                                                        "inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-widest",
                                                        user.status == USER_STATUS.ACTIVE ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"
                                                    )}>
                                                        {user.status}
                                                    </span>
                                                </TableCell>
                                                <TableCell className="px-6 py-4 text-right">
                                                    <div className="flex justify-end gap-2">
                                                        <Button
                                                            variant="outline"
                                                            size="icon"
                                                            className="h-9 w-9 rounded-lg font-bold text-blue-600 hover:bg-blue-50 border-slate-200"
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                handleEditUser(user);
                                                            }}
                                                        >
                                                            <Pencil className="h-4 w-4"/>
                                                        </Button>
                                                        <div onClick={(e) => e.stopPropagation()}>
                                                            <AlertAction onConfirm={deleteUser} type={ALERT_TYPE.DELETE} icon={true}/>
                                                        </div>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ))
                                        :
                                        <TableRow>
                                            <TableCell colSpan={5} className="py-20 text-center">
                                                <div className="flex flex-col items-center gap-2 opacity-40">
                                                    <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-2">
                                                        <UserIcon className="w-6 h-6 text-slate-400" />
                                                    </div>
                                                    <p className="text-sm font-black uppercase tracking-widest">No users found</p>
                                                    <p className="text-xs font-medium">Try adjusting your filters</p>
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    }
                                </TableBody>
                            </Table>
                        </div>
                        
                        <PaginationButton totalPages={totalPages}/>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Users;
