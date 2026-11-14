/**
 * Centralized display-value safety utilities.
 *
 * Use these in JSX instead of sprinkling `?? 0` / `|| "-"` everywhere.
 * They guard against null, undefined, and (for text) empty-string values
 * so the UI never renders the word "undefined" or shows `$NaN`.
 */

/**
 * Safe text display — returns fallback for null / undefined / empty-string.
 * @param {*} value
 * @param {string} fallback
 * @returns {string}
 *
 * @example <span>{text(user.name)}</span>          // "-" if missing
 * @example <span>{text(user.name, "Unknown")}</span>
 */
export const text = (value, fallback = "-") =>
    value != null && value !== "" ? value : fallback;

/**
 * Safe numeric display — returns fallback for null / undefined / NaN.
 * @param {*} value
 * @param {number} fallback
 * @returns {number}
 *
 * @example <span>{num(item.quantity)} Left</span>
 */
export const num = (value, fallback = 0) => {
    const n = Number(value);
    return value != null && !Number.isNaN(n) ? n : fallback;
};

/**
 * Safe price display — "$12.50" or "$0.00".
 * @param {*} value
 * @param {string} currency  symbol prepended
 * @param {number} decimals  decimal places (default 2)
 * @returns {string}
 *
 * @example <span>{price(item.totalPrice)}</span>      // "$0.00"
 * @example <span>{price(item.profit, "TK")}</span>    // "TK450.00"
 */
export const price = (value, currency = "$", decimals = 2) => {
    const n = Number(value);
    const safe = value != null && !Number.isNaN(n) ? n : 0;
    return `${currency}${safe.toFixed(decimals)}`;
};

/**
 * Safe image src — returns a placeholder when the URL is falsy.
 * @param {*} url
 * @param {string} fallback
 * @returns {string}
 *
 * @example <img src={img(product.image)} />
 */
export const img = (url, fallback = "/placeholder.svg") =>
    url || fallback;

/**
 * Safe alt text for images.
 * @param {*} value
 * @param {string} fallback
 * @returns {string}
 *
 * @example <img alt={alt(item.productName)} />
 */
export const alt = (value, fallback = "Image") =>
    value != null && value !== "" ? String(value) : fallback;

/**
 * Safe string for .toLowerCase() / .includes() etc. — prevents TypeError.
 * @param {*} value
 * @returns {string}
 *
 * @example user.filter(u => str(u.name).toLowerCase().includes(term))
 */
export const str = (value) =>
    value != null ? String(value) : "";
