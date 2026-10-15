import React from 'react';
import SEO from '@/common/SEO';

const AboutUs = () => {
    return (
        <div className="container mx-auto py-10 px-4 max-w-4xl">
            <SEO
                title="About Us | Aspire"
                description="Learn about Aspire's mission to provide exceptional shopping experiences, premium curated products, and innovative technology."
                canonicalUrl="/about"
                ogType="website"
            />
            <h1 className="text-4xl font-bold text-slate-900 mb-10 tracking-tighter uppercase">
                About Us
            </h1>
            <div className="prose prose-slate max-w-none prose-headings:font-bold prose-headings:tracking-tight prose-a:text-blue-600 font-medium text-slate-700 leading-relaxed">
                <p>Welcome to our E-Commerce platform. We are dedicated to providing the best shopping experience for our customers.</p>
                <h2>Our Mission</h2>
                <p>Our mission is to deliver high-quality products with exceptional customer service. We believe in transparency, innovation, and customer satisfaction.</p>
                <h2>Our Story</h2>
                <p>Started as a small project, we have grown into a full-scale microservices-based platform, showcasing the latest in web technology and real-time features.</p>
            </div>
        </div>
    );
};

export default AboutUs;
