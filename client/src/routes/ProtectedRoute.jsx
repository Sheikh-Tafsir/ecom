import {Outlet, Navigate, useLocation} from 'react-router-dom'

import NavigationBar from '@/components/common/NavigationBar';
import {useUserStore} from '@/store/useUserStore';
import Footer from "@/components/common/Footer.jsx";
import {hasPermission} from "@/utils/AuthUtils";

const ProtectedRoute = ({allowedPermissions}) => {
    const location = useLocation();
    const user = useUserStore(state => state.user);
    
    if (!user) {
         return (
            <Navigate
                to="/auth/login"
                state={{ from: location }}
                replace
            />
        );
    }

    if (allowedPermissions && allowedPermissions.length > 0) { 
        if (!hasPermission(user, allowedPermissions)) {
            return (
                <Navigate
                    to="/auth/login"
                    state={{ from: location }}
                    replace
                />
            );
        }
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