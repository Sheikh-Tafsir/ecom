const TABLE_PAGINATION_SIZE = 10;

const pageCount = (rowCount) => {
    return Math.floor((rowCount + TABLE_PAGINATION_SIZE - 1) / TABLE_PAGINATION_SIZE);
}

const isNull = (value) => {
    return !value || false || value === "" || value === undefined;
}

const isNotNull = (value) => {
    return !isNull(value);
}

module.exports = {
    TABLE_PAGINATION_SIZE,
    pageCount,
    isNull,
    isNotNull,
}
