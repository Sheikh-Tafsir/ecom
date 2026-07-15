import { useState, useCallback } from 'react';

/**
 * A reusable hook to handle upload progress for Axios requests.
 * Centralizes the logic for tracking and resetting progress.
 */
export const useUploadProgress = () => {
    const [progress, setProgress] = useState(0);

    const onUploadProgress = useCallback((progressEvent) => {
        if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setProgress(percentCompleted);
        }
    }, []);

    const resetProgress = useCallback(() => {
        setProgress(0);
    }, []);

    return { 
        progress, 
        onUploadProgress, 
        setProgress,
        resetProgress 
    };
};
