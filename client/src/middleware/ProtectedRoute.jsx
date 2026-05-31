import { Outlet, Navigate } from 'react-router-dom'

import NavigationBar from '@/mycomponents/NavigationBar';
import { useUserStore } from '../store/useUserStore';

const ProtectedRoute = ({ allowedRoles }) => {
    const { isAuthenticated, user } = useUserStore();

    const hasPermission = isAuthenticated() && (!allowedRoles || allowedRoles.length === 0 || allowedRoles.includes(user.role));

    if (!hasPermission) {
        return <Navigate to="/auth/login" state={{ from: location.pathname }} replace />
    }

    return (
        <main>
            {<NavigationBar />}
            <div className="bg-gradient-to-br from-blue-50 to-indigo-100 pt-24 pb-8">
                <Outlet />
            </div>
        </main>
    )
}

export default ProtectedRoute