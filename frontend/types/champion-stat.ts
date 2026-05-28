export type ChampionPosition = "TOP" | "JUNGLE" | "MIDDLE" | "BOTTOM" | "UTILITY";

export type ChampionStat = {
  patch: string;
  queueId: number;
  position: ChampionPosition;
  championId: number;
  championName: string;
  games: number;
  wins: number;
  winRate: number;
  pickRate: number;
  avgKda: number;
  avgDamage: number;
  avgGold: number;
  avgCs: number;
  avgVisionScore: number;
  tierScore: number;
  tier: "S" | "A" | "B" | "C" | "D" | "N/A";
};