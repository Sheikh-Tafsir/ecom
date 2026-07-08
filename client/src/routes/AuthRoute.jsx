import {Outlet, Navigate, Link} from 'react-router-dom'
import {APP_NAME} from '@/utils';
import {useUserStore} from '@/store/useUserStore';

const AuthRoute = () => {
    const user = useUserStore(state => state.user);
    const isAuthenticated = !!user;

    return (
        !isAuthenticated ?
            <>
                <main>
                    <Outlet/>
                </main>
            </>
            :
            <Navigate to="/" replace/>
    )
}

export default AuthRoute