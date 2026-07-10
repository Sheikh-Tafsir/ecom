import {Outlet, Navigate, Link} from 'react-router-dom'
import {APP_NAME} from '@/utils';
import {useUserStore} from '@/store/useUserStore';

const AuthRoute = () => {
    const user = useUserStore(state => state.user);
    const isAuthenticated = !!user;

    return (
        !isAuthenticated ?
            <>
                <nav className='absolute w-[100%]'>
                    <div className='flex py-3 font-semibold w-[80%] mx-auto'>
                        <Link to="/" className="text-xl font-bold ">
                            <div className='flex'>
                                <img src="/navbar/icon3.png" className="h-12 w-12" alt="logo"/>
                                <p className='text-blue-900 my-auto ml-2'>{APP_NAME}</p>
                            </div>
                        </Link>
                    </div>
                </nav>
                <main>
                    <Outlet/>
                </main>
            </>
            :
            <Navigate to="/" replace/>
    )
}

export default AuthRoute