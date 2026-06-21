import {Link, useNavigate} from 'react-router-dom';
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
import {AxiosNoInterceptor} from '@/services/http/Axios.js';
import {ButtonLoading} from '@/components/common/ButtonLoading';
import { handleErrors } from '@/utils';
import StaredLabel from '@/components/common/StaredLabel';
import InputError from '@/components/common/InputError';


const ForgetPasswordSchema = z.object({
    email: z
        .string()
        .nonempty('Email is required')
        .max(31, 'Email must be shorter than 31 characters'),
});

const ForgetPassword = () => {
    const navigate = useNavigate();

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(ForgetPasswordSchema)
    });

    const handleForgetPassword = async (data) => {
        try {
            await AxiosNoInterceptor.post(`/auth/forget-password`, data)

            const email = data.email;
            reset();

            navigate(`/auth/forget-password/verify?email=${email}`);
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    }

    return (
        <div className='lg:flex h-[100vh]  overflow-hidden'>
            <div className='flex w-full lg:w-[50%] bg-gray-100 h-full'>
                <Card className="mx-auto my-auto w-[420px]">
                    <form onSubmit={handleSubmit(handleForgetPassword)}>
                        <CardHeader>
                            <CardTitle>Forgot Password</CardTitle>
                            <CardDescription>
                                Good to see you again
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <div className="space-y-1">
                                <StaredLabel label = "Email"/>
                                <Input type="email" placeholder="ex something12@gmail.com" {...register('email')} required/>
                                <InputError errors={errors} field={"email"}/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex-col gap-2 ">
                            {isSubmitting ? 
                                <ButtonLoading/>
                                : 
                                <Button type="submit" className="w-full">Reset Password</Button>
                            }

                            <Link to="/auth/login" className='text-sm'>Remember Password?</Link>
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

export default ForgetPassword
