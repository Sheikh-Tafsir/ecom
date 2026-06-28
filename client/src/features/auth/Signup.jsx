import { useNavigate, Link } from 'react-router-dom';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {EyeOff, Eye} from "lucide-react";

import {Button} from "@/components/ui/button.jsx"
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card.jsx"
import {Input} from "@/components/ui/input.jsx"
import {Label} from "@/components/ui/label.jsx"
import {ButtonLoading} from '@/components/common/ButtonLoading';
import {AxiosNoInterceptor} from '@/services/http/Axios';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils.js';
import InputError from "@/components/common/InputError.jsx";
import StaredLabel from '@/components/common/StaredLabel';
import { useState } from 'react';

const SignupSchema = z.object({
    name: z
        .string()
        .min(2, 'Name must be at least 2 characters')
        .max(31, 'Name must be shorter than 31 characters'),
    email: z
        .string()
        .min(1, 'Email is required')
        .max(31, 'Email must be shorter than 31 characters'),
    password: z
        .string()
        .min(8, 'Password must be at least 8 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
    confirmPassword: z
        .string()
        .min(8, 'Confirm Password must be at least 8 characters long')
        .max(15, 'Confirm Password must be shorter than 15 characters'),
}).refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
});

const Signup = () => {
    const navigate = useNavigate();

    const {register, handleSubmit, setError, reset, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(SignupSchema)
    });

    const [showPassword, setShowPassword] = useState(true);
    const [showConfirmPassword, setShowConfirmPassword] = useState(true);

    const handleSignup = async (data) => {
        try {
            await AxiosNoInterceptor.post(`/auth/signup`, data);

            const email = data.email;
            reset();

            navigate(`/auth/signup/verify?email=${email}`);
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    };

    return (
        <div className="lg:flex h-[100vh]">
            <div className="lg:w-[50%]">
                <img
                    src="https://static.vecteezy.com/system/resources/thumbnails/005/879/539/small_2x/cloud-computing-modern-flat-concept-for-web-banner-design-man-enters-password-and-login-to-access-cloud-storage-for-uploading-and-processing-files-illustration-with-isolated-people-scene-free-vector.jpg"
                    alt="Signup visual"
                    className="cover h-full w-full"
                />
            </div>

            <div className="flex w-full lg:w-[50%] h-full">
                <Card className="mx-auto my-auto w-[450px]">
                    <form onSubmit={handleSubmit(handleSignup)}>
                        <CardHeader>
                            <CardTitle>Signup</CardTitle>
                            <CardDescription>Register as a user</CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                            {/* Name */}
                            <div className="space-y-2">
                                <Label htmlFor="name" className="flex">
                                    Name<p className="text-red-600 ml-1">*</p>
                                </Label>
                                <Input type="text" id="name" {...register("name")} required/>
                                <InputError errors={errors} field={"name"}/>
                            </div>

                            {/* Email */}
                            <div className="space-y-2">
                                <Label htmlFor="email" className="flex">
                                    Email<p className="text-red-600 ml-1">*</p>
                                </Label>
                                <Input type="email" id="email" {...register("email")} required />
                                <InputError errors={errors} field={"email"}/>
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                 <StaredLabel label="Password"/>
                                <div
                                    className='flex rounded-lg'
                                    style={{border: "0.5px solid rgba(0,0,0,0.1)"}}
                                >
                                    <Input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        {...register('password')}
                                        required
                                    />
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        className="my-auto cursor-pointer bg-gray-100"
                                        onClick={() => setShowPassword(!showPassword)}
                                    >
                                        {showPassword ? (
                                            <Eye className=''/>
                                        ) : (
                                            <EyeOff className=''/>
                                        )}
                                    </Button>
                                </div>
                                <InputError errors={errors} field={"password"}/>
                            </div>

                            {/* Confirm Password */}
                            <div className="space-y-2">
                                <StaredLabel label="Confirm Password"/>
                                <div
                                    className='flex rounded-lg'
                                    style={{border: "0.5px solid rgba(0,0,0,0.1)"}}
                                >
                                    <Input
                                        id="confirmPassword"
                                        type={showConfirmPassword ? "text" : "password"}
                                        {...register('confirmPassword')}
                                        required
                                    />
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        className="my-auto cursor-pointer bg-gray-100"
                                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    >
                                        {showConfirmPassword ? (
                                            <Eye className=''/>
                                        ) : (
                                            <EyeOff className=''/>
                                        )}
                                    </Button>
                                </div>
                                <InputError errors={errors} field={"confirmPassword"}/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex-col">
                            {isSubmitting ? 
                                <ButtonLoading />
                                : 
                                <Button type="submit" className="w-full">Sign Up</Button>
                            }
                            <div className="flex mt-4">
                                <Link to="/auth/login" className="flex mx-auto text-sm">
                                    Already have an account?
                                    <p className="text-blue-900 ml-1">Login</p>
                                </Link>
                            </div>
                        </CardFooter>
                    </form>
                </Card>
            </div>
        </div>
    );
};

export default Signup;