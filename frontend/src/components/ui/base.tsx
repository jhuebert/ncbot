import { type ComponentPropsWithoutRef, forwardRef } from "react";
import { clsx } from "clsx";

// ── Button ──

const buttonVariants = {
  primary:
    "bg-primary-600 text-white hover:bg-primary-500 focus-visible:ring-primary-500",
  secondary:
    "bg-gray-800 text-gray-200 border border-gray-700 hover:bg-gray-700 hover:text-white",
  danger:
    "bg-red-600 text-white hover:bg-red-500 focus-visible:ring-red-500",
  ghost: "text-gray-400 hover:bg-gray-800 hover:text-gray-100",
};

const buttonSizes = {
  sm: "px-2.5 py-1.5 text-sm",
  md: "px-4 py-2 text-sm",
  lg: "px-5 py-2.5 text-base",
};

interface ButtonProps extends ComponentPropsWithoutRef<"button"> {
  variant?: keyof typeof buttonVariants;
  size?: keyof typeof buttonSizes;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { variant = "primary", size = "md", className, disabled, ...props },
    ref,
  ) => (
    <button
      ref={ref}
      disabled={disabled}
      className={clsx(
        "inline-flex items-center justify-center gap-1.5 rounded-md font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2",
        buttonVariants[variant],
        buttonSizes[size],
        disabled && "cursor-not-allowed opacity-50",
        className,
      )}
      {...props}
    />
  ),
);
Button.displayName = "Button";

// ── Input ──

interface InputProps extends ComponentPropsWithoutRef<"input"> {
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, ...props }, ref) => (
    <div className="w-full">
      <input
        ref={ref}
        className={clsx(
          "block w-full rounded-md border bg-gray-900 px-3 py-2 text-sm text-gray-100 shadow-sm transition-colors placeholder:text-gray-500",
          "focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none",
          error
            ? "border-red-500 focus:border-red-500 focus:ring-red-500"
            : "border-gray-700",
          className,
        )}
        {...props}
      />
      {error && <p className="mt-1 text-sm text-red-400">{error}</p>}
    </div>
  ),
);
Input.displayName = "Input";

// ── Textarea ──

interface TextareaProps extends ComponentPropsWithoutRef<"textarea"> {
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, error, ...props }, ref) => (
    <div className="w-full">
      <textarea
        ref={ref}
        className={clsx(
          "block w-full rounded-md border bg-gray-900 px-3 py-2 text-sm text-gray-100 shadow-sm transition-colors placeholder:text-gray-500",
          "focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none",
          error
            ? "border-red-500 focus:border-red-500 focus:ring-red-500"
            : "border-gray-700",
          className,
        )}
        {...props}
      />
      {error && <p className="mt-1 text-sm text-red-400">{error}</p>}
    </div>
  ),
);
Textarea.displayName = "Textarea";

// ── Label ──

interface LabelProps extends ComponentPropsWithoutRef<"label"> {
  required?: boolean;
}

export function Label({
  children,
  required,
  className,
  ...props
}: LabelProps) {
  return (
    <label
      className={clsx(
        "block text-sm font-medium text-gray-300",
        className,
      )}
      {...props}
    >
      {children}
      {required && <span className="ml-0.5 text-red-400">*</span>}
    </label>
  );
}

// ── Select ──

interface SelectProps extends ComponentPropsWithoutRef<"select"> {
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, error, children, ...props }, ref) => (
    <div className="w-full">
      <select
        ref={ref}
        className={clsx(
          "block w-full rounded-md border bg-gray-900 px-3 py-2 text-sm text-gray-100 shadow-sm transition-colors",
          "focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none",
          error
            ? "border-red-500 focus:border-red-500 focus:ring-red-500"
            : "border-gray-700",
          className,
        )}
        {...props}
      >
        {children}
      </select>
      {error && <p className="mt-1 text-sm text-red-400">{error}</p>}
    </div>
  ),
);
Select.displayName = "Select";

// ── Badge ──

interface BadgeProps {
  variant?: "default" | "success" | "warning" | "danger";
  children: React.ReactNode;
}

const badgeVariants = {
  default: "bg-gray-800 text-gray-300",
  success: "bg-emerald-500/10 text-emerald-400",
  warning: "bg-yellow-500/10 text-yellow-400",
  danger: "bg-red-500/10 text-red-400",
};

export function Badge({ variant = "default", children }: BadgeProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        badgeVariants[variant],
      )}
    >
      {children}
    </span>
  );
}

// ── Spinner ──

export function Spinner({ className }: { className?: string }) {
  return (
    <svg
      className={clsx("animate-spin h-5 w-5 text-primary-400", className)}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  );
}

// ── Table ──

export function Table({ children }: { children: React.ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="min-w-full divide-y divide-gray-800 text-sm">
        {children}
      </table>
    </div>
  );
}

export function THead({ children }: { children: React.ReactNode }) {
  return (
    <thead className="bg-gray-900">
      <tr>{children}</tr>
    </thead>
  );
}

export function Th({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <th
      className={clsx(
        "px-4 py-3 text-left font-semibold text-gray-300",
        className,
      )}
    >
      {children}
    </th>
  );
}

export function Td({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <td className={clsx("px-4 py-3 text-gray-400", className)}>
      {children}
    </td>
  );
}