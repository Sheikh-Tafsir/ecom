import {Outlet, Navigate} from 'react-router-dom'

import NavigationBar from '@/components/common/NavigationBar';
import {useUserStore} from '@/store/useUserStore';
import Footer from "@/components/common/Footer.jsx";

const ProtectedRoute = ({allowedRoles}) => {
    const user = useUserStore(state => state.user);
    const isAuthenticated = !!user;

    const userRoles = (isAuthenticated && user?.role) ? (Array.isArray(user.role) ? user.role : [user.role]) : [];
    const hasPermission = isAuthenticated && (!allowedRoles || allowedRoles.length === 0 || allowedRoles.some(role => userRoles.includes(role)));

    if (!hasPermission) {
        return <Navigate to="/auth/login" state={{from: location.pathname}} replace/>
    }

    return (
        <>
            <NavigationBar/>
            <main className="bg-gradient-to-br from-blue-50 to-indigo-100 pb-8" style={{paddingTop: '70px'}}>
                <Outlet/>
            </main>
            <Footer/>
        </>
    )
}

export default ProtectedRoute