export const API_BASE_URL = "http://localhost:8080";

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers || {}),
    },
  });

  const contentType = response.headers.get("content-type") || "";

  let data: unknown;
  if (contentType.includes("application/json")) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    if (typeof data === "object" && data !== null && "message" in data) {
      throw new Error(String((data as { message?: string }).message));
    }
    if (typeof data === "object" && data !== null && "error" in data) {
      throw new Error(String((data as { error?: string }).error));
    }
    throw new Error(typeof data === "string" && data ? data : "요청 처리 중 오류가 발생했습니다.");
  }

  return data as T;
}
