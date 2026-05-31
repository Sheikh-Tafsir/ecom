import React from 'react'
import ReactDOM from 'react-dom/client'
import 'regenerator-runtime/runtime';
import { GoogleOAuthProvider } from '@react-oauth/google'
import App from './App.jsx'
import './index.css'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from './mycomponents/queryClient.js';

const GOOGLE_LOGIN_CLIENT_ID = import.meta.env.VITE_GOOGLE_LOGIN_CLIENT_ID;

ReactDOM.createRoot(document.getElementById('root')).render(
    <GoogleOAuthProvider clientId={GOOGLE_LOGIN_CLIENT_ID}>
        <React.StrictMode>
            <QueryClientProvider client={queryClient}>
                <App />
                {/* <ReactQueryDevtools setInitialIsOpen={false} /> */}
            </QueryClientProvider>
        </React.StrictMode>
    </GoogleOAuthProvider>
)
