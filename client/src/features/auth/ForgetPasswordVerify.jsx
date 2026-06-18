import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import z from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp"
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
import { Label } from '@/components/ui/label';
import InputError from '@/components/common/InputError';

const ForgetPasswordVerifySchema = z.object({
    otp: z
        .string()
        .nonempty('Reset code is required')
        .length(6, 'Reset code must be 6 digits'),
    password: z
        .string()
        .nonempty('Password is required')
        .min(8, 'Password must be at least 8 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
    confirmPassword: z
        .string()
        .nonempty('Confirm Password is required')
        .min(8, 'Confirm Password must be at least 8 characters long')
        .max(15, 'Confirm Password must be shorter than 15 characters'),
}).refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
});

const ForgetPasswordVerify = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const email = new URLSearchParams(location.search).get("email");

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(ForgetPasswordVerifySchema)
    });

    const [isResendOtpButtonLoading, setIsResendOtpButtonLoading] = useState(false);

    const handleForgetResetVerify = async (data) => {
        try {
            await AxiosNoInterceptor.post(`/auth/forget-password/verify`, data)
        
            reset();
            navigate("/auth/login", { replace: true });
        } catch (error) {
            console.log(error);
            handleError(error, setError);
        }
    }

    const handleResendOTP = async () => {
        setIsResendOtpButtonLoading(true);

        try {
            await AxiosNoInterceptor.post('/auth/password-forget/resend',
                {
                    email,
                }
            )
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        } finally {
            setIsResendOtpButtonLoading(false);
        }
    }

    return (
        <div className='lg:flex h-[100vh]  overflow-hidden'>
            <div className='flex w-full lg:w-[50%] bg-gray-100 h-full'>
                <Card className="mx-auto my-auto w-[420px]">
                    <form onSubmit={handleSubmit(handleForgetResetVerify)}>
                        <CardHeader>
                            <CardTitle>Reset Password</CardTitle>
                            <CardDescription>
                                A 6 digits reset code has been sent to your email
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <div className="space-y-2">
                                <Label>Reset Code</Label>
                                <div className="flex">
                                    <InputOTP maxLength={6} {...register('otp')} required>
                                        <InputOTPGroup>
                                            <InputOTPSlot index={0} />
                                            <InputOTPSlot index={1} />
                                            <InputOTPSlot index={2} />
                                            <InputOTPSlot index={3} />
                                            <InputOTPSlot index={4} />
                                            <InputOTPSlot index={5} />
                                        </InputOTPGroup>
                                    </InputOTP>
                                </div>
                                <InputError errors={errors} field={"otp"}/>
                            </div>

                            <div className="space-y-2">
                                <Label>Password</Label>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    {...register('password')}
                                    required
                                />
                                <InputError errors={errors} field={"password"}/>
                            </div>

                            <div className="space-y-2">
                                <Label>Confirm Password</Label>
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
    )
}

export default ForgetPasswordVerify