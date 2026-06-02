import React, { useRef, useEffect, useState } from 'react';
import { Input } from '@/components/ui/input.jsx';
import { Label } from '@/components/ui/label.jsx';
import { Button } from '@/components/ui/button.jsx';
import { MAX_FILE_SIZE } from '@/utils';

const ImageInput = ({ existingImage, onImageChange, error }) => {
  const fileRef = useRef();
  const [previewUrl, setPreviewUrl] = useState(null);

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (file && file.size <= MAX_FILE_SIZE) {
      onImageChange(file);
      setPreviewUrl(URL.createObjectURL(file));
    } else {
      onImageChange(null); // clear if invalid
    }
  };

  // Cleanup preview URL when component unmounts or image changes
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleClick = () => {
    fileRef.current?.click();
  };

  const displayImage = previewUrl || existingImage;

  return (
    <div className="flex flex-col space-y-1.5">
      <Label htmlFor="image">Image</Label>

      <Input
        type="file"
        accept="image/*"
        ref={fileRef}
        onChange={handleFileSelect}
        className="hidden"
      />

      <div className="w-[60%]">
        {displayImage && (
          <img
            src={displayImage}
            alt="Profile Preview"
            className="rounded-sm"
          />
        )}
      </div>

      <Button
        type="button"
        variant="outline"
        onClick={handleClick}
        className="align-left"
      >
        {displayImage ? 'Change image' : 'Upload image'}
      </Button>

      {error && <p className="validation-error">{error}</p>}
    </div>
  );
};

export default ImageInput;
