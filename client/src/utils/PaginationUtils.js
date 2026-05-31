export const FIRST_PAGE = 1;
export const DEFAULT_PAGE_SIZE = 10;
export const DEFAULT_SORT_BY = "createdAt";
export const DEFAULT_SORT_ORDER = "desc";

export const getDefaultPaginationParams = () => ({
  page: FIRST_PAGE - 1,
  size: DEFAULT_PAGE_SIZE,
  sortBy: DEFAULT_SORT_BY,
  sortOrder: DEFAULT_SORT_ORDER,
  search: "",
});

export const getQueryString = (params = {}) => {
  // Merge provided params with pagination defaults
  const mergedParams = {
    ...getDefaultPaginationParams(),
    ...params,
  };

  // Convert 1-based page (frontend) to 0-based page (Spring)
  if (mergedParams.page && mergedParams.page > 0) {
    mergedParams.page = mergedParams.page - 1;
  }

  // Remove nullish and empty-string values
  const filteredEntries = Object.entries(mergedParams).filter(
    ([, value]) => value !== undefined && value !== null && value !== ""
  );

  // If no valid params exist, return empty string
  if (filteredEntries.length === 0) return "";

  // Build query string safely
  const query = new URLSearchParams(Object.fromEntries(filteredEntries)).toString();

  return `?${query}`;
};
