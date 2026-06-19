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

  latestPatch: string;
  latestPatchMatchCount: number;
  latestPatchStatRows: number;
  latestPatchTotalGames: number;
  targetPatchGames: number;
  latestPatchProgressPercent: number;

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

export type AutoCollectionRunResult = {
  success: boolean;
  manual: boolean;
  jobName: string;
  latestPatch: string;
  targetGameCount: number;
  latestPatchMatchCount: number;
  latestPatchTotalGames: number;
  latestPatchProgressPercent: number;
  processedSeedCount: number;
  savedMatchCount: number;
  skippedExistingMatchCount: number;
  failedMatchCount: number;
  savedParticipantCount: number;
  rebuiltStatCount: number;
  startedAt: string;
  endedAt: string;
  message: string;
};

export type StatsRebuildResult = {
  rebuiltCount: number;
  message: string;
};
