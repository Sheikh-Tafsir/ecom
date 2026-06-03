import React, {useEffect, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {useForm, Controller} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {useQuery, useMutation, useQueryClient} from "@tanstack/react-query";
import {z} from "zod";

import {Button} from "@/components/ui/button.jsx";
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card.jsx";
import {Input} from "@/components/ui/input.jsx";
import {Label} from "@/components/ui/label.jsx";

import {Axios} from "@/services/http/Axios";
import {AlertAction} from "@/components/common/AlertAction";
import {ButtonLoading} from "@/components/common/ButtonLoading";
import ImageInput from "@/components/common/ImageInput";
import InputViewMode from "@/components/common/InputViewMode";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay";
import StaredLabel from "@/components/common/StaredLabel";
import {ToastAlert} from "@/components/common/ToastAlert";

import {useUserStore} from "@/store/useUserStore";
import {handleErrors} from "@/utils/ErrorUtils";
import {TOAST_TYPE, ALERT_TYPE} from "@/utils/enums";
import {initialToastState, prepareMultipartForm} from "@/utils";
import InputError from "@/components/common/InputError.jsx";

const ProfileSchema = z.object({
    name: z.string()
        .min(2, "Name is required")
        .max(31, "Name must be shorter than 31 characters"),
    image: z.any().optional(),
});

const Profile = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();

    const isEditable = location.pathname.endsWith("/edit");

    const {login, logout} = useUserStore();

    const [toastData, setToastData] = useState(initialToastState);

    const {
        register,
        handleSubmit,
        control,
        watch,
        reset,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(ProfileSchema),
    });

    const {data: profile, isLoading: isPageLoading} = useQuery({
        queryKey: ["profile"],
        queryFn: async () => {
            const res = await Axios.get("/profile");
            return res.data.data;
        },
    });

    useEffect(() => {
        if (profile) {
            reset(profile);
        }
    }, [profile, reset]);

    const saveProfile = useMutation({
        mutationFn: async (data) => {
            const formData = prepareMultipartForm(data);

            const response = await Axios.put("/profile", formData);

            login(response.data.data);

            return response.data.data;
        },

        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: ["profile"]});

            showToast("Successfully updated", TOAST_TYPE.SUCCESS);

            setTimeout(() => {
                navigate("/profile");
            }, 500);
        },

        onError: (error, variables) => {
            console.error(error);
            handleErrors(error, setError, variables);

            showToast("Failed to update profile", TOAST_TYPE.ERROR);
        },
    });

    // ---------------- DELETE PROFILE ----------------
    const deleteProfile = useMutation({
        mutationFn: async () => {
            await Axios.delete("/profile");
        },

        onSuccess: async () => {
            showToast("Account deleted successfully", TOAST_TYPE.SUCCESS);

            await logout(); // IMPORTANT: await it

            queryClient.clear(); // optional but good

            navigate("/login");
        },

        onError: (error) => {
            console.error(error);
            handleErrors(error, setError, null);

            showToast("Failed to delete account", TOAST_TYPE.ERROR);
        },
    });

    // ---------------- HELPERS ----------------
    const handleNavigateToEdit = () => {
        navigate("edit");
    };

    const showToast = (message, type) => {
        setToastData({
            message,
            type,
            id: Date.now(),
        });
    };

    // ---------------- UI ----------------
    return (
        <React.Fragment>
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="min-h-[90vh] flex">
                <Card className="mx-auto my-auto w-[450px]">
                    <form onSubmit={handleSubmit((data) => saveProfile.mutate(data))}>
                        <fieldset disabled={!isEditable}>
                            <CardHeader>
                                <CardTitle>Profile</CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-4">
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
                                    <InputError errors={errors} field="name"/>
                                </div>

                                <div className="space-y-1">
                                    <Label>Role</Label>
                                    <InputViewMode value={(watch("role") || []).join(", ")} isEditable={isEditable}/>
                                </div>
                            </CardContent>
                        </fieldset>

                        <CardFooter className="flex-col gap-2">
                            {isSubmitting ? (
                                <ButtonLoading css="w-full"/>
                            ) : !isEditable ? (
                                <div className="w-full flex gap-2">
                                    <Button
                                        type="button"
                                        className="w-[50%] bg-gray-600 hover:bg-gray-800"
                                        onClick={handleNavigateToEdit}
                                    >
                                        Edit
                                    </Button>

                                    <AlertAction
                                        onConfirm={() => deleteProfile.mutate()}
                                        type={ALERT_TYPE.DELETE}
                                        css="w-[50%]"
                                    />
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

            {toastData.message && (
                <ToastAlert
                    key={toastData.id}
                    message={toastData.message}
                    type={toastData.type}
                />
            )}
        </React.Fragment>
    );
};

export default Profile;