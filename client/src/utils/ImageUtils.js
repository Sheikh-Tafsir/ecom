import imageCompression from 'browser-image-compression';

const defaultOptions = {
    maxSizeMB: 1,            // Max size in MB
    maxWidthOrHeight: 1280,  // Max dimension
    useWebWorker: true       // Multithreading for performance
};

/**
 * Compresses a single image file.
 * @param {File} file 
 * @param {Object} options 
 * @returns {Promise<File>}
 */
export const compressImage = async (file, options = {}) => {
    if (!file) return null;

    try {
        return await imageCompression(file, { ...defaultOptions, ...options });
    } catch (error) {
        console.error('Image compression failed:', error);
        return file; // Fallback to original
    }
};

/**
 * Compresses an array or Set of image files.
 * @param {File[]|Set<File>} files 
 * @param {Object} options 
 * @returns {Promise<File[]>}
 */
export const compressImages = async (files, options = {}) => {
    if (!files) return [];
    
    const fileList = Array.from(files);
    return Promise.all(fileList.map(file => compressImage(file, options)));
};
