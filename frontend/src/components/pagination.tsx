import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./ui/base";

interface PaginationProps {
  currentPage?: number;
  totalPages?: number;
  totalElements?: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  currentPage = 0,
  totalPages = 0,
  totalElements = 0,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const start = currentPage * 25 + 1;
  const end = Math.min((currentPage + 1) * 25, totalElements);

  return (
    <nav
      role="navigation"
      aria-label="Pagination"
      className="flex items-center justify-between gap-4 pt-4"
    >
      <p className="text-sm text-gray-500">
        Showing {start}–{end} of {totalElements}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
          Previous
        </Button>
        <span className="text-sm text-gray-400">
          Page {currentPage + 1} of {totalPages}
        </span>
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage >= totalPages - 1}
          onClick={() => onPageChange(currentPage + 1)}
          aria-label="Next page"
        >
          Next
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </nav>
  );
}