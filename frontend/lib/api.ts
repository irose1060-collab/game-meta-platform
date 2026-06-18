export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

function buildApiUrl(path: string) {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

function isNetworkError(error: unknown) {
  return (
    error instanceof TypeError ||
    (error instanceof Error && error.message.toLowerCase().includes("fetch"))
  );
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(buildApiUrl(path), {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(options?.headers || {}),
      },
    });
  } catch (error) {
    if (isNetworkError(error)) {
      throw new Error(
        "백엔드 서버에 연결하지 못했습니다. 배포 주소와 NEXT_PUBLIC_API_BASE_URL을 확인해주세요."
      );
    }

    throw error;
  }

  const contentType = response.headers.get("content-type") || "";

  let data: unknown;
  if (contentType.includes("application/json")) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    if (typeof data === "object" && data !== null && "message" in data) {
      const message = String((data as { message?: string }).message);
      throw new Error(toUserFriendlyApiError(message));
    }

    if (typeof data === "object" && data !== null && "error" in data) {
      const message = String((data as { error?: string }).error);
      throw new Error(toUserFriendlyApiError(message));
    }

    throw new Error(
      typeof data === "string" && data
        ? toUserFriendlyApiError(data)
        : "요청 처리 중 오류가 발생했습니다."
    );
  }

  return data as T;
}

function toUserFriendlyApiError(message: string) {
  const lower = message.toLowerCase();

  if (lower.includes("forbidden") || lower.includes("unauthorized")) {
    return "Riot API 키가 만료되었거나 권한이 없습니다. 서버 환경변수 RIOT_API_KEY를 확인해주세요.";
  }

  if (lower.includes("rate limit") || lower.includes("429")) {
    return "Riot API 요청 제한에 걸렸습니다. 잠시 후 다시 시도해주세요.";
  }

  if (lower.includes("not found") || lower.includes("404")) {
    return "해당 Riot ID를 찾지 못했습니다. 소환사명과 태그를 확인해주세요.";
  }

  if (lower.includes("timeout") || lower.includes("timed out")) {
    return "응답 시간이 초과되었습니다. 서버 상태를 확인한 뒤 다시 시도해주세요.";
  }

  return message || "요청 처리 중 오류가 발생했습니다.";
}
