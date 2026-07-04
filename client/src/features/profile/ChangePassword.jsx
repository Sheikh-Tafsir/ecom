import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import z from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

import { Button } from "@/components/ui/button"
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { ButtonLoading } from '@/components/common/ButtonLoading';
import { AxiosNoInterceptor } from '@/services/http/Axios.js';
import InputError from '@/components/common/InputError';
import StaredLabel from '@/components/common/StaredLabel';
import { TOAST_TYPE } from '@/utils/enums';
import { toastInitialState } from '@/utils';

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
});

const ChangePassword = () => {
    const navigate = useNavigate();

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(ChangePasswordSchema)
    });

    const [toastData, setToastData] = useState(toastInitialState);

    const handleChangePassword = async (data) => {
        try {
            await AxiosNoInterceptor.put(`password`, data)
        
            showToast("Password changed successfully", TOAST_TYPE.SUCCESS);
            reset();

            setTimeout(() => {
                navigate("/profile", { replace: true });
            }, 500);
        } catch (error) {
            console.log(error);
            handleError(error, setError);
        }
    }

    const showToast = (message, type) => {
        setToastData({message, type, id: Date.now()});
    };

    return (
        <>
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
                                <div className="space-y-2">
                                    <StaredLabel label={"Current Password"} field={"currentPassword"}/>
                                    <Input
                                        type="password"
                                        placeholder="8 characters min"
                                        {...register('currentPassword')}
                                        required
                                    />
                                    <InputError errors={errors} field={"currentPassword"}/>
                                </div>

                                <div className="space-y-2">
                                    <StaredLabel label={"New Password"} field={"newPassword"}/>
                                    <Input
                                        type="password"
                                        placeholder="8 characters min"
                                        {...register('newPassword')}
                                        required
                                    />
                                    <InputError errors={errors} field={"newPassword"}/>
                                </div>

                                <div className="space-y-2">
                                    <StaredLabel label={"Confirm Password"} field={"confirmPassword"}/>
                                    <Input
                                        type="password"
                                        placeholder="8 characters min"
                                        {...register('confirmPassword')}
                                        required
                                    />
                                    <InputError errors={errors} field={"confirmPassword"}/>
                                </div>
                            </CardContent>

                            <CardFooter className="flex-col gap-2 ">
                                {isSubmitting ?
                                    <ButtonLoading />
                                    :
                                    <Button type="submit" className="w-full">Save</Button>
                                }

                                {isResendOtpButtonLoading ? 
                                    <ButtonLoading/>
                                    : 
                                    <Button variant="outline" className="w-full" onClick={() => handleResendOTP()}>
                                        Resend OTP?
                                    </Button>
                                }
                            </CardFooter>
                        </form>
                    </Card>
                </div>

                <div className='lg:w-[50%]'>
                    <img src='https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSni4W_ssx3U1KqS7a7wY_Q4NVU2hW3CP-1jA&s'
                        className='cover h-full w-full' />
                </div>
            </div>

        
            <ToastAlert
                key={toastData.id}
                message={toastData.message}
                type={toastData.type}
            />
        </>
    )
}

export default ChangePassword