import React from 'react'
import {Outlet} from 'react-router-dom'

import Footer from '@/components/common/Footer'
import NavigationBar from '@/components/common/NavigationBar'

const PublicRoute = () => {
    return (
        <>
            <NavigationBar/>
            <main className="bg-slate-50 from-blue-50 to-indigo-100 pb-8">
                <Outlet/>
            </main>
            <Footer/>
        </>
    )
}


export default PublicRoute