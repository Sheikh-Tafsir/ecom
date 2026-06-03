import { Routes, Route, BrowserRouter } from "react-router-dom";
import React, { useEffect } from 'react';
import './App.css'

import AuthRoute from "@/routes/AuthRoute";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";

import { connectSocket, disconnectSocket } from '@/services/realtime/socket';
import { USER_ROLE } from "@/utils/enums";
import NotificationListener from "@/components/common/NotificationListener";

import Homepage from '@/pages/homepage/Homepage';
import NotFound from '@/pages/NotFound';

import Login from "@/pages/auth/Login.jsx";
import Signup from "@/pages/auth/Signup.jsx";
import ForgotPassword from "@/pages/auth/ForgotPassword";
import ResetPassword from "@/pages/auth/ResetPassword";

import Profile from '@/pages/profile/Profile.jsx'

import UserList from "@/pages/users/UserList.jsx";
import UserEdit from "@/pages/users/UserEdit.jsx";

import ProductList from "@/pages/product/ProductList";
import ProductView from "@/pages/product/ProductView";
import ProductCreate from "@/pages/product/ProductCreate";
import Inventory from "@/pages/product/Inventory";
import InventoryCreate from "@/pages/product/InventoryCreate";
import Sales from "@/pages/product/Sales";

import Cart from "@/pages/order/Cart";
import OrderList from "@/pages/order/OrderList";
import OrderView from "@/pages/order/OrderView";
import OrderCreate from "@/pages/order/OrderCreate";

import { useUserStore } from "@/store/useUserStore";

const App = () => {

  return (
    <>
      <BrowserRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
        <InnerApp />
        <NotificationListener />
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

  useEffect(() => {
    if (user?.id) {
      connectSocket(user.getAccessToken);
    } else {
      disconnectSocket();
    }

    return () => {
      disconnectSocket();
    };
  }, [user?.id])

  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/" element={<Homepage />} />
        <Route path="/products" element={<ProductList />} />
        <Route path="/products/:id" element={<ProductView />} />
      </Route>

      <Route element={<AuthRoute />}>
        <Route path="/auth/signup" element={<Signup />} />
        <Route path="/auth/login" element={<Login />} />
        <Route path="/auth/forgot-password" element={<ForgotPassword />} />
        <Route path="/auth/reset-password" element={<ResetPassword />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path='/profile' element={<Profile />} />
        <Route path='/profile/edit' element={<Profile />} />

        <Route path="/cart" element={<Cart />} />
        <Route path="/orders" element={<OrderList />} />
        <Route path="/orders/:id" element={<OrderView />} />
        <Route path="/orders/create" element={<OrderCreate />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={[USER_ROLE.ADMIN, USER_ROLE.SUPER_ADMIN]} />}>
        <Route path='/users' element={<UserList />} />
        <Route path='/users/:id' element={<UserEdit />} />
        <Route path='/users/:id/edit' element={<UserEdit />} />

        <Route path="/products/create" element={<ProductCreate />} />
        <Route path="/products/:id/edit" element={<ProductCreate />} />

        <Route path="/inventory" element={<Inventory />} />

        <Route path="/inventory/create" element={<InventoryCreate />} />

        <Route path="/sales" element={<Sales />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default App