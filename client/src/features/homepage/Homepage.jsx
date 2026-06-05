import NavigationBar from '@/components/common/NavigationBar'
import { useState, useEffect } from "react";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import Footer from '@/components/common/Footer';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"
import CardData from '@/features/homepage/HomepageCardData';
import HomepageAccordionData from '@/features/homepage/HomepageAccordionData';
import { API_PATH } from '@/services/http/Axios.js';

const backgroundImages = [
  "/img/pexels-pixabay-207891.jpg",
  "/img/pexels-magda-ehlers-1319572.jpg",
  "/img/pexels-markus-spiske-168866.jpg",
]

const Homepage = () => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentIndex((prevIndex) => (prevIndex + 1) % backgroundImages.length);
    }, 3000); // Change image every 3 seconds

    return () => clearInterval(interval); // Clean up interval on unmount
  }, []);

  return (
    <div className='bg-gradient-to-br from-blue-50 to-indigo-100'>
      <div
        className="w-[100%] [height:min(710px,100vh)] flex overflow-hidden bg-cover bg-center transition-all duration-1000"
        style={{ backgroundImage: `url(${backgroundImages[currentIndex]})` }}
      >
        <div className='flex'>
          <div className='my-auto w-[80%] lg:w-[90%] pl-[10%]'>
            <h1 className='text-3xl lg:text-5xl mb-8 leading-[1.2] text-white'>Empower Your Learning Journey</h1>
            <p className='mb-8 text-white text-sm lg:text-md xl:text-base'>All the essential tools and resources you need to study smarter, stay organized, and succeed..</p>
            <div className='flex'>
              <Button className="bg-blue-600 mr-4 hover:bg-blue-700">
                <a href="#works">Get Started</a>
              </Button>
              <Button className="bg-white text-blue-600 hover:bg-gray-100">
                <a href={`${API_PATH}/common/download-file?filename=something.sql`} download>
                  {/* Download Report */}
                  Know more
                </a>
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Homepage