import { useEffect, useRef } from "react";
import { clsx } from "clsx";
import { Spinner } from "./ui/base";

interface InfiniteScrollProps {
  /** Fired when the user scrolls near the sentinel and more data is available. */
  onLoadMore: () => void;
  /** Whether a subsequent page still exists on the server. */
  hasMore: boolean;
  /** Whether a subsequent page fetch is currently in flight. */
  isLoadingMore: boolean;
  /**
   * Where the "load more" sentinel lives. Use "bottom" for standard lists
   * and "top" for chat-style views where newer content is at the bottom and
   * you scroll up to reach older items.
   */
  sentinel?: "top" | "bottom";
  /** Scroll to the bottom when the container first mounts (chat views). */
  scrollToBottomOnMount?: boolean;
  /** Label shown next to the loading spinner while fetching more. */
  loadingLabel?: string;
  /** Applied to the scrollable box (e.g. a max-height). */
  className?: string;
  children: React.ReactNode;
}

/**
 * A bounded scroll container that fetches the next page of data as the user
 * scrolls to the sentinel edge. Falls back to plain rendering in environments
 * without IntersectionObserver (e.g. jsdom), so tests keep working.
 */
export function InfiniteScroll({
  onLoadMore,
  hasMore,
  isLoadingMore,
  sentinel = "bottom",
  scrollToBottomOnMount = false,
  loadingLabel,
  className,
  children,
}: InfiniteScrollProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const onLoadMoreRef = useRef(onLoadMore);
  onLoadMoreRef.current = onLoadMore;

  // Jump to the newest content on first mount (chat views).
  useEffect(() => {
    if (scrollToBottomOnMount && containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [scrollToBottomOnMount]);

  // Observe the sentinel and trigger the next fetch when it comes into view.
  useEffect(() => {
    const container = containerRef.current;
    const sentinelEl = sentinelRef.current;
    if (
      !container ||
      !sentinelEl ||
      typeof IntersectionObserver === "undefined"
    ) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          onLoadMoreRef.current();
        }
      },
      { root: container, rootMargin: "200px 0px", threshold: 0 },
    );
    observer.observe(sentinelEl);
    return () => {
      observer.disconnect();
    };
  }, []);

  const showLoading =
    hasMore &&
    isLoadingMore && (
      <div className="flex items-center justify-center gap-2 py-3 text-sm text-gray-500">
        <Spinner className="h-4 w-4" />
        <span>{loadingLabel ?? "Loading more…"}</span>
      </div>
    );

  return (
    <div
      ref={containerRef}
      className={clsx("overflow-y-auto", className)}
      data-infinite-scroll
    >
      {sentinel === "top" && <div ref={sentinelRef} className="min-h-px" />}
      {sentinel === "top" && showLoading}
      {children}
      {sentinel === "bottom" && showLoading}
      {sentinel === "bottom" && <div ref={sentinelRef} className="min-h-px" />}
    </div>
  );
}
