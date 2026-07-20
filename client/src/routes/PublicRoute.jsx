import React from 'react'
import {Outlet} from 'react-router-dom'

import Footer from '@/common/Footer.jsx'
import NavigationBar from '@/common/NavigationBar.jsx'

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