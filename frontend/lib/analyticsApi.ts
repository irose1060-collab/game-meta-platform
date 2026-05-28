import type { AnalyticsOverview } from "@/types/analytics";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function fetchAnalyticsOverview({
  patch,
  minGames = 10,
}: {
  patch?: string;
  minGames?: number;
} = {}): Promise<AnalyticsOverview> {
  const params = new URLSearchParams();

  if (patch) {
    params.set("patch", patch);
  }

  params.set("minGames", String(minGames));

  const response = await fetch(
    `${API_BASE_URL}/api/riot/analytics/overview?${params.toString()}`,
    {
      method: "GET",
      cache: "no-store",
    }
  );

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`통계 분석 데이터 조회 실패: ${response.status} ${text}`);
  }

  return response.json();
}
