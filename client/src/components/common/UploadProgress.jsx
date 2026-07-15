import React from 'react';
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

/**
 * A centralized, reusable upload progress component.
 * 
 * @param {Object} props
 * @param {number} props.progress - Current progress (0-100)
 * @param {string} props.label - Text to display above the bar
 * @param {string} props.className - Additional classes for the container
 */
const UploadProgress = ({ progress, label = "Uploading...", className }) => {

    if (progress <= 0 || progress >= 100) return null;

    return (
        <div className={cn("w-full space-y-2 animate-in fade-in duration-300", className)}>
            <div className="flex justify-between items-center text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <span className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse" />
                    {label}
                </span>
                <span className="tabular-nums">{progress}%</span>
            </div>
            <Progress value={progress} className="h-2 bg-slate-100" />
        </div>
    );
};

export default UploadProgress;
