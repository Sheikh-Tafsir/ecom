const sharp = require('sharp');

const allowedImageTypes = ['image/jpg', 'image/jpeg', 'image/png', 'image/gif', 'image/webp'];

const reduceImageBySize = async (imageBuffer) => {
  const originalSizeKB = Buffer.byteLength(imageBuffer) / 1024;

  // Determine quality and resize options based on size
  let quality = 90;
  let width = null;

  if (originalSizeKB > 5000) {        // >5MB
    quality = 60;
    width = 1280;
  } else if (originalSizeKB > 3000) { // >3MB
    quality = 65;
    width = 1024;
  } else if (originalSizeKB > 1000) { // >1MB
    quality = 75;
    width = 800;
  } else {
    quality = 85;
    width = null; // don't resize small images
  }

  let sharpInstance = sharp(imageBuffer).jpeg({ quality });

  if (width) {
    sharpInstance = sharpInstance.resize({
      width,
      fit: 'inside',
      withoutEnlargement: true
    });
  }

  return await sharpInstance.toBuffer();
};

module.exports = {
  allowedImageTypes,
  reduceImageBySize,
};
