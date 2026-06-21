import {useState} from 'react';
import {useNavigate, Link} from 'react-router-dom';
import {useGoogleLogin} from '@react-oauth/google';
import {FcGoogle} from "react-icons/fc";
import {EyeOff, Eye} from "lucide-react";
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';

import {Button} from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Checkbox} from '@/components/ui/checkbox';
import {ButtonLoading} from '@/components/common/ButtonLoading';
import {AxiosNoInterceptor} from '@/services/http/Axios';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import {useUserStore} from '@/store/useUserStore.js';
import InputError from "@/components/common/InputError.jsx";

// ✅ Validation schema
const LoginSchema = z.object({
    email: z
        .string()
        .nonempty('Email is required')
        .max(31, 'Email must be shorter than 31 characters'),
    password: z
        .string()
        .nonempty('Password is required')
        .min(8, 'Password must be at least 8 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
});

const Login = () => {
     const navigate = useNavigate();
    const {login} = useUserStore();

    const {
        register,
        handleSubmit,
        setError,
        reset,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(LoginSchema),
    });

    const [isGoogleSubmitting, setIsGoogleSubmitting] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    // ✅ Handle normal login
    const handleLogin = async (data) => {
        try {
            const response = await AxiosNoInterceptor.post(`/auth/login`, data);
            login(response.data.data);

            navigate(`/`, {replace: true});
        } catch (error) {
            console.error(error);
            handleErrors(error, setError);
        }
    };

    // ✅ Handle Google login
    const handleGoogleAuth = useGoogleLogin({
        onSuccess: async (tokenResponse) => {
            setIsGoogleSubmitting(true);

            try {
                const response = await AxiosNoInterceptor.post('/auth/google-login', {
                    token: tokenResponse.access_token,
                });

                //console.log(response.data.data);
                login(response.data.data);
                reset();
            } catch (err) {
                console.error(err);
                handleErrors(err, setError);
            } finally {
                setIsGoogleSubmitting(false);
            }
        },
        onError: (error) => {
            handleErrors(error, setError);
        },
    });

    const isAnyLoginSubmitting = () => isSubmitting || isGoogleSubmitting;

    return (
        <div className='lg:flex h-[100vh] overflow-hidden'>
            {/* Left side - form */}
            <div className='flex w-full lg:w-[50%] h-full'>
                <Card className="mx-auto my-auto w-[420px] pb-8">
                    <form onSubmit={handleSubmit(handleLogin)}>
                        <CardHeader>
                            <CardTitle>Login</CardTitle>
                            <CardDescription>Good to see you again</CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-4">
                            <InputError errors={errors} field={GLOBAL_ERROR}/>

                            {/* Email */}
                            <div className="space-y-2">
                                <Label htmlFor="email" className="flex">
                                    Email<p className='text-red-600'>*</p>
                                </Label>
                                <Input id="email" type="email" {...register('email')} required/>
                                <InputError errors={errors} field={"email"}/>
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <Label htmlFor="password" className="flex">
                                    Password<p className='text-red-600'>*</p>
                                </Label>
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

                            {/* Remember me + Forgot password */}
                            <Link to="/auth/forget-password" className='text-sm'>
                                Forgot Password?
                            </Link>
                        </CardContent>

                        <CardFooter className="">
                            {isAnyLoginSubmitting() ? (
                                <ButtonLoading/>
                            ) : (
                                <Button
                                    type="submit"
                                    className="w-full"
                                    disabled={isAnyLoginSubmitting()}
                                >
                                    Login
                                </Button>
                            )}
                        </CardFooter>
                    </form>

                    {/* Divider */}
                    <p className='text-center mb-4'>or</p>

                    {/* Google Login */}
                    <div className='flex'>
                        {isAnyLoginSubmitting()
                            ? (
                                <ButtonLoading css="w-[90%] mx-auto"/>
                            )
                            : (
                                <Button
                                    onClick={handleGoogleAuth}
                                    className="mx-auto"
                                    variant="outline"
                                    disabled={isAnyLoginSubmitting()}
                                >
                                    <FcGoogle className='my-auto mr-2 text-xl'/>
                                    <p>Google</p>
                                </Button>
                            )}
                    </div>

                    {/* Signup redirect */}
                    <div className='flex mt-4'>
                        <Link to="/auth/signup" className='flex mx-auto text-sm'>
                            Don't have an account?{" "}
                            <p className='text-blue-900 ml-1'>Signup</p>
                        </Link>
                    </div>
                </Card>
            </div>

            {/* Right side - image */}
            <div className='lg:w-[50%]'>
                <img
                    src='https://img.freepik.com/premium-vector/illustration-cartoon-female-user-entering-login_241107-682.jpg?w=740'
                    className='cover h-full w-full'
                />
            </div>
        </div>
    );
};

export default Login;
