import React, {useEffect, useRef, useState} from "react";
import {X} from "lucide-react";

import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import StaredLabel from "./StaredLabel";
import InputError from "@/components/common/InputError";

import {
    handleClientSideError,
    MAX_FILE_SIZE, ONE_MB,
} from "@/utils";
import {Label} from "@/components/ui/label.jsx";

const MultiImageInput = ({
                             existingImages = [],
                             onExistingImagesChange,
                             onImagesChange,
                             maxImages = 5,
                             errors,
                             setError,
                             label = "Images",
                             isRequired = false
                         }) => {

    const inputRef = useRef(null);

    const [images, setImages] = useState([]);

    const createKey = (file) => `${file.name}-${file.size}-${file.lastModified}`;

    useEffect(() => {
        return () => {
            images.forEach((img) =>
                URL.revokeObjectURL(img.previewUrl)
            );
        };
    }, [images]);

    const handleChange = (e) => {
        const selectedFiles = Array.from(e.target.files || []);

        const totalImages = existingImages.length + images.length + selectedFiles.length;

        if (totalImages > maxImages) {
            handleClientSideError(
                "images",
                `Maximum ${maxImages} images allowed. You tried to add ${selectedFiles.length} more.`,
                setError
            );
            return;
        }

        const validImages = [];
        const skippedFiles = [];

        selectedFiles.forEach((file) => {
            if (file.size > MAX_FILE_SIZE) {
                skippedFiles.push({ name: file.name, reason: "too large" });
            } else if (isDuplicate(file)) {
                skippedFiles.push({ name: file.name, reason: "already added" });
            } else {
                validImages.push({
                    file,
                    previewUrl: URL.createObjectURL(file),
                });
            }
        });

        if (skippedFiles.length > 0) {
            const errorMsg = skippedFiles
                .map(f => `"${f.name}" (${f.reason})`)
                .join(", ");
            handleClientSideError(
                "images",
                `Skipped ${skippedFiles.length} files: ${errorMsg}. Max size is ${MAX_FILE_SIZE / ONE_MB}MB.`,
                setError
            );
        }

        const updatedImages = [...images, ...validImages];

        setImages(updatedImages);
        onImagesChange(updatedImages.map((img) => img.file));

        e.target.value = "";
    };

    const removeUploadedImage = (index) => {
        const updated = images.filter((_, i) => i !== index);

        setImages(updated);

        onImagesChange(updated.map((img) => img.file));
    };

    const removeExistingImage = (id) => {
        const updated = existingImages.filter(
            (img) => img.id !== id
        );

        onExistingImagesChange(updated);
    };

    const isDuplicate = (file) =>
        images.some(img =>
            img.file.name == file.name &&
            img.file.size == file.size &&
            img.file.lastModified == file.lastModified
        );

    return (
        <div className="space-y-2">
            {isRequired ?
                <StaredLabel label={label}/>
                : <Label htmlFor="images">{label}</Label>
            }

            <Input
                type="file"
                multiple
                accept="image/*"
                className="hidden"
                ref={inputRef}
                onChange={handleChange}
            />

            <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={() => inputRef.current?.click()}
            >
                Upload Images
            </Button>

            {(existingImages.length > 0 || images.length > 0) && (
                <div className="flex gap-3 overflow-x-auto">
                    {existingImages.map((img) => (
                        <div
                            key={img.id}
                            className="relative"
                        >
                            <img
                                src={img.image}
                                alt=""
                                className="w-[120px] h-[120px] object-cover rounded-md border"
                            />

                            <Button
                                type="button"
                                size="icon"
                                className="absolute -top-2 -right-2 w-5 h-5 rounded-full"
                                onClick={() =>
                                    removeExistingImage(img.id)
                                }
                            >
                                <X size={14}/>
                            </Button>
                        </div>
                    ))}

                    {images.map((img, index) => (
                        <div
                            key={createKey(img.file)}
                            className="relative space-y-1"
                        >
                            <img
                                src={img.previewUrl}
                                alt=""
                                className="w-[120px] h-[120px] object-cover rounded-md border"
                            />

                            <Button
                                type="button"
                                size="icon"
                                className="absolute -top-2 -right-2 w-5 h-5 rounded-full"
                                onClick={() =>
                                    removeUploadedImage(index)
                                }
                            >
                                <X size={14}/>
                            </Button>

                            <p className="text-[10px] text-slate-500 font-medium truncate w-[120px]" title={img.file.name}>
                                {img.file.name}
                            </p>
                        </div>
                    ))}
                </div>
            )}

            <InputError errors={errors} field="images"/>
        </div>
    );
};

export default React.memo(MultiImageInput);