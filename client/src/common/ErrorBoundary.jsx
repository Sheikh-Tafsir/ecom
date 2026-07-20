import React from 'react';
import { Button } from '@/components/ui/button.jsx';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card.jsx';
import { AlertCircle, RefreshCw } from 'lucide-react';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error("Uncaught error:", error, errorInfo);
    }

    handleRefresh = () => {
        window.location.reload();
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
                    <Card className="max-w-md w-full shadow-lg">
                        <CardHeader className="text-center">
                            <div className="mx-auto bg-red-100 w-12 h-12 rounded-full flex items-center justify-center mb-4">
                                <AlertCircle className="text-red-600 h-6 w-6" />
                            </div>
                            <CardTitle className="text-2xl font-bold text-gray-900">Something went wrong</CardTitle>
                        </CardHeader>
                        <CardContent className="text-center space-y-6">
                            <p className="text-gray-600">
                                An unexpected error occurred. We apologize for the inconvenience. 
                                Please try refreshing the page.
                            </p>
                            <Button 
                                onClick={this.handleRefresh} 
                                className="w-full flex items-center justify-center gap-2"
                            >
                                <RefreshCw className="h-4 w-4" />
                                Refresh Page
                            </Button>
                        </CardContent>
                    </Card>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
