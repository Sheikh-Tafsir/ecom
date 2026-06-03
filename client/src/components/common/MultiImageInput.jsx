import React, { useEffect, useState, useRef } from 'react'
import { X } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import StaredLabel from './StaredLabel';
import {MAX_FILE_SIZE} from "@/utils/index.js";

const MultiImageInput = ({ onChangeImages, maxImages, errors }) => {
    const imageInputRef = useRef(null);

    const [images, setImages] = useState([]);
    const [error, setError] = useState(errors?.images || {});

    useEffect(() => {
        return () => {
            images.forEach(file => URL.revokeObjectURL(file));
        };
    }, [images]);

    const handleImageUploadClick = () => {
        imageInputRef.current?.click();
    };

    const handleImageChange = (e) => {
        const selectedFiles = Array.from(e.target.files);
        const availableSlots = maxImages - images.length;

        const validFiles = [];
        let skippedDueToSize = false;

        for (const file of selectedFiles) {
            if (validFiles.length >= availableSlots) {
                setError({
                    ...prevErrors,
                    images: `You can only upload up to ${maxImages} images.`,
                });

                return;
            }

            if (file.size <= MAX_FILE_SIZE) {
                validFiles.push(file);
            } else {
                skippedDueToSize = true;
            }
        }

        const newImageList = [...images, ...validFiles];
        setImages(newImageList);

        onChangeImages(newImageList);

        if (skippedDueToSize) {
            setError({
                ...prev,
                images: `Some images were too large and were skipped. Max size: ${MAX_FILE_SIZE / (1024 * 1024)}MB.`,
            });
        } else {
            setError({});
        }

        if (imageInputRef.current) {
            imageInputRef.current.value = '';
        }
    };

    const removeImage = (indexToRemove) => {
        setImages((prevImages) => prevImages.filter((_, index) => index !== indexToRemove));
    };

    return (
        <div className='space-y-1'>
            <StaredLabel label="Images" />
            <Input
                type="file"
                accept="image/*"
                multiple
                onChange={handleImageChange}
                ref={imageInputRef}
                className="hidden"
            />

            {images?.length < 5 &&
                <Button
                    type="button"
                    variant="outline"
                    onClick={handleImageUploadClick}
                    className="align-left"
                >
                    {images?.length > 0 ? 'Upload more images' : 'Upload images'}
                </Button>
            }

            {images.length > 0 && (
                <div className="flex space-x-4 overflow-x-auto py-2">
                    {images.map((file, index) => (
                        <div key={index} className="relative min-w-[100px] max-w-[160px]">
                            <img
                                src={URL.createObjectURL(file)}
                                alt={`preview-${index}`}
                                className="max-h-[150px] object-cover rounded-md border"
                            />
                            <Button
                                type="button"
                                size="icon"
                                onClick={() => removeImage(index)}
                                className="absolute -top-2 -right-2 bg-blue-600 text-white rounded-full w-5 h-5 text-xs flex items-center justify-center"
                            >
                                <X />
                            </Button>
                        </div>
                    ))}
                </div>
            )}
            {error.images && <p className='validation-error'>{error.images}</p>}
        </div>
    )
}

export default React.memo(MultiImageInput)