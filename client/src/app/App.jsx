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

        <Route path='/users' element={<UserList />} />
        <Route path='/users/:id' element={<UserEdit />} />
        <Route path='/users/:id/edit' element={<UserEdit />} />
      </Route>

      {/* <Route element={<ProtectedRoute allowedRoles={[USER_ROLE.ADMIN]} />}>
        <Route path='/users' element={<UserList />} />
        <Route path='/users/:id' element={<UserEdit />} />
        <Route path='/users/:id/edit' element={<UserEdit />} />
      </Route> */}

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default App