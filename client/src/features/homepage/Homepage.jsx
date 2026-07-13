import { useState, useEffect } from "react";
import { ChevronRight, ArrowRight } from "lucide-react";
import { Button } from '@/components/ui/button';
import { API_PATH } from '@/services/http/Axios.js';
import { cn } from "@/lib/utils";

const backgroundImages = [
  "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=2071&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=2070&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?q=80&w=2070&auto=format&fit=crop",
]

const Homepage = () => {
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentIndex((prevIndex) => (prevIndex + 1) % backgroundImages.length);
    }, 5000); 

    return () => clearInterval(interval);
  }, []);

  return (
    <div className='min-h-screen bg-slate-50 flex flex-col'>
      {/* Hero Section */}
      <div className="relative h-[100vh] min-h-[600px] w-full overflow-hidden flex items-center">
        {/* Background Images with Crossfade */}
        {backgroundImages.map((img, idx) => (
          <div 
            key={idx}
            className={cn(
              "absolute inset-0 bg-cover bg-center transition-opacity duration-1000 ease-in-out",
              currentIndex == idx ? "opacity-100 scale-100" : "opacity-0 scale-110"
            )}
            style={{ backgroundImage: `url(${img})` }}
          />
        ))}

        {/* Overlay for Readability */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950/80 via-slate-950/40 to-transparent" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full z-10">
          <div className="max-w-2xl animate-in fade-in slide-in-from-left-8 duration-1000">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/20 border border-blue-400/30 backdrop-blur-md mb-6">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
              </span>
              <span className="text-[10px] font-bold text-blue-100 uppercase tracking-widest">New Courses Available</span>
            </div>

            <h1 className="text-5xl md:text-7xl font-black text-white leading-[1.1] tracking-tight mb-6">
              Empower Your <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-300">Learning</span> Journey
            </h1>

            <p className="text-lg md:text-xl text-slate-200 mb-10 leading-relaxed max-w-xl">
              Access premium tools, expert resources, and a supportive community to help you study smarter and achieve your goals faster.
            </p>

            <div className="flex flex-wrap gap-4">
              <Button className="h-14 px-8 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-base shadow-xl shadow-blue-600/20 group transition-all hover:scale-105 active:scale-95">
                <a href="#works" className="flex items-center gap-2">
                  Get Started
                  <ChevronRight className="h-5 w-5 group-hover:translate-x-1 transition-transform" />
                </a>
              </Button>
              <Button className="h-14 px-8 rounded-2xl bg-white/10 hover:bg-white/20 text-white border border-white/30 backdrop-blur-md font-bold text-base transition-all hover:scale-105 active:scale-95">
                <a href={`${API_PATH}/common/download-file?filename=something.sql`} download className="flex items-center gap-2">
                  View Demo
                  <ArrowRight className="h-4 w-4 opacity-70" />
                </a>
              </Button>
            </div>

            <div className="mt-12 flex items-center gap-6">
              <div className="flex -space-x-3">
                {[1,2,3,4].map(i => (
                  <div key={i} className="w-10 h-10 rounded-full border-2 border-slate-900 bg-slate-800 flex items-center justify-center overflow-hidden">
                    <img src={`https://i.pravatar.cc/100?u=${i}`} alt="user" />
                  </div>
                ))}
                <div className="w-10 h-10 rounded-full border-2 border-slate-900 bg-blue-600 flex items-center justify-center text-[10px] font-bold text-white">
                  +2k
                </div>
              </div>
              <p className="text-sm text-slate-400 font-medium">
                Joined by over <span className="text-white font-bold">2,000+</span> active students
              </p>
            </div>
          </div>
        </div>

        {/* Decorative elements */}
        <div className="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-slate-50 to-transparent z-10" />
      </div>
    </div>
  );
}

export default Homepage