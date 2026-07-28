export const PRODUCT_SORTBY = Object.freeze({
    NEWEST: {label: "Newest", value: "createdAt,DESC"},
    OLDEST: {label: "Oldest", value: "createdAt,ASC"},
    PRICE_LOW: {label: "Price: Low to High", value: "price,ASC"},
    PRICE_HIGH: {label: "Price: High to Low", value: "price,DESC"},
    NAME_ASC: {label: "Name: A to Z", value: "name,ASC"},
});
