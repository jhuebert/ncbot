import { Search } from "lucide-react";

interface SearchInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  ariaLabel?: string;
}

/**
 * Compact search input for list pages. Fires on every keystroke; pages are
 * expected to reset their page number when the value changes.
 */
export function SearchInput({
  value,
  onChange,
  placeholder = "Search…",
  ariaLabel = "Search",
}: SearchInputProps) {
  return (
    <div className="relative">
      <Search className="pointer-events-none absolute top-1/2 left-2.5 h-4 w-4 -translate-y-1/2 text-gray-500" />
      <input
        type="search"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
        }}
        placeholder={placeholder}
        aria-label={ariaLabel}
        className="w-56 rounded-md border border-gray-700 bg-gray-900 py-1.5 pr-3 pl-8 text-sm text-gray-100 placeholder:text-gray-500 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none"
      />
    </div>
  );
}
