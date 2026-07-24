import React, { useState, useEffect } from 'react'
import { Input } from "@/components/ui/input.jsx"
import { Facebook, Instagram, Twitter, Youtube } from "lucide-react"
import { Link } from "react-router-dom"
import { APP_NAME } from '@/utils/index.js'

const Footer = () => {

  useEffect(() => {
    const componentSlide = () => {
      var sectionId = window.location.hash.substring(1);

      // Check if the section ID exists and corresponds to an element on the page
      if (sectionId) {
        var targetSection = document.getElementById(sectionId);

        // Scroll to the target section smoothly
        if (targetSection) {
          targetSection.scrollIntoView({
            behavior: 'auto'
          });
        }
      }
    }

    setTimeout(() => {
      componentSlide();
    }, 150);
  }, []);

  return (
    <footer className="text-gray-300 bg-blue-900">
      <div className="max-w-6xl mx-auto px-4 py-10">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="mb-8 md:mb-0">
            {/* <h2 className="text-2xl font-bold mb-4">বিশ্বনাথ পৌরসভা</h2> */}
            <div className='flex mb-4'>
              <img src="/navbar/icon3.png" className="h-12 w-12" alt="logo" />
              <p className='text-white my-auto ml-2 text-2xl'>{APP_NAME}</p>
            </div>
            <p className="text-sm">
              Empowering lives with understanding, one unique mind at a time.
            </p>
          </div>
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Pages</h3>
            <div className="space-y-2 flex flex-col">
              <Link to="/" className="hover:text-white">
                Home
              </Link>
              <Link to="/about" className="hover:text-white">
                About Us
              </Link>
              <Link to="/privacy" className="hover:text-white">
                Privacy Policy
              </Link>
              <Link to="/terms" className="hover:text-white">
                Terms & Conditions
              </Link>
            </div>
          </div>
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Social Media</h3>
            <div className="flex space-x-4">
              <a href="#" className="hover:text-white transition-colors">
                <Facebook size={24} />
                <span className="sr-only">Facebook</span>
              </a>
              <a href="#" className="hover:text-white transition-colors">
                <Twitter size={24} />
                <span className="sr-only">Twitter</span>
              </a>
              <a href="#" className="hover:text-white transition-colors">
                <Instagram size={24} />
                <span className="sr-only">Instagram</span>
              </a>
              <a href="#" className="hover:text-white transition-colors">
                <Youtube size={24} />
                <span className="sr-only">YouTube</span>
              </a>
            </div>
          </div>
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Newsletter</h3>
            <p className="text-sm mb-4">For the latest news stay with us</p>
            <form className="flex flex-col sm:flex-row gap-2">
              <Input
                type="email"
                placeholder="Your email"
                className="text-black border-gray-700 focus:border-gray-600"
                value="190041130tafsir@gmail.com"
                disabled={true}
              />
            </form>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-8 pt-8 text-sm text-center">
          <p>&copy; {new Date().getFullYear()} {APP_NAME}. All rights reserved.</p>
        </div>
      </div>
    </footer>
  )
}

export default Footer;