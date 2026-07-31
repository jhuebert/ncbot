import createClient from "openapi-fetch";
import type { paths } from "./schema";

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public detail?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export const client = createClient<paths>({
  baseUrl: typeof window !== "undefined" ? window.location.origin : "http://localhost:3000",
  fetch: (input) => globalThis.fetch(input),
});

/**
 * Parse a fetch Response into a typed ApiError.
 * Handles problem+json, plain json, and plain text error bodies.
 */
export async function parseApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get("content-type") ?? "";
  let detail: string | undefined;

  try {
    if (contentType.includes("application/problem+json")) {
      const body = (await response.json()) as {
        detail?: string;
        title?: string;
      };
      detail = body.detail ?? body.title;
    } else if (contentType.includes("application/json")) {
      const body = (await response.json()) as Record<string, unknown>;
      detail = typeof body.detail === "string" ? body.detail : undefined;
    } else {
      const text = await response.text();
      if (text) detail = text.slice(0, 500);
    }
  } catch {
    // ignore parse failures
  }

  return new ApiError(
    detail ?? `Request failed with status ${response.status}`,
    response.status,
    detail,
  );
}