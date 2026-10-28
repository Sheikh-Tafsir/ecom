import React from 'react';
import {useNavigate} from 'react-router-dom';
import {useForm} from 'react-hook-form';
import z from 'zod';
import {zodResolver} from '@hookform/resolvers/zod';

import {Button} from "@/components/ui/button"
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {ButtonLoading} from '@/components/common/ButtonLoading';
import {AuthenticatedAxios} from '@/services/http/Axios.js';
import InputError from '@/components/common/InputError';
import FormField from '@/components/common/FormField';
import {TOAST_TYPE} from "@/constants/app.constants";
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import {toastify} from '@/common/toastify.js';

const ChangePasswordSchema = z.object({
    currentPassword: z
        .string()
        .min(8, 'Password must be at least 8 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
    newPassword: z
        .string()
        .min(8, 'Password must be at least 8 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
    confirmPassword: z
        .string()
        .min(8, 'Confirm Password must be at least 8 characters long')
        .max(15, 'Confirm Password must be shorter than 15 characters'),
}).refine((data) => data.newPassword == data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
});

const ChangePassword = () => {
    const navigate = useNavigate();

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(ChangePasswordSchema)
    });

    const handleChangePassword = async (data) => {
        try {
            await AuthenticatedAxios.put(`/profile/password`, data)

            toastify(TOAST_TYPE.SUCCESS, "Password changed successfully");
            reset();

            setTimeout(() => {
                navigate("/profile", {replace: true});
            }, 500);
        } catch (error) {
            handleErrors(error, setError);
        }
    }

    return (
        <div className='lg:flex h-[100vh]  overflow-hidden'>
            <div className='flex w-full lg:w-[50%] bg-gray-100 h-full'>
                <Card className="mx-auto my-auto w-[420px]">
                    <form onSubmit={handleSubmit(handleChangePassword)}>
                        <CardHeader>
                            <CardTitle>Change Password</CardTitle>
                            <CardDescription>
                                Enter your current password and a new password
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                            <FormField label="Current Password" field="currentPassword" errors={errors} required>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    {...register('currentPassword')}
                                    required
                                />
                            </FormField>

                            <FormField label="New Password" field="newPassword" errors={errors} required>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    {...register('newPassword')}
                                    required
                                />
                            </FormField>

                            <FormField label="Confirm Password" field="confirmPassword" errors={errors} required>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    {...register('confirmPassword')}
                                    required
                                />
                            </FormField>
                        </CardContent>

                        <CardFooter className="flex-col gap-2 ">
                            {isSubmitting ?
                                <ButtonLoading/>
                                :
                                <Button type="submit" className="w-full">Save</Button>
                            }

                            <Button
                                type="button"
                                variant="outline"
                                className="w-full"
                                onClick={() => navigate("/profile")}
                            >
                                Cancel
                            </Button>
                        </CardFooter>
                    </form>
                </Card>
            </div>

            <div className='lg:w-[50%]'>
                <img src='https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSni4W_ssx3U1KqS7a7wY_Q4NVU2hW3CP-1jA&s'
                     className='cover h-full w-full' alt="change password visual"/>
            </div>
        </div>
    )
}

export default ChangePassword
