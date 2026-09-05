import {Routes, Route, BrowserRouter} from "react-router-dom";
import {useEffect, lazy, Suspense} from 'react';
import './App.css'
import {Bounce, ToastContainer} from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import AuthRoute from "@/routes/AuthRoute";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";

import {connectSocket, disconnectSocket, isSocketOn} from '@/services/realtime/socket';
import {notificationService, isSseOn} from '@/services/realtime/notificationService.js';
import {PERMISSION} from "@/constants/auth.constants";
import NotificationWrapper from "@/services/realtime/NotificationWrapper.jsx";

const Homepage = lazy(() => import('@/features/homepage/Homepage'));
const NotFound = lazy(() => import('@/features/NotFound'));

const Login = lazy(() => import("@/features/auth/Login.jsx"));
const Signup = lazy(() => import("@/features/auth/Signup.jsx"));
const ForgetPassword = lazy(() => import("@/features/auth/ForgetPassword"));
const ForgetPasswordVerify = lazy(() => import("@/features/auth/ForgetPasswordVerify"));
const SignupVerify = lazy(() => import("@/features/auth/SignupVerify"));

const Profile = lazy(() => import('@/features/profile/Profile.jsx'));
const ChangePassword = lazy(() => import("@/features/profile/ChangePassword"));

const Users = lazy(() => import("@/features/users/Users.jsx"));
const UserEdit = lazy(() => import("@/features/users/UserEdit.jsx"));
const Roles = lazy(() => import("@/features/roles/Roles.jsx"));
const RoleSave = lazy(() => import("@/features/roles/RoleSave.jsx"));

const Products = lazy(() => import("@/features/product/Products.jsx"));
const ProductDetails = lazy(() => import("@/features/product/ProductDetails.jsx"));
const ProductSave = lazy(() => import("@/features/product/ProductSave"));
const Stocks = lazy(() => import("@/features/stock/Stocks.jsx"));
const StockCreate = lazy(() => import("@/features/stock/StockCreate.jsx"));
const StockItems = lazy(() => import("@/features/stock/StockItems.jsx"));
const StockDetails = lazy(() => import("@/features/stock/StockDetails.jsx"));
const Sales = lazy(() => import("@/features/sale/Sales"));

const Cart = lazy(() => import("@/features/order/Cart"));
const Orders = lazy(() => import("@/features/order/Orders"));
const OrderDetails = lazy(() => import("@/features/order/OrderDetails"));
const OrderCreate = lazy(() => import("@/features/order/OrderCreate"));

const Chat = lazy(() => import("@/features/chats/Chats.jsx"));
const PaymentSuccess = lazy(() => import("@/features/payment/PaymentSuccess"));
const PaymentFail = lazy(() => import("@/features/payment/PaymentFail"));

const Blogs = lazy(() => import("@/features/blogs/Blogs"));
const BlogDetails = lazy(() => import("@/features/blogs/BlogDetails"));
const AboutUs = lazy(() => import("@/features/staticPages/AboutUs"));
const PrivacyPolicy = lazy(() => import("@/features/staticPages/PrivacyPolicy"));
const TermsOfService = lazy(() => import("@/features/staticPages/TermsOfService"));
const BannerManager = lazy(() => import("@/features/banner/BannerManager"));
const FaqManager = lazy(() => import("@/features/faq/FaqManager"));

import {useUserStore} from "@/store/useUserStore";
import PageLoadingOverlay from "@/components/common/pageLoadingOverlay/PageLoadingOverlay.jsx";

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
    const isLoading = useUserStore((state) => state.isLoading);
    const setSocket = useUserStore((state) => state.setSocket);

    useEffect(() => {
        initUser();
    }, [initUser]);

    useEffect(() => {
        if (!isSocketOn() || isLoading) {
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
    }, [user, setSocket, isLoading])

    useEffect(() => {
        if (!isSseOn() || isLoading) {
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
    }, [user, isLoading])

    if (isLoading) {
        return <PageLoadingOverlay />;
    }

    return (
        <>
            <Suspense fallback={<div style={{display:'flex',justifyContent:'center',alignItems:'center',height:'100vh'}}>Loading...</div>}>
            <Routes>
                <Route element={<PublicRoute/>}>
                    <Route path="/" element={<Homepage/>}/>
                    <Route path="/about" element={<AboutUs/>}/>
                    <Route path="/privacy" element={<PrivacyPolicy/>}/>
                    <Route path="/terms" element={<TermsOfService/>}/>

                    <Route path="/blogs" element={<Blogs/>}/>
                    <Route path="/blogs/:title" element={<BlogDetails/>}/>

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
                    <Route path='/profile/change-password' element={<ChangePassword/>}/>

                    <Route path="/cart" element={<Cart/>}/>
                    <Route path="/orders" element={<Orders/>}/>
                    <Route path="/orders/:id" element={<OrderDetails/>}/>
                    <Route path="/orders/create" element={<OrderCreate/>}/>

                    <Route path="/chats" element={<Chat/>}/>
                    <Route path="/chats/:id" element={<Chat/>}/>

                    <Route path="/payment/success" element={<PaymentSuccess/>}/>
                    <Route path="/payment/fail" element={<PaymentFail/>}/>
                </Route>

                <Route element={<ProtectedRoute
                    allowedPermissions={[PERMISSION.ADMIN_ACCESS, PERMISSION.SUPER_ADMIN_ACCESS]}/>}>
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

                    <Route path="/banners" element={<BannerManager/>}/>
                    <Route path="/faqs" element={<FaqManager/>}/>
                </Route>

                <Route path="/not-found" element={<NotFound/>}/>
                <Route path="*" element={<NotFound/>}/>
            </Routes>
            </Suspense>

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