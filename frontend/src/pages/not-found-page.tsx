import { Link } from "react-router-dom";
import { FileQuestion } from "lucide-react";

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-20">
      <FileQuestion className="h-12 w-12 text-gray-600" />
      <h1 className="text-xl font-semibold text-gray-100">Page Not Found</h1>
      <p className="text-sm text-gray-500">
        The page you&apos;re looking for doesn&apos;t exist.
      </p>
      <Link
        to="/channels"
        className="inline-flex items-center justify-center gap-1.5 rounded-md bg-gray-800 px-4 py-2 text-sm font-medium text-gray-200 border border-gray-700 hover:bg-gray-700 hover:text-white transition-colors"
      >
        Go to Channels
      </Link>
    </div>
  );
}