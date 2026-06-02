import React from 'react'
import { Outlet} from 'react-router-dom'

import Footer from '@/components/common/Footer'
import NavigationBar from '@/components/common/NavigationBar'

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