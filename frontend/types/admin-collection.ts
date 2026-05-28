export type AdminSeedStatus = {
  id: number;
  gameName: string;
  tagLine: string;
  enabled: boolean;
  lastCollectedAt: string | null;
  totalSavedMatches: number;
  totalFailedCount: number;
  lastResultMessage: string | null;
};

export type AdminCollectionStatus = {
  matchCount: number;
  participantCount: number;
  championStatCount: number;
  seedPlayerCount: number;
  enabledSeedPlayerCount: number;
  rankingPlayerCount: number;

  totalSavedMatchesBySeeds: number;
  failedSeedCount: number;

  lastCollectedAt: string | null;
  lastMatchCreatedAt: string | null;
  lastStatsUpdatedAt: string | null;

  autoCollectEnabled: boolean;
  matchCountPerPlayer: number;
  maxPlayersPerCycle: number;
  fixedDelayMs: number;
  delayBetweenPlayersMs: number;

  recentSeeds: AdminSeedStatus[];
};