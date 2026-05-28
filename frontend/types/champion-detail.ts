import type { ChampionStat } from "@/types/champion-stat";

export type SpellStat = {
  spell1Id: number;
  spell2Id: number;
  games: number;
  wins: number;
  winRate: number;
};

export type ItemStat = {
  itemId: number;
  games: number;
  wins: number;
  winRate: number;
};

export type CounterStat = {
  enemyChampionId: number;
  enemyChampionName: string;
  games: number;
  wins: number;
  winRate: number;
};

export type ChampionDetail = {
  basic: ChampionStat;
  spells: SpellStat[];
  items: ItemStat[];
  hardCounters: CounterStat[];
  easyMatchups: CounterStat[];
};