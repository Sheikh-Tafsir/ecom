import {useState} from 'react';
import {useNavigate, useLocation} from 'react-router-dom';
import { useForm } from 'react-hook-form';
import z from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

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
import {AuthAxios} from '@/services/http/Axios.js';
import {ButtonLoading} from '@/components/common/ButtonLoading';
import { GLOBAL_ERROR, handleErrors } from '@/utils';
import StaredLabel from '@/components/common/StaredLabel';
import { useUserStore } from '@/store/useUserStore';
import InputError from '@/components/common/InputError';

const SignupVerifySchema = z.object({
    email: z
        .string()
        .min(1, 'Email is required')
        .max(31, 'Email must be shorter than 31 characters'),
});

const SignupVerify = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const email = new URLSearchParams(location.search).get("email");

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(SignupVerifySchema)
    });

    const {login} = useUserStore();
    
    const [isResendOtpButtonLoading, setIsResendOtpButtonLoading] = useState(false);

    const handleSignupVerify = async (data) => {
        try {
            const response = await AuthAxios.post('/auth/signup/verify', {
                ...data,
                email
            });

            reset();
            login(response.data.data);

            navigate("/", {replace: true});
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    }

    const handleResendOTP = async () => {
        setIsResendOtpButtonLoading(true);

        try {
            await AuthAxios.post('/auth/signup/resend',
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
                    <form onSubmit={handleSubmit(handleSignupVerify)}>
                        <CardHeader>
                            <CardTitle>Verify Email</CardTitle>
                            <CardDescription>
                                Enter otp to complete your registration
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>
                         
                            <div className="space-y-1">
                                <StaredLabel label="otp"/>
                                <Input type="text" value={otp} {...register("otp")} required/>
                                <InputError errors={errors} field={"otp"}/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex-col gap-2 ">
                            {isSubmitting ?
                                <ButtonLoading/>
                                :
                                <Button type="submit" className="w-full">Submit</Button>
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
                     className='cover h-full w-full' alt={""}/>
            </div>
        </div>
    )
}

export default SignupVerify
