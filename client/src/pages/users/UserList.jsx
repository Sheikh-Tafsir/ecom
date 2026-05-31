import React, { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Pencil } from "lucide-react";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Axios } from "@/middleware/Axios";
import { AlertAction } from "@/mycomponents/AlertAction";
import PaginationButton from "@/mycomponents/PaginationButton";
import PaginationSearch from "@/mycomponents/PaginationSearch";
import PageLoadingOverlay from "@/mycomponents/pageLoadingOverlay/PageLoadingOverlay";
import { ToastAlert } from "@/mycomponents/ToastAlert";
import { formatDateString } from "@/utils/DateUtils";
import { handleErrors } from "@/utils/ErrorUtils";
import { ALERT_TYPE, TOAST_TYPE, USER_ROLE, USER_STATUS } from "@/utils/enums";
import { FIRST_PAGE } from "@/utils/PaginationUtils.js";
import { initialToastState } from "@/utils/Utils";

const UserList = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const queryParams = Object.fromEntries(searchParams.entries());
  const role = searchParams.get("role") || "";
  const status = searchParams.get("status") || "";

  const [toastData, setToastData] = useState(initialToastState);
  const [selectedUser, setSelectedUser] = useState(null);

  const fetchUsers = async ({ queryKey }) => {
    const [_key, params] = queryKey;
    const response = await Axios.get("/users", { queryParams });
    return response.data.data;
  };

  const {
    data,
    isFetching: isPageLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["users", queryParams],
    queryFn: fetchUsers,
    keepPreviousData: true,
  });

  const users = data?.content || [];
  const totalPages = data?.totalPages || FIRST_PAGE;

  if (isError) {
    console.error(error);
    showToast("Failed to load users", TOAST_TYPE.ERROR);
  }

  const handleEditUser = (user) => {
    navigate(`/users/${user.id}`, { state: { user } });
  };

  const changeUserStatus = async (id, status) => {
    try {
      await Axios.put(`/users/${id}`, { status });
      showToast(`User ${status}`, TOAST_TYPE.SUCCESS);
      refetch(); // Refresh list after update
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

  const handleRoleChange = (value) => {
    const params = new URLSearchParams(searchParams);
    if (value) params.set("role", value);
    else params.delete("role");
    params.set("page", FIRST_PAGE);
    setSearchParams(params);
  };

  const handleStatusChange = (value) => {
    const params = new URLSearchParams(searchParams);
    if (value) params.set("status", value);
    else params.delete("status");
    params.set("page", FIRST_PAGE);
    setSearchParams(params);
  };

  const showToast = (message, type) => {
    setToastData({ message, type, id: Date.now() });
  };

  return (
    <>
      {isPageLoading && <PageLoadingOverlay />}
      <div className="container min-h-[90vh] pt-2">
        <h1 className="text-center text-2xl lg:text-2xl xl:text-3xl mb-[10px]">User List</h1>

        <PaginationSearch moduleName="Users" />

        <Table className="cursor-pointer bg-white w-full">
          <TableHeader>
            <TableRow className="bg-blue-100 hover:bg-blue-200 transition-colors duration-200">
              <TableHead className="text-black text-base">Name</TableHead>
              <TableHead className="text-black text-base">Email</TableHead>
              <TableHead className="text-black text-base">
                <Select onValueChange={handleRoleChange} value={role}>
                  <SelectTrigger className="my-auto w-[150px]">
                    <SelectValue placeholder="Role" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={null}>All</SelectItem>
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
                <Select onValueChange={handleStatusChange} value={status}>
                  <SelectTrigger className="my-auto">
                    <SelectValue placeholder="Status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={null}>All</SelectItem>
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
              users.map((item) => (
                <TableRow key={item.id} onClick={() => setSelectedUser(item)}>
                  <TableCell>{item.name}</TableCell>
                  <TableCell>{item.email}</TableCell>
                  <TableCell
                    className={
                      item.role === USER_ROLE.ADMIN || item.role === USER_ROLE.SUPER_ADMIN
                        ? "text-blue-600"
                        : item.role === USER_ROLE.CARE_GIVER
                          ? "text-green-600"
                          : item.role === USER_ROLE.PATIENT
                            ? "text-red-600"
                            : "text-gray-700"
                    }
                  >
                    {item.role}
                  </TableCell>
                  <TableCell>{formatDateString(item.createdAt)}</TableCell>
                  <TableCell
                    className={
                      item.status === USER_STATUS.ACTIVE ? "text-green-600" : "text-red-600"
                    }
                  >
                    {item.status}
                  </TableCell>
                  <TableCell className="flex gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      className="hover:bg-blue-600 text-blue-600 hover:text-white"
                      onClick={() => handleEditUser(item)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <AlertAction onConfirm={deleteUser} type={ALERT_TYPE.DELETE} icon={true} />
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

        <PaginationButton totalPages={totalPages} />
      </div>
      {toastData.message && (
        <ToastAlert key={toastData.id} message={toastData.message} type={toastData.type} />
      )}
    </>
  );
};

export default UserList;
