import React from 'react'
import {Outlet} from 'react-router-dom'

import Footer from '@/components/common/Footer'
import NavigationBar from '@/components/common/NavigationBar'

const PublicRoute = () => {
    return (
        <>
            <NavigationBar/>
            <main className="bg-gradient-to-br from-blue-50 to-indigo-100 pb-8" style={{paddingTop: '70px'}}>
                <Outlet/>
            </main>
            <Footer/>
        </>
    )
}


export default PublicRoute