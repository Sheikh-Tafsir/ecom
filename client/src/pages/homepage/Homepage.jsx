import NavigationBar from '@/mycomponents/NavigationBar'
import { useState, useEffect } from "react";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import Footer from '@/mycomponents/Footer';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"
import CardData from '@/pages/homepage/HomepageCardData';
import HomepageAccordionData from '@/pages/homepage/HomepageAccordionData';
import { API_PATH } from '@/middleware/Axios.js';

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
      <NavigationBar />
      <div
        className="w-[100%] [height:min(710px,100vh)] flex overflow-hidden pt-[80px] bg-cover bg-center transition-all duration-1000"
        style={{ backgroundImage: `url(${backgroundImages[currentIndex]})` }}
      >
        <div className='flex'>
          <div className='my-auto w-[80%] lg:w-[90%] pl-[10%]'>
            {/* <h1 className='text-3xl lg:text-5xl mb-8 leading-[1.2] text-white'>Understand Developmental Differences Through Eye Gaze</h1>
            <p className='mb-8 text-white text-sm lg:text-md xl:text-base'>Our advanced AI analyzes natural eye movements to offer early insights into social communication patterns — empowering families and clinicians with accessible, non-invasive screening.</p> */}
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

      <div className='flex flex-wrap justify-between bg-white px-[10%] lg:px-[20%] pt-10' id="works">
        <div className='w-[100%] lg:w-[44%] my-auto'>
          <h2 className='text-2xl lg:text-4xl mb-4 font-semibold'>How are system works</h2>
          <p className='text-xs lg:text-md'>Lorem ipsum dolor sit amet consectetur adipisicing elit. Aliquam rerum neque inventore libero pariatur est nobis accusantium nostrum saepe commodi sapiente sequi architecto expedita consequuntur blanditiis deleniti enim, totam illo!</p>
        </div>
        <img src="/img/feature_set.png" className='w-[100% lg:w-[54%] rounded-lg' />
      </div>

      <div className='px-[10%] lg:px-[15%] py-14 bg-gray-100'>
        <h2 className='text-center text-2xl lg:text-4xl pb-8 font-semibold'>Gaze heatmap difference</h2>
        <div className='flex flex-wrap justify-between gap-3.5'>
          {CardData.map((card, index) => (
            <Card key={index} className="w-[300px]">
              <CardHeader>
                <img src={card.imgSrc} alt={card.title} />
              </CardHeader>
              <CardContent className="text-center">
                <CardTitle className="text-black pb-4 text-lg lg:text-xl">{card.title}</CardTitle>
                <p className='text-sm lg:text-base'>{card.description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      <div className='w-full bg-white py-12'>
        <h2 className='text-center text-2xl lg:text-4xl mb-6 font-semibold'>Questionnaire</h2>
        <Accordion type="single" collapsible className="w-[320px] lg:w-[650px] 2xl:w-[800px] mx-auto px-10 rounded-lg">
          {HomepageAccordionData.map((item) => (
            <AccordionItem key={item.value} value={item.value} className="mb-4 text-xs lg:text-base xl:text-xl">
              <AccordionTrigger className="text-black">{item.question}</AccordionTrigger>
              <AccordionContent className="border-b-2">{item.answer}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>

      {/* <CalendlyEmbed /> */}
      <Footer />
    </div>
  );
}

export default Homepage