import React, { useMemo, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import { PaginationEllipsis } from "@/components/ui/pagination";
import { FIRST_PAGE, getQueryString } from "@/utils/PaginationUtils";

const clamp = (value, min, max) =>
    Math.min(Math.max(value, min), max);

const PaginationButton = ({ totalPages = 0 }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    // convert query params (cheap, no need for useMemo)
    const queryParams = Object.fromEntries(searchParams.entries());

    const currentPage = useMemo(() => {
        const page = Number(queryParams.page);
        return clamp(
            Number.isNaN(page) ? FIRST_PAGE : page,
            FIRST_PAGE,
            totalPages || FIRST_PAGE
        );
    }, [queryParams.page, totalPages]);

    const canRender = totalPages > 1;

    const goToPage = useCallback(
        (page) => {
            if (!canRender) return;

            const nextPage = clamp(page, FIRST_PAGE, totalPages);

            navigate(
                getQueryString({
                    ...queryParams,
                    page: nextPage,
                })
            );
        },
        [navigate, queryParams, totalPages, canRender]
    );

    if (!canRender) return null;

    const isFirst = currentPage === FIRST_PAGE;
    const isLast = currentPage === totalPages;

    const PageButton = ({ page, active = false }) => (
        <Button
            variant={active ? "default" : "outline"}
            aria-current={active ? "page" : undefined}
            onClick={() => !active && goToPage(page)}
            className={active ? "bg-primary text-primary-foreground" : ""}
        >
            {page}
        </Button>
    );

    return (
        <div className="flex mt-6 justify-center space-x-1">
            {/* Prev */}
            <Button
                variant="outline"
                onClick={() => goToPage(currentPage - 1)}
                disabled={isFirst}
            >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Previous
            </Button>

            {/* First */}
            {currentPage > FIRST_PAGE && (
                <>
                    <PageButton page={FIRST_PAGE} />
                    {currentPage > FIRST_PAGE + 1 && <PaginationEllipsis />}
                </>
            )}

            {/* Prev page */}
            {currentPage > FIRST_PAGE + 1 && (
                <PageButton page={currentPage - 1} />
            )}

            {/* Current */}
            <PageButton page={currentPage} active />

            {/* Next page */}
            {currentPage < totalPages && (
                <PageButton page={currentPage + 1} />
            )}

            {/* Last */}
            {currentPage < totalPages - 1 && (
                <>
                    {currentPage + 2 < totalPages && <PaginationEllipsis />}
                    <PageButton page={totalPages} />
                </>
            )}

            {/* Next */}
            <Button
                variant="outline"
                onClick={() => goToPage(currentPage + 1)}
                disabled={isLast}
            >
                Next
                <ChevronRight className="w-4 h-4 ml-1" />
            </Button>
        </div>
    );
};

export default React.memo(PaginationButton);