import React, {useEffect, useRef, useState} from "react";
import {X} from "lucide-react";

import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import StaredLabel from "./StaredLabel";
import InputError from "@/components/common/InputError";

import {
    handleClientSideError,
    MAX_FILE_SIZE,
    ONE_MB,
} from "@/utils";
import {Label} from "@/components/ui/label.jsx";

const ImageInput = ({
                        existingImage,
                        onExistingImageChange,
                        onImageChange,
                        error,
                        setError,
                        label = "Image",
                        isRequired = false
                    }) => {
    const inputRef = useRef(null);

    const [image, setImage] = useState(null);

    useEffect(() => {
        return () => {
            if (image?.previewUrl) {
                URL.revokeObjectURL(image.previewUrl);
            }
        };
    }, [image]);

    const handleChange = (e) => {
        const file = e.target.files?.[0];

        if (!file) return;

        if (file.size > MAX_FILE_SIZE) {
            handleClientSideError(
                "image",
                `Maximum ${MAX_FILE_SIZE / ONE_MB}MB file is allowed`,
                setError
            );
            return;
        }

        setImage({
            file,
            previewUrl: URL.createObjectURL(file) || null,
        });
        onImageChange(file);
        onExistingImageChange(null)

        e.target.value = "";
    };

    const removeUploadedImage = () => {
        setImage(null);
        onImageChange(null);
    };

    return (
        <div className="space-y-2">
            {isRequired ?
                <StaredLabel label="Image"/>
                : <Label htmlFor="image">Image</Label>
            }

            <Input
                ref={inputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleChange}
            />

            <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={() => inputRef.current?.click()}
            >
                Upload Image
            </Button>

            {existingImage && !image &&
                <div className="relative">
                    <img
                        src={existingImage}
                        alt=""
                        className="w-[60%] object-cover rounded-md border"
                    />

                    <Button
                        type="button"
                        size="icon"
                        className="absolute -top-2 -right-2 w-5 h-5 rounded-full"
                        onClick={() =>
                            onExistingImageChange(null)
                        }
                    >
                        <X size={14}/>
                    </Button>
                </div>
            }

            {image &&
                <div className="relative">
                    <img
                        src={image.previewUrl}
                        alt=""
                        className="w-[60%] object-cover rounded-md border"
                    />

                    <Button
                        type="button"
                        size="icon"
                        className="absolute -top-2 -right-2 w-5 h-5 rounded-full"
                        onClick={() => removeUploadedImage()}
                    >
                        <X size={14}/>
                    </Button>
                </div>}

            <InputError errors={error} field="image"/>
        </div>
    );
};

export default React.memo(ImageInput);