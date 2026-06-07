import React, {useEffect, useState, useRef} from 'react'
import {X} from 'lucide-react';

import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import StaredLabel from './StaredLabel';
import {handleClientSideError, MAX_FILE_SIZE} from "@/utils/index.js";
import InputError from "@/components/common/InputError.jsx";

const MultiImageInput = ({onChangeImages, maxImages = 5, errors = {}, setError}) => {
    const inputRef = useRef(null);

    const [images, setImages] = useState([]);

    const createKey = (file) => `${file.name}-${file.size}-${file.lastModified}`;

    useEffect(() => {
        return () => {
            images.forEach(file => URL.revokeObjectURL(file));
        };
    }, [images]);

    const handleImageChange = (e) => {
        const selectedFiles = Array.from(e.target.files || []);
        setError([]);

        const availableSlots = maxImages - images.length;
        if (availableSlots <= 0) {
            handleClientSideError("images", `You can only upload up to ${maxImages} images.`, setError);
            return;
        }

        const existingKeys = new Set(images.map((img) => createKey(img.file)));

        const validFiles = [];
        let skippedDuplicate = 0;
        let skippedSize = 0;

        for (const file of selectedFiles) {
            if (validFiles.length >= availableSlots) {
                break
            }

            const key = createKey(file);

            if (existingKeys.has(key)) {
                skippedDuplicate++;
                continue;
            }

            if (file.size > MAX_FILE_SIZE) {
                skippedSize++;
                continue;
            }

            validFiles.push({
                file,
                previewUrl: URL.createObjectURL(file),
            });

            existingKeys.add(key);
        }

        const updatedImages = [...images, ...validFiles];

        setImages(updatedImages);
        onChangeImages(updatedImages.map((img) => img.file));

        if (skippedDuplicate || skippedSize) {
            handleClientSideError("images",
                [
                    skippedDuplicate
                        ? `${skippedDuplicate} duplicate file(s) skipped.`
                        : "",
                    skippedSize
                        ? `${skippedSize} file(s) too large (max ${MAX_FILE_SIZE / (1024 * 1024)}MB).`
                        : "",
                ]
                    .filter(Boolean)
                    .join(" "),
                setError
            );
        }

        e.target.value = "";
    };

    const removeImage = (indexToRemove) => {
        setImages((prev) => {
            const removed = prev[indexToRemove];
            URL.revokeObjectURL(removed.previewUrl);

            const updated = prev.filter((_, i) => i !== indexToRemove);

            onChangeImages(updated.map((img) => img.file));

            return updated;
        });
    };

    const handleUploadClick = () => {
        inputRef.current?.click();
    };

    return (
        <div className='space-y-2'>
            <StaredLabel label="Images"/>

            <Input
                type="file"
                accept="image/*"
                multiple
                onChange={handleImageChange}
                ref={inputRef}
                className="hidden"
            />

            <Button
                type="button"
                variant="outline"
                onClick={handleUploadClick}
                className="align-left w-full"
            >
                {images?.length > 0 ? 'Upload more images' : 'Upload images'}
            </Button>

            {images.length > 0 && (
                <div className="flex gap-4 overflow-x-auto py-2">
                    {images.map((img, index) => (
                        <div key={createKey(img.file)} className="relative min-w-[100px] max-w-[160px]">
                            <img
                                src={img.previewUrl}
                                alt={`preview-${index}`}
                                className="h-[120px] w-[120px] object-cover rounded-md border aspect-square"
                            />

                            <Button
                                type="button"
                                size="icon"
                                onClick={() => removeImage(index)}
                                className="absolute -top-2 -right-2 bg-blue-600 text-white rounded-full w-5 h-5 text-xs flex items-center justify-center"
                            >
                                <X/>
                            </Button>
                        </div>
                    ))}
                </div>
            )}

            <InputError errors={errors} field={"images"}/>
        </div>
    )
}

export default React.memo(MultiImageInput)