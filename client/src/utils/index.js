export * from './AuthUtils';
export * from './DateUtils';
export * from './ErrorUtils';
export * from './PaginationUtils';
export * from './Utils';

export const LOCAL_STORAGE_CART = 'visoredCart';
export const URL_NOT_FOUND = '/not-found';

export const isInvalidPage = (page, totalPages) => {
  if (!totalPages) return false;
  return page < 1 || page > totalPages;
};
