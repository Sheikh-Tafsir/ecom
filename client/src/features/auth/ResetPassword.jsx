import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

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
import { Axios } from '@/services/http/Axios.js';
import { Label } from '@/components/ui/label';


const ResetPassword = () => {
    const navigate = useNavigate();

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [token, setToken] = useState("");
    const [errors, setErrors] = useState([]);
    const [isButtonLoading, setIsButtonLoading] = useState(false);

    const handleError = (error) => {
        const responseErrors = error.response?.data || { message: error?.response?.data?.message };
        setErrors(responseErrors);
    };

    const handlePasswordReset = async (e) => {
        e.preventDefault();

        if (token.length != 6) return;

        setIsButtonLoading(true);
        try {
            await Axios.put(`/auth/reset-password`,
                {
                    token,
                    password,
                    confirmPassword,
                })

            setToken("");
            setPassword("");
            setConfirmPassword("");

            navigate("/auth/login", { replace: true });
        } catch (error) {
            console.log(error);
            handleError(error);
        } finally {
            setIsButtonLoading(false);
        }
    }

    return (
        <div className='lg:flex h-[100vh]  overflow-hidden'>
            <div className='flex w-full lg:w-[50%] bg-gray-100 h-full'>
                <Card className="mx-auto my-auto w-[420px]">
                    <form onSubmit={handlePasswordReset}>
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
                                    <InputOTP maxLength={6} value={token} onChange={(value) => setToken(value)}>
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
                                <p className='validation-error'>{errors.token || ""}</p>
                            </div>

                            <div className="space-y-2">
                                <Label>Password</Label>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    value={password}
                                    onChange={(event) => { setPassword(event.target.value); }}
                                />
                                <p className='validation-error'>{errors.password || ""}</p>
                            </div>

                            <div className="space-y-2">
                                <Label>Confirm Password</Label>
                                <Input
                                    type="password"
                                    placeholder="8 characters min"
                                    value={confirmPassword}
                                    onChange={(event) => { setConfirmPassword(event.target.value); }}
                                />
                                <p className='validation-error'>{errors.confirmPassword || ""}</p>
                            </div>

                            <p className='validation-error'>{errors.message || ""}</p>
                        </CardContent>

                        <CardFooter className="flex-col gap-2 ">
                            {isButtonLoading ?
                                <ButtonLoading />
                                :
                                <Button type="submit" className="w-full">Save</Button>
                            }
                            <Link to="/auth/login" className='text-sm'>Remember Password?</Link>
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

export default ResetPassword