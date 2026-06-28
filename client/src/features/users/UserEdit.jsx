import React, {useState, useEffect} from 'react';
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
import {Input} from '@/components/ui/input.jsx';
import {Label} from '@/components/ui/label.jsx';
import {Axios} from '@/services/http/Axios';
import {AlertAction} from '@/components/common/AlertAction';
import {ButtonLoading} from "@/components/common/ButtonLoading";
import ImageInput from '@/components/common/ImageInput';
import InputViewMode from '@/components/common/InputViewMode';
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import StaredLabel from '@/components/common/StaredLabel';
import {TOAST_TYPE, ALERT_TYPE} from '@/utils/enums';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import InputError from "@/components/common/InputError.jsx";
import { notify } from '@/components/common/notification';

// Zod schema
const UserSchema = z.object({
    name: z.string()
        .min(2, "Name is required")
        .max(31, "Name must be shorter than 31 characters"),
    image: z.any().optional(),
});

const UserEdit = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const isEditable = location.pathname.endsWith("/edit");

    const queryClient = useQueryClient();
    const {register, handleSubmit, control, watch, reset, setError, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(UserSchema),
        defaultValues: {},
    });

    const {
        data: user,
        isFetching: isPageLoading
    } = useQuery({
        queryKey: ["user", id],
        queryFn: async () => {
            const res = await Axios.get(`/users/${id}`);
            return res.data.data;
        },
        enabled: !!id,
    });

    useEffect(() => {
        if (user) reset(user);
    }, [reset, user]);

    const updateUser = useMutation({
        mutationFn: async (data) => {
            const formData = prepareMultipartForm(data);
            await Axios.put(`/users/${id}`, formData);
        },
        onSuccess: async () => {
            notify(TOAST_TYPE.SUCCESS, "User successfully updated")
            await queryClient.invalidateQueries({queryKey: ["user", id]});
            navigate(`/user/${id}`);
        },
        onError: (error) => {
            console.error(error);
            handleErrors(error, setError);
            notify(TOAST_TYPE.ERROR, "Failed to update user")
        },
    });

    // ✅ Delete
    const deleteUser = useMutation({
        mutationFn: async () => await Axios.delete(`/users/${id}`),
        onSuccess: async () => {
            notify(TOAST_TYPE.SUCCESS, "User deleted")
            await queryClient.invalidateQueries({queryKey: ["users"]});
            navigate("/users", {replace: true});
        },
        onError: (error) => {
            console.error(error);
            notify(TOAST_TYPE.ERROR, "Failed to delete user")
        },
    })

    const handleNavigateToEdit = () => {
        navigate(`edit`);
    }

    return (
        <>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="min-h-[90vh] flex">
                <Card className="mx-auto my-auto w-[450px]">
                    <form onSubmit={handleSubmit((data) => updateUser.mutate(data))}>
                        <fieldset disabled={!isEditable}>
                            <CardHeader>
                                <CardTitle>Profile</CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-4">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>

                                <Controller
                                    name="image"
                                    control={control}
                                    render={({field}) => (
                                        <ImageInput
                                            existingImage={field.value}
                                            onImageChange={field.onChange}
                                            error={errors.image}
                                        />
                                    )}
                                />

                                <div className="space-y-1">
                                    <Label>Email</Label>
                                    <InputViewMode value={watch("email")} isEditable={isEditable}/>
                                </div>

                                <div className="space-y-1">
                                    <StaredLabel label="Name"/>
                                    <Input {...register("name")} placeholder="Md Rafiquddin"/>
                                    {errors.name && <p className="validation-error">{errors.name.message}</p>}
                                </div>

                                <div className="space-y-1">
                                    <Label>Role</Label>
                                    <InputViewMode value={watch("role") || user?.role} isEditable={isEditable}/>
                                </div>
                            </CardContent>
                        </fieldset>

                        <CardFooter className="flex-col gap-2">
                            {isSubmitting ? (
                                <ButtonLoading/>
                            ) : !isEditable ? (
                                <div className="w-full flex gap-2">
                                    <Button type="button" className="w-[50%] bg-gray-600 hover:bg-gray-800"
                                            onClick={handleNavigateToEdit}>
                                        Edit
                                    </Button>
                                    <AlertAction onConfirm={() => deleteUser.mutate()} type={ALERT_TYPE.DELETE}
                                                 css="w-[50%]"/>
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
        </>
    );
};

export default UserEdit;