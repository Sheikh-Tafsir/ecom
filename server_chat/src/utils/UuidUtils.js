const { v7: uuidv7 } = require('uuid');

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const generateUuidV7 = () => uuidv7();

const isValidUuid = (value) => {
    return typeof value === 'string' && UUID_REGEX.test(value);
};

module.exports = {
    generateUuidV7,
    isValidUuid,
    UUID_REGEX,
};
