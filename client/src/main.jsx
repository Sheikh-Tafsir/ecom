import React from 'react'
import ReactDOM from 'react-dom/client'
import 'regenerator-runtime/runtime';
import { GoogleOAuthProvider } from '@react-oauth/google'
import App from './app/App.jsx'
import './index.css'
import {
    QueryClientProvider,
} from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from '@/services/queryClient.js';
import ErrorBoundary from './components/common/ErrorBoundary.jsx';

const GOOGLE_LOGIN_CLIENT_ID = import.meta.env.VITE_GOOGLE_LOGIN_CLIENT_ID;

ReactDOM.createRoot(document.getElementById('root')).render(
    <GoogleOAuthProvider clientId={GOOGLE_LOGIN_CLIENT_ID}>
        <React.StrictMode>
            <ErrorBoundary>
                <QueryClientProvider client={queryClient}>
                    <App />
                    {process.env.NODE_ENV == 'development' && <ReactQueryDevtools setInitialIsOpen={false} />}
                </QueryClientProvider>
            </ErrorBoundary>
        </React.StrictMode>
    </GoogleOAuthProvider>
)
