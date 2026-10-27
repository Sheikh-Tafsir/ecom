/**
 * Recursively trims all strings in an object or array.
 * Converts empty strings or whitespace-only strings to null.
 *
 * @param {object|array} obj
 * @param {number} depth
 * @returns {object|array}
 */
const sanitize = (obj, depth = 0) => {
    if (!obj || typeof obj !== 'object' || depth > 10) return obj;

    if (Array.isArray(obj)) {
        for (let i = 0; i < obj.length; i++) {
            const item = obj[i];
            if (typeof item === 'string') {
                const trimmed = item.trim();
                obj[i] = trimmed.length > 0 ? trimmed : null;
            } else if (item && typeof item === 'object') {
                sanitize(item, depth + 1);
            }
        }
    } else {
        for (const key of Object.keys(obj)) {
            const value = obj[key];
            if (typeof value === 'string') {
                const trimmed = value.trim();
                obj[key] = trimmed.length > 0 ? trimmed : null;
            } else if (value && typeof value === 'object') {
                sanitize(value, depth + 1);
            }
        }
    }

    return obj;
};

const TrimInput = (req, res, next) => {
    if (req.body) sanitize(req.body);
    if (req.query) sanitize(req.query);
    if (req.params) sanitize(req.params);

    next();
};

TrimInput.sanitize = sanitize;

module.exports = TrimInput;
