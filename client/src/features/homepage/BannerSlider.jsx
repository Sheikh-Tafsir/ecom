import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Axios } from "@/services/http/Axios";
import {
    Carousel,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from "@/components/ui/carousel";
import { Skeleton } from "@/components/ui/skeleton";

const getActiveBanners = async () => {
    const response = await Axios.get("/banners/active");
    return response.data.data;
};

const BannerSlider = () => {
    const { data: banners, isLoading } = useQuery({
        queryKey: ['banners', 'active'],
        queryFn: getActiveBanners
    });

    if (isLoading) {
        return <Skeleton className="w-full h-[400px] rounded-xl" />;
    }

    if (!banners || banners.length == 0) {
        return null;
    }

    return (
        <section className="w-full">
            <Carousel className="w-full" opts={{ loop: true }}>
                <CarouselContent>
                    {banners.map((banner) => (
                        <CarouselItem key={banner.id}>
                            <div className="relative h-[300px] md:h-[450px] w-full overflow-hidden rounded-xl">
                                <img
                                    src={banner.imageUrl}
                                    alt={banner.title}
                                    className="h-full w-full object-cover"
                                />
                                <div className="absolute inset-0 bg-black/40 flex flex-col justify-center px-10 text-white">
                                    <h2 className="text-3xl md:text-5xl font-bold mb-4">{banner.title}</h2>
                                    {banner.subtitle && <p className="text-xl md:text-2xl mb-8">{banner.subtitle}</p>}
                                    {banner.linkUrl && (
                                        <a
                                            href={banner.linkUrl}
                                            className="bg-white text-black px-6 py-3 rounded-lg font-bold w-fit hover:bg-slate-100 transition-colors"
                                        >
                                            Learn More
                                        </a>
                                    )}
                                </div>
                            </div>
                        </CarouselItem>
                    ))}
                </CarouselContent>
                <CarouselPrevious className="left-4" />
                <CarouselNext className="right-4" />
            </Carousel>
        </section>
    );
};

export default BannerSlider;
