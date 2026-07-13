import {ArrowLeft} from 'lucide-react'
import React from 'react'
import {Button} from '../ui/button'
import { useNavigate } from 'react-router-dom';

export const BackButton = ({url}) => {
    const navigate = useNavigate();

    return (
        <>
            <Button
                variant="ghost"
                onClick={() => navigate(url || -1)}
                className="group -ml-4 text-slate-500 hover:text-blue-600 font-bold transition-all"
            >
                <ArrowLeft className="w-4 h-4 mr-2 transition-transform group-hover:-translate-x-1"/>
                Back to Inventory
            </Button>
        </>
    )
}
