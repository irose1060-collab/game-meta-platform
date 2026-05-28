import type { ChampionDetail } from "@/types/champion-detail";
import type { ChampionPosition } from "@/types/champion-stat";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function fetchChampionDetail({
  championId,
  position,
  patch,
}: {
  championId: number;
  position: ChampionPosition;
  patch?: string;
}): Promise<ChampionDetail> {
  const params = new URLSearchParams();

  params.set("championId", String(championId));
  params.set("position", position);

  if (patch) {
    params.set("patch", patch);
  }

  const response = await fetch(
    `${API_BASE_URL}/api/riot/stats/champions/detail?${params.toString()}`,
    {
      method: "GET",
      cache: "no-store",
    }
  );

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`챔피언 상세 조회 실패: ${response.status} ${text}`);
  }

  return response.json();
}