import { Button } from '@/components/ui/button';
import BannerSlider from "@/features/homepage/BannerSlider";
import Faqs from "./Faqs";
import { useNavigate } from "react-router-dom";

const Homepage = () => {
  const navigate = useNavigate();

  return (
    <div className='min-h-screen bg-slate-50 flex flex-col'>
      <div className="container mx-auto px-4 py-8">
        <BannerSlider />
      </div>

      <div className="relative h-[600px] w-full overflow-hidden flex items-center bg-slate-900 rounded-3xl mt-10 mx-auto max-w-7xl shadow-2xl">
        <div className="absolute inset-0 bg-cover bg-center opacity-40"
             style={{ backgroundImage: `url(https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=2071&auto=format&fit=crop)` }}
        />
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/60 to-transparent" />

        <div className="relative max-w-7xl mx-auto px-10 w-full z-10">
          <div className="max-w-2xl">
            <h1 className="text-5xl md:text-7xl font-bold text-white leading-[1.1] tracking-tight mb-6">
              Welcome to <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-300">E-Commerce</span>
            </h1>
            <p className="text-lg text-slate-200 mb-10 leading-relaxed max-w-xl">
              Discover our premium collection of products and enjoy a seamless shopping experience.
            </p>
            <Button className="h-14 px-8 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-base"
              onClick={() => navigate("/products")}>
                Shop Now
            </Button>
          </div>
        </div>
      </div>

        <Faqs />
    </div>
  );
}

export default Homepage