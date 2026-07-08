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
import {AuthAxios} from '@/services/http/Axios';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import {useUserStore} from '@/store/useUserStore.js';
import InputError from "@/components/common/InputError.jsx";
import { APP_NAME } from '@/utils';

// ✅ Validation schema
const LoginSchema = z.object({
    email: z
        .string()
        .min(1, 'Email is required')
        .max(31, 'Email must be shorter than 31 characters'),
    password: z
        .string()
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
            const response = await AuthAxios.post(`/auth/login`, data);
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
                const response = await AuthAxios.post('/auth/google-login', {
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
        <div className='flex min-h-screen bg-slate-50 overflow-hidden'>
            {/* Left side - form */}
            <div className='flex-1 flex items-center justify-center p-8 lg:p-12 animate-in fade-in slide-in-from-left duration-700'>
                <div className="w-full max-w-[440px]">
                    <div className="mb-10 text-center lg:text-left">
                        <Link to="/" className="inline-flex items-center gap-2 mb-8 group transition-transform active:scale-95">
                            <img src="/navbar/icon3.png" className="h-10 w-10" alt="logo"/>
                            <span className="text-2xl font-black text-slate-900 tracking-tight">{APP_NAME}</span>
                        </Link>
                        <h1 className="text-3xl font-black text-slate-900 mb-2 tracking-tight">Welcome back!</h1>
                        <p className="text-slate-500 font-medium">Please enter your details to sign in to your account.</p>
                    </div>

                    <Card className="border-slate-200/60 shadow-xl shadow-slate-200/50 rounded-3xl overflow-hidden bg-white">
                        <form onSubmit={handleSubmit(handleLogin)}>
                            <CardHeader className="pb-2">
                                <CardTitle className="text-xl font-bold text-slate-800">Login</CardTitle>
                                <CardDescription className="text-slate-500">Good to see you again</CardDescription>
                            </CardHeader>

                            <CardContent className="space-y-5 pt-4">
                                <InputError errors={errors} field={GLOBAL_ERROR}/>

                                {/* Email */}
                                <div className="space-y-2">
                                    <Label htmlFor="email" className="text-xs font-bold text-slate-700 uppercase tracking-wider ml-1">
                                        Email Address
                                    </Label>
                                    <Input 
                                        id="email" 
                                        type="email" 
                                        placeholder="name@example.com"
                                        className="h-12 rounded-xl border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                        {...register('email')} 
                                        required
                                    />
                                    <InputError errors={errors} field={"email"}/>
                                </div>

                                {/* Password */}
                                <div className="space-y-2">
                                    <div className="flex items-center justify-between ml-1">
                                        <Label htmlFor="password" className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                                            Password
                                        </Label>
                                        <Link to="/auth/forget-password" className='text-xs font-bold text-blue-600 hover:text-blue-700'>
                                            Forgot?
                                        </Link>
                                    </div>
                                    <div className="relative group">
                                        <Input
                                            id="password"
                                            type={showPassword ? "text" : "password"}
                                            placeholder="••••••••"
                                            className="h-12 rounded-xl border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all pr-12"
                                            {...register('password')}
                                            required
                                        />
                                        <Button
                                            type="button"
                                            variant="ghost"
                                            size="icon"
                                            className="absolute right-1 top-1 h-10 w-10 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
                                            onClick={() => setShowPassword(!showPassword)}
                                        >
                                            {showPassword ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                                        </Button>
                                    </div>
                                    <InputError errors={errors} field={"password"}/>
                                </div>
                            </CardContent>

                            <CardFooter className="flex flex-col gap-4 pt-2">
                                {isAnyLoginSubmitting() ? (
                                    <ButtonLoading css="w-full h-12 rounded-xl bg-blue-600"/>
                                ) : (
                                    <Button
                                        type="submit"
                                        className="w-full h-12 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-200 transition-all active:scale-95"
                                        disabled={isAnyLoginSubmitting()}
                                    >
                                        Sign In
                                    </Button>
                                )}

                                <div className="relative w-full py-4">
                                    <div className="absolute inset-0 flex items-center">
                                        <span className="w-full border-t border-slate-100"></span>
                                    </div>
                                    <div className="relative flex justify-center text-xs uppercase">
                                        <span className="bg-white px-4 text-slate-400 font-bold tracking-widest">or continue with</span>
                                    </div>
                                </div>

                                <Button
                                    type="button"
                                    onClick={handleGoogleAuth}
                                    className="w-full h-12 rounded-xl border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-bold transition-all active:scale-95"
                                    variant="outline"
                                    disabled={isAnyLoginSubmitting()}
                                >
                                    <FcGoogle className='text-xl mr-2'/>
                                    Google Account
                                </Button>
                            </CardFooter>
                        </form>
                    </Card>

                    {/* Signup redirect */}
                    <p className="mt-8 text-center text-sm font-medium text-slate-500">
                        Don't have an account?{" "}
                        <Link to="/auth/signup" className="text-blue-600 font-bold hover:underline decoration-2 underline-offset-4">
                            Create an account
                        </Link>
                    </p>
                </div>
            </div>

            {/* Right side - image */}
            <div className='hidden lg:flex lg:w-[50%] xl:w-[55%] bg-blue-50 items-center justify-center p-12 relative overflow-hidden'>
                <div className="absolute top-0 right-0 w-64 h-64 bg-blue-100/50 rounded-full -mr-32 -mt-32 blur-3xl animate-pulse" />
                <div className="absolute bottom-0 left-0 w-64 h-64 bg-indigo-100/50 rounded-full -ml-32 -mb-32 blur-3xl animate-pulse" />
                
                <div className="relative z-10 w-full max-w-2xl transform hover:scale-[1.02] transition-transform duration-700">
                    <img
                        src='https://img.freepik.com/premium-vector/illustration-cartoon-female-user-entering-login_241107-682.jpg?w=740'
                        className='w-full h-auto drop-shadow-2xl rounded-3xl'
                        alt="Login illustration"
                    />
                    <div className="absolute -bottom-6 -right-6 bg-white p-6 rounded-3xl shadow-xl max-w-[240px] animate-bounce duration-[3000ms]">
                        <p className="text-sm font-bold text-slate-800 leading-tight">
                            "The best investment you can make is in yourself."
                        </p>
                        <p className="text-[10px] font-bold text-blue-600 mt-2 uppercase tracking-widest">— Learning Portal</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;
