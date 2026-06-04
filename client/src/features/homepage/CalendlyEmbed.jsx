import React, { useEffect } from 'react';

const CalendlyEmbed = () => {

  const CALENDLY_URL = import.meta.env.VITE_CALENDLY_URL;

  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://assets.calendly.com/assets/external/widget.js';
    script.async = true;
    document.body.appendChild(script);

    return () => {
      document.body.removeChild(script);
    };
  }, []);

  return (
    <div
      className="h-[700px]"
      data-url= {CALENDLY_URL}
    />
  );
};

export default CalendlyEmbed;