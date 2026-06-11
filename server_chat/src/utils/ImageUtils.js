const axios = require("axios");
const mime = require("mime-types");
const sharp = require('sharp');

const allowedImageTypes = ['image/jpg', 'image/jpeg', 'image/png', 'image/gif', 'image/webp'];

const changeImageByteToBase64 = (imageByte) => {
  const buffer = Buffer.from(imageByte);
  return "data:image/jpeg;base64," + buffer.toString('base64');
}

const processImageUrls = async (images) => {
  try {
    return await Promise.all(images.map(urlToGenerativePart));
  } catch (error) {
    console.error("processImageUrls | Error:", error);
    return { Error: "Error processing image URLs" };
  }
};

const urlToGenerativePart = async (url) => {
  try {
    const response = await axios.get(url, { responseType: "arraybuffer" });

    const mimeType = response.headers["content-type"] || mime.lookup(url);

    if (!mimeType || !mimeType.startsWith("image/")) {
      console.error("urlToGenerativePart | Unsupported MIME type:", mimeType);
      return { Error: "Unsupported image MIME type" };
    }

    return {
      inlineData: {
        data: binaryToBase64(response.data),
        mimeType,
      },
    };
  } catch (error) {
    console.error("urlToGenerativePart | Error fetching image:", error.message);
    return { Error: "Error fetching image from URL" };
  }
};

const processImageFiles = async (images) => {
  try {
    return await Promise.all(images.map(imageFileToGenerativeParts));
  } catch (error) {
    console.error("processImageFiles | Error:", error);
    return { Error: "Error processing image files" };
  }
};

const imageFileToGenerativeParts = async (image) => {
  try {
    return {
      inlineData: {
        data: Buffer.from(image.buffer).toString("base64"),
        mimeType: image.mimetype,
      },
    };
  } catch (error) {
    console.error("imageFileToGenerativeParts | Error:", error);
    return { Error: "Error processing image file" };
  }
};

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
  changeImageByteToBase64,
  // binaryToBase64,
  processImageUrls,
  processImageFiles,
  reduceImageBySize,
};
