import type { ChampionPosition, ChampionStat } from "@/types/champion-stat";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function fetchChampionStats(
  position?: ChampionPosition
): Promise<ChampionStat[]> {
  const params = new URLSearchParams();

  if (position) {
    params.set("position", position);
  }

  const url = `${API_BASE_URL}/api/riot/stats/champions${
    params.toString() ? `?${params.toString()}` : ""
  }`;

  const response = await fetch(url, {
    method: "GET",
    cache: "no-store",
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`챔피언 통계 조회 실패: ${response.status} ${text}`);
  }

  return response.json();
}