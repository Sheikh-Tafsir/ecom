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
                <Navigate to="/not-found"/>
            );
        }
    }

    const hideFooter =
        location.pathname == "/chats" ||
        location.pathname.startsWith("/chats/");

    return (
        <>
            <NavigationBar/>
                <main className="bg-slate-50 from-blue-50 to-indigo-100 pb-8">
                    <Outlet/>
                </main>
            {!hideFooter && <Footer />}
        </>
    )
}

export default ProtectedRoute