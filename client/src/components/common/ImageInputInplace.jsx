import React, { useRef } from 'react';
import { BookImage } from 'lucide-react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { validateFile } from '@/utils';

export default function ImageInputInplace({ image, setImage }) {
  const fileInputRef = useRef(null);

  const handleImageChange = ({ target: { files } }) => {
    const file = files[0];
    if (validateFile(file)) {
      //console.log("yes");
      setImage(file);
    } else {
      console.log("File too large, select an image smaller than 5MB.");
      alert("File too large, select an image smaller than 5MB.");
    }
  };

  const handleImageChangeClicked = () => {
    fileInputRef.current?.click();
  };

  return (
    <>
      <Input
        name="image"
        type="file"
        accept="image/*"
        onChange={handleImageChange}
        className="hidden"
        ref={fileInputRef}
      />

      {image ? (
        <div
          className="w-10 h-10 rounded overflow-hidden shadow-lg cursor-pointer"
          onClick={handleImageChangeClicked}
        >
          <img src={URL.createObjectURL(image)} className="w-full h-full object-cover" alt="Preview" />
        </div>
      ) : (
        <Button type="button" size="icon" onClick={handleImageChangeClicked} className="bg-blue-900">
          <BookImage className="h-5 w-5" />
          <span className="sr-only">Upload image</span>
        </Button>
      )}
    </>
  );
}
