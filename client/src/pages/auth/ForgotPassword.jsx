import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';

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
import {Label} from "@/components/ui/label"
import {Axios} from '@/services/http/Axios.js';
import {ButtonLoading} from '@/components/common/ButtonLoading';

const ForgotPassword = () => {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [errors, setErrors] = useState([]);
    const [isButtonLoading, setIsButtonLoading] = useState(false);

    const handleError = (error) => {
        const responseErrors = error.response?.data || {message: error?.response?.data?.message};
        setErrors(responseErrors);
    };

    const handleForgotPassword = async (e) => {
        e.preventDefault();

        setIsButtonLoading(true);
        try {
            await Axios.post(`/auth/forgot-password`,
                {
                    email,
                })

            //console.log(response);
            setEmail('');
            setErrors([]);

            navigate("/auth/reset-password");
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
                    <form onSubmit={handleForgotPassword}>
                        <CardHeader>
                            <CardTitle>Forgot Password</CardTitle>
                            <CardDescription>
                                Good to see you again
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <div className="space-y-1">
                                <Label htmlFor="name">Email</Label>
                                <Input type="email" placeholder="ex something12@gmail.com" value={email}
                                       onChange={(event) => {
                                           setEmail(event.target.value);
                                       }} required/>
                            </div>
                            <p className='validation-error'>{errors.message || ""}</p>
                        </CardContent>

                        <CardFooter className="flex-col gap-2 ">
                            {isButtonLoading ?
                                <ButtonLoading css="w-full"/>
                                :
                                <Button type="submit" className="w-full">Reset</Button>
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

export default ForgotPassword
