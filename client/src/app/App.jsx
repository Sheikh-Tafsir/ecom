import {Routes, Route, BrowserRouter} from "react-router-dom";
import {useEffect} from 'react';
import './App.css'

import AuthRoute from "@/routes/AuthRoute";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";

import {connectSocket, disconnectSocket} from '@/services/realtime/socket';
import {USER_ROLE} from "@/utils/enums";
import NotificationListener from "@/components/common/NotificationListener";

import Homepage from '@/features/homepage/Homepage';
import NotFound from '@/features/NotFound';

import Login from "@/features/auth/Login.jsx";
import Signup from "@/features/auth/Signup.jsx";
import ForgotPassword from "@/features/auth/ForgotPassword";
import ResetPassword from "@/features/auth/ResetPassword";

import Profile from '@/features/profile/Profile.jsx'

import Users from "@/features/users/Users.jsx";
import UserEdit from "@/features/users/UserEdit.jsx";

import Products from "@/features/product/Products.jsx";
import ProductDetails from "@/features/product/ProductDetails.jsx";
import ProductCreate from "@/features/product/ProductCreate";
import Stocks from "@/features/stock/Stocks.jsx";
import StockCreate from "@/features/stock/StockCreate.jsx";
import Sales from "@/features/product/Sales";

import Cart from "@/features/order/Cart";
import OrderList from "@/features/order/OrderList";
import OrderView from "@/features/order/OrderView";
import OrderCreate from "@/features/order/OrderCreate";

import {useUserStore} from "@/store/useUserStore";
import StockItems from "@/features/stock/StockItems.jsx";
import StockDetails from "@/features/stock/StockDetails.jsx";
import {getAccessToken} from "@/utils/index.js";

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

    useEffect(() => {
        initUser();
    }, [initUser]);

    // useEffect(() => {
    //     if (user?.email) {
    //         connectSocket(getAccessToken());
    //     } else {
    //         disconnectSocket();
    //     }
    //
    //     return () => {
    //         disconnectSocket();
    //     };
    // }, [user])

    return (
        <Routes>
            <Route element={<PublicRoute/>}>
                <Route path="/" element={<Homepage/>}/>
                <Route path="/products" element={<Products/>}/>
                <Route path="/products/:id" element={<ProductDetails/>}/>
            </Route>

            <Route element={<AuthRoute/>}>
                <Route path="/auth/signup" element={<Signup/>}/>
                <Route path="/auth/login" element={<Login/>}/>
                <Route path="/auth/forgot-password" element={<ForgotPassword/>}/>
                <Route path="/auth/reset-password" element={<ResetPassword/>}/>
            </Route>

            <Route element={<ProtectedRoute/>}>
                <Route path='/profile' element={<Profile/>}/>
                <Route path='/profile/edit' element={<Profile/>}/>

                <Route path="/cart" element={<Cart/>}/>
                <Route path="/orders" element={<OrderList/>}/>
                <Route path="/orders/:id" element={<OrderView/>}/>
                <Route path="/orders/create" element={<OrderCreate/>}/>
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
    );
};

export default App