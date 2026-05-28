export type AnalyticsSummary = {
  matchCount: number;
  participantCount: number;
  championStatCount: number;
  seedPlayerCount: number;
  rankingPlayerCount: number;
  latestPatch: string;
  analyzedPickCount: number;
  minGames: number;
};

export type PositionDistribution = {
  position: string;
  pickCount: number;
  percentage: number;
};

export type AnalyticsChampion = {
  patch: string;
  queueId: number;
  position: string;
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
  tier: string;
};

export type PositionTopChampions = {
  position: string;
  champions: AnalyticsChampion[];
};

export type AnalyticsOverview = {
  summary: AnalyticsSummary;
  positionDistribution: PositionDistribution[];
  positionTopChampions: PositionTopChampions[];
  topWinRateChampions: AnalyticsChampion[];
  topPickRateChampions: AnalyticsChampion[];
  topDamageChampions: AnalyticsChampion[];
  topKdaChampions: AnalyticsChampion[];
  scatterChampions: AnalyticsChampion[];
};
