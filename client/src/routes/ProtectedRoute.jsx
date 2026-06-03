import {Outlet, Navigate} from 'react-router-dom'

import NavigationBar from '@/components/common/NavigationBar';
import {useUserStore} from '@/store/useUserStore';

const ProtectedRoute = ({allowedRoles}) => {
    const {isAuthenticated, user} = useUserStore();

    const hasPermission = isAuthenticated() && (!allowedRoles || allowedRoles.length === 0 || allowedRoles.includes(user.role));

    if (!hasPermission) {
        return <Navigate to="/auth/login" state={{from: location.pathname}} replace/>
    }

    return (
        <>
            <NavigationBar/>
            <main className="bg-gradient-to-br from-blue-50 to-indigo-100 pt-24 pb-8">
                <Outlet/>
            </main>
        </>
    )
}

export default ProtectedRoute