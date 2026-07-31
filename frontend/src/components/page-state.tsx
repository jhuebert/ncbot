import { AlertCircle, RefreshCw, Inbox } from "lucide-react";
import { Button, Spinner } from "./ui/base";

interface PageStateProps {
  isLoading: boolean;
  error: Error | null;
  isEmpty: boolean;
  loadingText?: string;
  emptyText?: string;
  children: React.ReactNode;
  onRetry?: () => void;
}

export function PageState({
  isLoading,
  error,
  isEmpty,
  loadingText = "Loading…",
  emptyText = "No results found.",
  children,
  onRetry,
}: PageStateProps) {
  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20 text-gray-500">
        <Spinner />
        <p className="text-sm">{loadingText}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20">
        <AlertCircle className="h-8 w-8 text-red-400" />
        <p className="text-sm text-red-400">
          {error.message || "An error occurred."}
        </p>
        {onRetry && (
          <Button variant="secondary" size="sm" onClick={onRetry}>
            <RefreshCw className="h-4 w-4" />
            Retry
          </Button>
        )}
      </div>
    );
  }

  if (isEmpty) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20 text-gray-500">
        <Inbox className="h-8 w-8" />
        <p className="text-sm">{emptyText}</p>
      </div>
    );
  }

  return <>{children}</>;
}

export function ErrorMessage({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-20">
      <AlertCircle className="h-8 w-8 text-red-400" />
      <p className="text-sm text-red-400">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          <RefreshCw className="h-4 w-4" />
          Retry
        </Button>
      )}
    </div>
  );
}