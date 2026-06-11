const sanitize = (obj) => {
    if (!obj || typeof obj !== 'object') return;

    for (const key of Object.keys(obj)) {
        const value = obj[key];

        if (typeof value === 'string') {
            const trimmed = value.trim();
            obj[key] = trimmed || null;
        } else if (Array.isArray(value)) {
            for (let i = 0; i < value.length; i++) {
                if (typeof value[i] === 'string') {
                    const trimmed = value[i].trim();
                    value[i] = trimmed || null;
                } else if (value[i] && typeof value[i] === 'object') {
                    sanitize(value[i]);
                }
            }
        } else if (value && typeof value === 'object') {
            sanitize(value);
        }
    }
};

const TrimInput = (req, res, next) => {
    sanitize(req.body);
    sanitize(req.query);
    sanitize(req.params);

    next();
};

module.exports = TrimInput;