import {Link} from 'react-router-dom';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';

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
import {useUserStore} from '@/store/useUserStore.js';
import InputError from "@/components/common/InputError.jsx";

const SignupSchema = z.object({
    name: z
        .string()
        .min(2, 'Name must be at least 2 characters')
        .max(31, 'Name must be shorter than 31 characters'),
    email: z
        .string()
        .max(31, 'Email must be shorter than 31 characters'),
    password: z
        .string()
        .min(8, 'Password must be at least 6 characters long')
        .max(15, 'Password must be shorter than 15 characters'),
    confirmPassword: z
        .string()
        .min(8, 'Confirm Password must be at least 6 characters long')
        .max(15, 'Confirm Password must be shorter than 15 characters'),
}).refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
});

const Signup = () => {
    const {login} = useUserStore();

    const {register, handleSubmit, setError, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(SignupSchema)
    });

    const handleSignup = async (data) => {
        try {
            const response = await AxiosNoInterceptor.post(`/users`, data);
            login(response.data.data);
        } catch (error) {
            //console.log(error);
            handleErrors(error, setError);
        }
    };

    return (
        <div className="lg:flex h-[100vh]">
            {/* Left Image */}
            <div className="lg:w-[50%]">
                <img
                    src="https://static.vecteezy.com/system/resources/thumbnails/005/879/539/small_2x/cloud-computing-modern-flat-concept-for-web-banner-design-man-enters-password-and-login-to-access-cloud-storage-for-uploading-and-processing-files-illustration-with-isolated-people-scene-free-vector.jpg"
                    alt="Signup visual"
                    className="cover h-full w-full"
                />
            </div>

            {/* Form Section */}
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
                                <Input type="text" id="name" {...register("name")} />
                                <InputError errors={errors} field={"name"}/>
                            </div>

                            {/* Email */}
                            <div className="space-y-2">
                                <Label htmlFor="email" className="flex">
                                    Email<p className="text-red-600 ml-1">*</p>
                                </Label>
                                <Input type="email" id="email" {...register("email")} />
                                <InputError errors={errors} field={"email"}/>
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <Label htmlFor="password" className="flex">
                                    Password<p className="text-red-600 ml-1">*</p>
                                </Label>
                                <Input type="password" id="password" {...register("password")} />
                                <InputError errors={errors} field={"password"}/>
                            </div>

                            {/* Confirm Password */}
                            <div className="space-y-2">
                                <Label htmlFor="confirmPassword" className="flex">
                                    Confirm Password<p className="text-red-600 ml-1">*</p>
                                </Label>
                                <Input
                                    type="password"
                                    id="confirmPassword"
                                    {...register("confirmPassword")}
                                />
                                <InputError errors={errors} field={"confirmPassword"}/>
                            </div>
                        </CardContent>

                        <CardFooter className="flex-col">
                            {isSubmitting
                                ? (
                                    <ButtonLoading />
                                )
                                : (
                                    <Button
                                        type="submit"
                                        className="w-full"
                                        style={{backgroundColor: "rgb(24,62,139)"}}
                                    >
                                        Sign Up
                                    </Button>
                                )}

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