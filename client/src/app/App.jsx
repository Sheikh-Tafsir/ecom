import {Routes, Route, BrowserRouter} from "react-router-dom";
import {useEffect} from 'react';
import './App.css'
import {Bounce, ToastContainer} from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import AuthRoute from "@/routes/AuthRoute";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";

import {connectSocket, disconnectSocket, isSocketOn} from '@/services/realtime/socket';
import {notificationService, isSseOn} from '@/services/realtime/notificationService.js';
import {PERMISSION} from "@/utils/enums";
import NotificationWrapper from "@/services/realtime/NotificationWrapper.jsx";

import Homepage from '@/features/homepage/Homepage';
import NotFound from '@/features/NotFound';

import Login from "@/features/auth/Login.jsx";
import Signup from "@/features/auth/Signup.jsx";
import ForgetPassword from "@/features/auth/ForgetPassword";
import ForgetPasswordVerify from "@/features/auth/ForgetPasswordVerify";

import Profile from '@/features/profile/Profile.jsx'

import Users from "@/features/users/Users.jsx";
import UserEdit from "@/features/users/UserEdit.jsx";
import Roles from "@/features/roles/Roles.jsx";
import RoleSave from "@/features/roles/RoleSave.jsx";

import Products from "@/features/product/Products.jsx";
import ProductDetails from "@/features/product/ProductDetails.jsx";
import ProductSave from "@/features/product/ProductSave";
import Stocks from "@/features/stock/Stocks.jsx";
import StockCreate from "@/features/stock/StockCreate.jsx";
import Sales from "@/features/sale/Sales";

import Cart from "@/features/order/Cart";
import Orders from "@/features/order/Orders";
import OrderDetails from "@/features/order/OrderDetails";
import OrderCreate from "@/features/order/OrderCreate";

import {useUserStore} from "@/store/useUserStore";
import StockItems from "@/features/stock/StockItems.jsx";
import StockDetails from "@/features/stock/StockDetails.jsx";
import Chat from "@/features/chats/Chats.jsx";
import SignupVerify from "@/features/auth/SignupVerify";
import PaymentSuccess from "@/features/payment/PaymentSuccess";
import PaymentFail from "@/features/payment/PaymentFail";
import ChangePassword from "@/features/profile/ChangePassword";

const App = () => {

    return (
        <>
            <BrowserRouter future={{v7_relativeSplatPath: true, v7_startTransition: true}}>
                <InnerApp/>
                <NotificationWrapper/>
            </BrowserRouter>
        </>
    )
}

const InnerApp = () => {
    const initUser = useUserStore((state) => state.init);
    const user = useUserStore((state) => state.user);
    const setSocket = useUserStore((state) => state.setSocket);

    useEffect(() => {
        initUser();
    }, [initUser]);

    useEffect(() => {
        if (!isSocketOn()) {
            disconnectSocket();
            setSocket(null);
            return;
        }

        if (user?.email) {
            connectSocket().then(socket => {
                setSocket(socket);
            });
        } else {
            disconnectSocket();
            setSocket(null);
        }

        return () => {
            disconnectSocket();
            setSocket(null);
        };
    }, [user, setSocket])

    useEffect(() => {
        if (!isSseOn()) {
            notificationService.stop();
            return;
        }

        if (user?.email) {
            notificationService.start();
        } else {
            notificationService.stop();
        }

        return () => {
            notificationService.stop();
        };
    }, [user])

    return (
        <>
            <Routes>
                <Route element={<PublicRoute/>}>
                    <Route path="/" element={<Homepage/>}/>
                    <Route path="/products" element={<Products/>}/>
                    <Route path="/products/:id" element={<ProductDetails/>}/>
                </Route>

                <Route element={<AuthRoute/>}>
                    <Route path="/auth/signup" element={<Signup/>}/>
                    <Route path="/auth/signup/verify" element={<SignupVerify/>}/>
                    <Route path="/auth/login" element={<Login/>}/>
                    <Route path="/auth/forget-password" element={<ForgetPassword/>}/>
                    <Route path="/auth/forget-password/verify" element={<ForgetPasswordVerify/>}/>
                </Route>

                <Route element={<ProtectedRoute/>}>
                    <Route path='/profile' element={<Profile/>}/>
                    <Route path='/profile/edit' element={<Profile/>}/>
                     <Route path='/profile/chage-password' element={<ChangePassword/>}/>

                    <Route path="/cart" element={<Cart/>}/>
                    <Route path="/orders" element={<Orders/>}/>
                    <Route path="/orders/:id" element={<OrderDetails/>}/>
                    <Route path="/orders/create" element={<OrderCreate/>}/>

                    <Route path="/chats" element={<Chat/>}/>
                    <Route path="/chats/:id" element={<Chat/>}/>

                    <Route path="/payment/success" element={<PaymentSuccess/>}/>
                    <Route path="/payment/fail" element={<PaymentFail/>}/>
                </Route>

                <Route element={<ProtectedRoute allowedPermissions={[PERMISSION.ADMIN_ACCESS, PERMISSION.SUPER_ADMIN_ACCESS]}/>}>
                    <Route path='/users' element={<Users/>}/>
                    <Route path='/users/:id' element={<UserEdit/>}/>

                    <Route path="/stocks" element={<Stocks/>}/>
                    <Route path="/stocks/items" element={<StockItems/>}/>
                    <Route path="/stocks/:id" element={<StockDetails/>}/>
                </Route>

                <Route element={<ProtectedRoute allowedPermissions={[PERMISSION.SUPER_ADMIN_ACCESS]}/>}>
                    <Route path='/users/:id/edit' element={<UserEdit/>}/>
                    
                    <Route path='/roles' element={<Roles/>}/>
                    <Route path='/roles/create' element={<RoleSave/>}/>
                    <Route path='/roles/:id/edit' element={<RoleSave/>}/>

                    <Route path="/products/create" element={<ProductSave/>}/>
                    <Route path="/products/:id/edit" element={<ProductSave/>}/>

                    <Route path="/stocks/create" element={<StockCreate/>}/>

                    <Route path="/sales" element={<Sales/>}/>
                </Route>

                <Route path="/not-found" element={<NotFound/>}/>
                <Route path="*" element={<NotFound/>}/>
            </Routes>

            <ToastContainer
                position="bottom-right"
                autoClose={5000}
                hideProgressBar={false}
                newestOnTop={false}
                closeOnClick={false}
                rtl={false}
                pauseOnFocusLoss
                draggable
                pauseOnHover
                theme="light"
                transition={Bounce}
            />
        </>
    );
};

export default App