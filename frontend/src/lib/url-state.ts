import { useSearchParams } from "react-router-dom";
import { useCallback } from "react";

/**
 * Read a numeric search param with a default.
 * Returns [value, setter] like useState but backed by URL search params.
 */
export function usePageParam(
  key: string,
  defaultValue: number,
): [number, (v: number) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const raw = searchParams.get(key);
  const value = raw ? parseInt(raw, 10) : defaultValue;
  const valid = isNaN(value) || value < 0 ? defaultValue : value;

  const setter = useCallback(
    (v: number) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (v === defaultValue) {
            next.delete(key);
          } else {
            next.set(key, String(v));
          }
          return next;
        },
        { replace: true },
      );
    },
    [key, defaultValue, setSearchParams],
  );

  return [valid, setter];
}

export function useStringParam(
  key: string,
  defaultValue: string,
): [string, (v: string) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const value = searchParams.get(key) ?? defaultValue;

  const setter = useCallback(
    (v: string) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (v === defaultValue) {
            next.delete(key);
          } else {
            next.set(key, v);
          }
          return next;
        },
        { replace: true },
      );
    },
    [key, defaultValue, setSearchParams],
  );

  return [value, setter];
}

export function useBooleanParam(
  key: string,
): [boolean | undefined, (v: boolean | undefined) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const raw = searchParams.get(key);
  const value: boolean | undefined =
    raw === "true" ? true : raw === "false" ? false : undefined;

  const setter = useCallback(
    (v: boolean | undefined) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (v === undefined) {
            next.delete(key);
          } else {
            next.set(key, String(v));
          }
          // Reset page when filter changes
          next.delete("page");
          return next;
        },
        { replace: true },
      );
    },
    [key, setSearchParams],
  );

  return [value, setter];
}

/**
 * Read a `query` search param used for list search. The setter updates the
 * query and clears the page param in a single navigation, so the two changes
 * compose (react-router's function updater reads a stale snapshot when
 * multiple setters run in one event batch, so calling two setters separately
 * would clobber the first change).
 */
export function useSearchQuery(): [string, (v: string) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const value = searchParams.get("query") ?? "";

  const setter = useCallback(
    (v: string) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (v) {
            next.set("query", v);
          } else {
            next.delete("query");
          }
          // Reset page when search changes
          next.delete("page");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  return [value, setter];
}
