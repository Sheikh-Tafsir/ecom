import React, {useEffect} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import {useForm, Controller} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import {z} from 'zod';

import {Button} from '@/components/ui/button.jsx';
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Label} from '@/components/ui/label.jsx';
import {Axios} from '@/services/http/Axios';
import {AlertAction} from '@/components/common/AlertAction';
import {ButtonLoading} from "@/components/common/ButtonLoading";
import InputViewMode from '@/components/common/InputViewMode';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import {TOAST_TYPE, ALERT_TYPE, ROLE_PREFIX, PERMISSION} from '@/utils/enums';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import InputError from "@/components/common/InputError.jsx";
import {toastify} from '@/common/toastify.js';
import {hasPermission} from "@/utils/index.js";
import {useUserStore} from "@/store/useUserStore.js";
import {MultiSelect} from "@/components/common/MultiSelect.jsx";

// Zod schema
const UserSchema = z.object({
    name: z.string().optional(),
    roleNames: z.array(z.string()).min(1, "At least one role is required"),
});

const fetchUser = async (id) => {
    const response = await Axios.get(`/users/${id}`)
    return response.data.data
}

const fetchRoles = async () => {
    const response = await Axios.get(`/roles`)
    return response.data.data
}

const UserEdit = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const isEditable = location.pathname.endsWith("/edit");
    const currentUser = useUserStore(state => state.user);
    const canManageRoles = hasPermission(currentUser, PERMISSION.SUPER_ADMIN_ACCESS);

    const queryClient = useQueryClient();
    const {
        handleSubmit, 
        control, 
        reset, 
        setError, 
        formState: {errors, isSubmitting}} 
    = useForm({
        resolver: zodResolver(UserSchema),
        defaultValues: {
            name: '',
            roleNames: []
        },
    });

    const {
        data: user,
        isFetching: isPageLoading
    } = useQuery({
        queryKey: ["user", id],
        queryFn: () => fetchUser(id),
        enabled: !!id,
    });

    const {data: roles} = useQuery({
        queryKey: ["roles"],
        queryFn: fetchRoles,
        enabled: canManageRoles && isEditable,
    });

    useEffect(() => {
        if (user) {
            reset({
                ...user,
                roleNames: user.roles || []
            });
        }
    }, [reset, user]);

    const updateUser = useMutation({
        mutationFn: async (data) => {
            await Axios.put(`/users/${id}`, {roles: data.roleNames});
        },
        onSuccess: async () => {
            toastify(TOAST_TYPE.SUCCESS, "User successfully updated")
            await queryClient.invalidateQueries({queryKey: ["user", id]});
            navigate(`/users/${id}`);
        },
        onError: (error) => {
            console.error(error);
            handleErrors(error, setError);
            toastify(TOAST_TYPE.ERROR, "Failed to update user")
        },
    });

    const deleteUser = useMutation({
        mutationFn: async () => await Axios.delete(`/users/${id}`),
        onSuccess: async () => {
            toastify(TOAST_TYPE.SUCCESS, "User deleted")
            await Promise.all([
                queryClient.invalidateQueries({queryKey: ["user", id]}),
                queryClient.invalidateQueries({queryKey: ["users"]}),
            ]);
            navigate("/users", {replace: true});
        },
        onError: (error) => {
            console.error(error);
            toastify(TOAST_TYPE.ERROR, "Failed to delete user")
        },
    })

    const handleNavigateToEdit = () => {
        navigate(`edit`);
    }

    return (
        <div className="min-h-[90vh] flex items-center justify-center">
            {isPageLoading && <PageLoadingOverlay/>}
            <Card className="mx-auto my-auto w-[450px]">
                <form onSubmit={handleSubmit((data) => updateUser.mutate(data))}>
                    <fieldset disabled={!isEditable}>
                        <CardHeader>
                            <CardTitle>User Details</CardTitle>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                            <div className="flex justify-center mb-6">
                                <div className="relative">
                                    <img
                                        src={user?.image || "/vite.svg"}
                                        alt={user?.name || "User"}
                                        className="w-24 h-24 rounded-full object-cover border-2 border-gray-200"
                                    />
                                </div>
                            </div>

                            <div className="space-y-1">
                                <Label>Email</Label>
                                <InputViewMode value={user?.email || "N/A"} isEditable={false}/>
                            </div>

                            <div className="space-y-1">
                                <Label>Name</Label>
                                <InputViewMode value={user?.name || "N/A"} isEditable={false}/>
                            </div>

                            <div className="space-y-1">
                                <Label>Roles</Label>
                                {isEditable && canManageRoles ? (
                                    <div className="mt-2">
                                        <Controller
                                            name="roleNames"
                                            control={control}
                                            render={({field}) => (
                                                <MultiSelect
                                                    options={roles?.map(role => ({
                                                        label: role.name?.replace(ROLE_PREFIX, ""),
                                                        value: role.name
                                                    })) || []}
                                                    selected={field.value || []}
                                                    onChange={field.onChange}
                                                    placeholder="Select roles..."
                                                />
                                            )}
                                        />
                                    </div>
                                ) : (
                                    <InputViewMode
                                        value={user?.roles?.map(role => role.replace(ROLE_PREFIX, "")).join(", ") ?? ""}
                                        isEditable={false}
                                    />
                                )}
                            </div>
                        </CardContent>
                    </fieldset>

                    <CardFooter className="flex-col gap-2">
                        {isSubmitting ? (
                            <ButtonLoading/>
                        ) : !isEditable ? (
                            <div className="w-full flex gap-2">
                                {canManageRoles && (
                                    <Button type="button" className="w-[70%] bg-blue-600 hover:bg-blue-700"
                                            onClick={handleNavigateToEdit}>
                                        Edit
                                    </Button>
                                )}
                                <AlertAction onConfirm={() => deleteUser.mutate()} type={ALERT_TYPE.DELETE}
                                             css={canManageRoles ? "w-[30%]" : "w-full"}/>
                            </div>
                        ) : (
                            <Button type="submit" className="w-full bg-blue-600">
                                Save
                            </Button>
                        )}
                    </CardFooter>
                </form>
            </Card>
        </div>
    );
};


export default UserEdit;