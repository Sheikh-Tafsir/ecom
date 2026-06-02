import React, { useCallback, useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { PaginationEllipsis } from '@/components/ui/pagination'
import { FIRST_PAGE, getQueryString } from '@/utils/PaginationUtils';

const PaginationButton = ({ totalPages = 1 }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const queryParams = useMemo(
        () => Object.fromEntries(searchParams.entries()),
        [searchParams]
    );

    const currentPage = useMemo(() => {
        const parsed = parseInt(queryParams.page || FIRST_PAGE, 10);
        return isNaN(parsed) ? FIRST_PAGE : Math.min(parsed, totalPages);
    }, [queryParams.page, totalPages]);

    const isFirstPage = currentPage === FIRST_PAGE;
    const isLastPage = currentPage === totalPages;

    const handlePagination = useCallback(
        (page) => {
            const nextPage = Math.min(Math.max(page, FIRST_PAGE), totalPages);
            const navQueryParams = { ...queryParams, page: nextPage };
            navigate(getQueryString(navQueryParams));
        },
        [navigate, queryParams, totalPages]
    );

    /** Helper to render numbered buttons */
    const renderPageButton = useCallback(
        (pageNumber, isActive = false) => (
            <Button
                key={pageNumber}
                variant={isActive ? "default" : "outline"}
                aria-current={isActive ? "page" : undefined}
                onClick={() => !isActive && handlePagination(pageNumber)}
                className={isActive ? "bg-primary text-primary-foreground" : ""}
            >
                {pageNumber}
            </Button>
        ),
        [handlePagination]
    );

    if (totalPages <= 1) return null;

    return (
        <div className='flex'>
            <div className="mt-6 flex space-x-1 mx-auto">
                {/* Previous Button */}
                <Button
                    variant="outline"
                    onClick={() => handlePagination(currentPage - 1)}
                    disabled={isFirstPage}
                    aria-label="Previous page"
                >
                    <ChevronLeft className="w-4 h-4 mr-1" />
                    Previous
                </Button>

                {/* First page link, show if not the first page */}
                {currentPage > FIRST_PAGE && (
                    <>
                        {renderPageButton(FIRST_PAGE)}
                        {currentPage > FIRST_PAGE + 1 && <PaginationEllipsis />}
                    </>
                )}

                {/* Previous Page */}
                {currentPage > FIRST_PAGE + 1 && renderPageButton(currentPage - 1)}

                {/* Current Page */}
                {renderPageButton(currentPage, true)}

                {/* Next Page */}
                {currentPage < totalPages && renderPageButton(currentPage + 1)}

                {/* Last Page */}
                {currentPage < totalPages - 1 && (
                    <>
                        {currentPage + 1 < totalPages - 1 && <PaginationEllipsis />}
                        {renderPageButton(totalPages)}
                    </>
                )}

                {/* Next Button */}
                <Button
                    variant="outline"
                    onClick={() => handlePagination(currentPage + 1)}
                    disabled={isLastPage}
                    aria-label="Next page"
                >
                    Next
                    <ChevronRight className="w-4 h-4 ml-1" />
                </Button>
            </div>
        </div>
    );
}

export default React.memo(PaginationButton);