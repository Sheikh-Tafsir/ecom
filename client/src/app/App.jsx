import {Routes, Route, BrowserRouter} from "react-router-dom";
import {useEffect} from 'react';
import './App.css'
import {Bounce, ToastContainer} from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import AuthRoute from "@/routes/AuthRoute";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";

import {connectSocket, disconnectSocket, isSocketOn} from '@/services/realtime/socket';
import {USER_ROLE} from "@/utils/enums";
import NotificationListener from "@/components/common/NotificationListener";

import Homepage from '@/features/homepage/Homepage';
import NotFound from '@/features/NotFound';

import Login from "@/features/auth/Login.jsx";
import Signup from "@/features/auth/Signup.jsx";
import ForgetPassword from "@/features/auth/ForgetPassword";
import ForgetPasswordVerify from "@/features/auth/ForgetPasswordVerify";

import Profile from '@/features/profile/Profile.jsx'

import Users from "@/features/users/Users.jsx";
import UserEdit from "@/features/users/UserEdit.jsx";

import Products from "@/features/product/Products.jsx";
import ProductDetails from "@/features/product/ProductDetails.jsx";
import ProductCreate from "@/features/product/ProductCreate";
import Stocks from "@/features/stock/Stocks.jsx";
import StockCreate from "@/features/stock/StockCreate.jsx";
import Sales from "@/features/sale/Sales";

import Cart from "@/features/order/Cart";
import Orders from "@/features/order/Orders";
import OrderView from "@/features/order/OrderView";
import OrderCreate from "@/features/order/OrderCreate";

import {useUserStore} from "@/store/useUserStore";
import StockItems from "@/features/stock/StockItems.jsx";
import StockDetails from "@/features/stock/StockDetails.jsx";
import {getAccessToken} from "@/utils/index.js";
import Chat from "@/features/chats/Chats.jsx";
import SignupVerify from "@/features/auth/SignupVerify";
import PaymentSuccess from "@/features/payment/PaymentSuccess";
import PaymentFail from "@/features/payment/PaymentFail";

const App = () => {

    return (
        <>
            <BrowserRouter future={{v7_relativeSplatPath: true, v7_startTransition: true}}>
                <InnerApp/>
                <NotificationListener/>
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
        //console.log("Socket is on:", isSocketOn);
        if (!isSocketOn) return;

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
    }, [user, isSocketOn, setSocket])

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

                    <Route path="/cart" element={<Cart/>}/>
                    <Route path="/orders" element={<Orders/>}/>
                    <Route path="/orders/:id" element={<OrderView/>}/>
                    <Route path="/orders/create" element={<OrderCreate/>}/>

                    <Route path="/chats" element={<Chat/>}/>
                    <Route path="/chats/:id" element={<Chat/>}/>

                    <Route path="/payment/success" element={<PaymentSuccess/>}/>
                    <Route path="/payment/fail" element={<PaymentFail/>}/>
                </Route>

                <Route element={<ProtectedRoute allowedRoles={[USER_ROLE.ADMIN, USER_ROLE.SUPER_ADMIN]}/>}>
                    <Route path='/users' element={<Users/>}/>
                    <Route path='/users/:id' element={<UserEdit/>}/>
                    <Route path='/users/:id/edit' element={<UserEdit/>}/>

                    <Route path="/products/create" element={<ProductCreate/>}/>
                    <Route path="/products/:id/edit" element={<ProductCreate/>}/>

                    <Route path="/stocks" element={<Stocks/>}/>
                    <Route path="/stocks/items" element={<StockItems/>}/>
                    <Route path="/stocks/:id" element={<StockDetails/>}/>
                    <Route path="/stocks/create" element={<StockCreate/>}/>

                    <Route path="/sales" element={<Sales/>}/>
                </Route>

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