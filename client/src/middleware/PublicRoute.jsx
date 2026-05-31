import React from 'react'
import { Outlet} from 'react-router-dom'

import Footer from '@/mycomponents/Footer'
import NavigationBar from '@/mycomponents/NavigationBar'

const PublicRoute = () => {
    return(
        <>
          <NavigationBar />
          <Outlet/> 
          <Footer />
        </>
    )
}


export default PublicRoute