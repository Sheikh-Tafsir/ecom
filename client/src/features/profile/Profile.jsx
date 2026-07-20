import React, {useEffect, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {useForm} from "react-hook-form";
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

import {useUserStore} from "@/store/useUserStore";
import {GLOBAL_ERROR, handleErrors} from "@/utils/ErrorUtils";
import {TOAST_TYPE, ALERT_TYPE} from "@/utils/enums";
import InputError from "@/components/common/InputError.jsx";
import {toastify} from "@/common/toastify.js";
import { cn } from "@/lib/utils";
import { compressImage } from "@/utils/ImageUtils";

import { useUploadProgress } from "@/hooks/useUploadProgress";
import UploadProgress from "@/components/common/UploadProgress";

const ProfileSchema = z.object({
    name: z.string()
        .min(2, "Name is required")
        .max(31, "Name must be shorter than 31 characters"),
    image: z.any().optional(),
});

const fetchProfile = async () => {
    const response = await Axios.get("/profile");
    return response.data.data;
}

const Profile = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();

    const isEditable = location.pathname.endsWith("/edit");

    const {login, logout} = useUserStore();

    const [existingImage, setExistingImage] = useState();
    const [newImage, setNewImage] = useState();

    const { progress, onUploadProgress, resetProgress } = useUploadProgress();

    const {
        register,
        handleSubmit,
        watch,
        reset,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(ProfileSchema),
    });

    const {
        data: profile,
        isFetching: isPageLoading,
        isError
    } = useQuery({
        queryKey: ["profile"],
        queryFn: fetchProfile
    });

    useEffect(() => {
        if (profile) {
            reset(profile);
            setExistingImage(profile.image)
        }
    }, [profile, reset]);

    const saveProfileMutation = useMutation({
        mutationFn: async (data) => {
            resetProgress();
            const formData = new FormData();

            Object.entries(data).forEach(([key, value]) => {
                if (key == "image") return;

                if (Array.isArray(value)) {
                    value.forEach((item) => formData.append(key, item));
                } else {
                    formData.append(key, value ?? "");
                }
            });

            if (newImage instanceof File) {
                const compressedImage = await compressImage(newImage);
                formData.append("image", compressedImage);
            }

            const response = await Axios.put("/profile", formData, {
                headers: {'Content-Type': 'multipart/form-data'},
                timeout: 8000,
                onUploadProgress,
            });

            const user = response.data.data;
            login(user.accessToken);
            reset(user);
        },

        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: ["profile"]});
            toastify(TOAST_TYPE.SUCCESS, "Profile updated successfully")

            navigate("/profile");
        },

        onError: (error) => {
            console.error(error);
            toastify(TOAST_TYPE.ERROR, "Failed to update profile")
            handleErrors(error, setError);
        },
    });

    const saveProfile = async (data) => {
        await saveProfileMutation.mutateAsync(data);
    };

    const deleteProfile = useMutation({
        mutationFn: async () => {
            await Axios.delete("/profile");
        },

        onSuccess: async () => {
            toastify(TOAST_TYPE.SUCCESS, "Account deleted successfully")

            await logout();

            queryClient.clear();

            setTimeout(() => {
                navigate("/login");
            }, 500);
        },

        onError: (error) => {
            console.error(error);
            handleErrors(error, setError);
        },
    });

    const handleNavigateToEdit = () => {
        navigate("edit");
    };

    useEffect(() => {
        if (isError) {
            toastify(TOAST_TYPE.ERROR, "Could not load profile")
        }
    }, [isError, errors])

    return (
        <div className="bg-slate-50 min-h-screen">
            {isPageLoading && <PageLoadingOverlay/>}

            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 md:py-20">
                <div className="mb-10 text-center md:text-left">
                    <h1 className="text-4xl font-bold text-slate-900 tracking-tight mb-2">Account Settings</h1>
                    <p className="text-slate-500 font-medium">Manage your personal information and security preferences</p>
                </div>

                <Card className="border-slate-100 shadow-xl shadow-slate-200/50 rounded-[2.5rem] overflow-hidden bg-white animate-in fade-in slide-in-from-bottom-8 duration-700">
                    <form onSubmit={handleSubmit(saveProfile)}>
                        <CardHeader className="bg-slate-50/50 border-b border-slate-50 p-8 md:p-120">
                            <div className="flex flex-col md:flex-row items-center gap-8">
                                <div className="relative group w-[60%]">
                                    <div className="absolute -inset-1 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-full blur opacity-25 group-hover:opacity-40 transition duration-500 " />
                                    <div className="relative">
                                        <ImageInput
                                            existingImage={existingImage}
                                            onExistingImageChange={setExistingImage}
                                            onImageChange={setNewImage}
                                            errors={errors}
                                            setError={setError}
                                            disabled={!isEditable}
                                        />
                                    </div>
                                </div>
                                <div className="text-center md:text-left space-y-2">
                                    <CardTitle className="text-3xl font-bold text-slate-900 tracking-tight">{profile?.name || "Your Profile"}</CardTitle>
                                    <div className="flex flex-wrap justify-center md:justify-start gap-3">
                                        <span className="px-3 py-1 rounded-full bg-blue-50 text-blue-700 text-[10px] font-bold uppercase tracking-widest">
                                            {profile?.roles?.[0] || "User"}
                                        </span>
                                        <span className="px-3 py-1 rounded-full bg-slate-100 text-slate-500 text-[10px] font-bold uppercase tracking-widest flex items-center gap-1.5">
                                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                                            Active Account
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </CardHeader>

                        <CardContent className="p-8 md:p-12">
                            <fieldset disabled={!isEditable} className="grid md:grid-cols-2 gap-8 md:gap-12">
                                <div className="space-y-8">
                                    <div>
                                        <h3 className="text-sm font-bold text-slate-800 uppercase tracking-widest mb-6 flex items-center gap-2">
                                            <span className="w-6 h-px bg-blue-600" />
                                            Personal Details
                                        </h3>
                                        
                                        <div className="space-y-6">
                                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                                            <div className="space-y-2">
                                                <Label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Full Name</Label>
                                                <Input 
                                                    {...register("name")} 
                                                    placeholder="Md Rafiquddin"
                                                    className={cn(
                                                        "h-12 rounded-xl transition-all font-medium",
                                                        isEditable ? "border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400" : "bg-slate-50 border-transparent cursor-not-allowed text-slate-600"
                                                    )}
                                                />
                                                <InputError errors={errors} field="name"/>
                                            </div>

                                            <div className="space-y-2">
                                                <Label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Email Address</Label>
                                                <div className="h-12 rounded-xl bg-slate-50 border border-transparent px-4 flex items-center text-slate-500 font-medium text-sm overflow-hidden">
                                                    {watch("email")}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="space-y-8">
                                    <div>
                                        <h3 className="text-sm font-bold text-slate-800 uppercase tracking-widest mb-6 flex items-center gap-2">
                                            <span className="w-6 h-px bg-blue-600" />
                                            Account Access
                                        </h3>
                                        
                                        <div className="space-y-6">
                                            <div className="space-y-2">
                                                <Label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Current Role</Label>
                                                <div className="h-12 rounded-xl bg-slate-50 border border-transparent px-4 flex items-center text-slate-500 font-bold text-xs uppercase tracking-widest overflow-hidden">
                                                    {watch("roles") || "Standard User"}
                                                </div>
                                            </div>

                                            <div className="p-6 rounded-2xl bg-amber-50 border border-amber-100 space-y-3">
                                                <h4 className="text-xs font-bold text-amber-900 uppercase tracking-widest flex items-center gap-2">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 002 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
                                                    Security Tip
                                                </h4>
                                                <p className="text-xs text-amber-800/80 leading-relaxed font-medium">
                                                    Keep your email private and enable two-factor authentication to ensure your account remains secure.
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                        </CardContent>

                        <CardFooter className="bg-slate-50/30 border-t border-slate-50 p-8 md:p-12 flex flex-col gap-6">
                            <UploadProgress progress={progress} />
                            
                            <div className="w-full flex flex-col sm:flex-row items-center gap-4">
                                {isSubmitting ? (
                                    <ButtonLoading className="w-full h-14 rounded-2xl bg-blue-600" />
                                ) : !isEditable ? (
                                    <>
                                        <Button
                                            type="button"
                                            className="w-full sm:flex-1 h-14 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-semibold text-lg shadow-xl shadow-blue-600/20 transition-all hover:scale-[1.02] active:scale-95 group"
                                            onClick={handleNavigateToEdit}
                                        >
                                            <svg className="w-5 h-5 mr-2 group-hover:rotate-12 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                                            Edit Profile
                                        </Button>

                                        <AlertAction
                                            onConfirm={() => deleteProfile.mutate()}
                                            type={ALERT_TYPE.DELETE}
                                            css="w-full sm:w-auto h-14 px-8 rounded-2xl border-slate-200 text-white font-bold hover:bg-red-500 transition-all"
                                        />
                                    </>
                                ) : (
                                    <div className="w-full flex gap-4">
                                        <Button 
                                            type="button" 
                                            variant="outline" 
                                            className="flex-1 h-14 rounded-2xl font-bold text-slate-500 hover:bg-slate-100"
                                            onClick={() => navigate("/profile")}
                                        >
                                            Cancel
                                        </Button>
                                        <Button 
                                            type="submit" 
                                            className="flex-[2] h-14 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-lg shadow-xl shadow-emerald-600/20 transition-all hover:scale-[1.02] active:scale-95"
                                        >
                                            Save Changes
                                        </Button>
                                    </div>
                                )}
                            </div>
                        </CardFooter>
                    </form>
                </Card>
            </div>
        </div>
    );
};

export default Profile;