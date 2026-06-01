export type User = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN" | string;
  token?: string;
  provider?: "LOCAL" | "GOOGLE" | "KAKAO" | "NAVER" | string;
  profileImageUrl?: string | null;
};

export type AuthResponse = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN" | string;
  token: string;
  provider?: "LOCAL" | "GOOGLE" | "KAKAO" | "NAVER" | string;
  profileImageUrl?: string | null;
};

export type RiotAccountResponse = {
  puuid: string;
  gameName: string;
  tagLine: string;
};

export type Notice = {
  id: number;
  cat?: string;
  category?: string;
  title: string;
  date: string;
  content: string;
};

export type AdminUser = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN" | string;
  status?: string;
  provider?: string;
  profileImageUrl?: string | null;
  createdAt?: string;
  lastLoginAt?: string | null;
};

export type NoticeResponse = {
  id: number;
  title: string;
  content: string;
  isPinned: boolean;
  status: string;
  viewCount: number;
  createdAt: string;
  updatedAt?: string | null;
};

export type DataCollectionLog = {
  id: number;
  jobName: string;
  status: string;
  startedAt?: string;
  endedAt?: string;
  totalCount?: number;
  successCount?: number;
  failCount?: number;
  errorMessage?: string | null;
  createdAt?: string;
};

export type HomeMetaResponse = {
  hotChampion: {
    name: string;
    nameKr: string;
    championKey: string;
    position: string;
    imageUrl?: string;
    winRate: number;
    pickRate: number;
    banRate: number;
    source?: string;
  };
  patchSummary: {
    version: string;
    summary: string;
    detail1: string;
    detail2: string;
    detail3: string;
    source?: string;
  };
  teamCompSummary: {
    apStatus: string;
    apRatio: number;
    ccStatus: string;
    ccScore: number;
    expectedWinRate: number;
    source?: string;
  };
  aiFeedbackSummary: {
    feedback1: string;
    feedback2: string;
    feedback3: string;
    source?: string;
  };
};

export type AssetDto = {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
};

export type MatchParticipantResponse = {
  puuid: string;
  summonerId: string;
  summonerName: string;

  riotGameName: string;
  riotTagLine: string;

  teamId: number;
  teamPosition: string;

  championName: string;
  championImageUrl: string;
  championLevel: number;

  win: boolean;

  kills: number;
  deaths: number;
  assists: number;
  kda: number;

  killParticipation: number;
  damageShare: number;

  totalCs: number;
  csPerMinute: number;

  goldEarned: number;
  totalDamageDealtToChampions: number;
  totalDamageTaken: number;

  visionScore: number;
  wardsPlaced: number;
  wardsKilled: number;
  controlWardsPlaced: number;

  opScore: number;
  opScoreRank: number;
  opScoreBadge: string;

  rankTier: string;

  items: AssetDto[];
  summonerSpells: AssetDto[];
  runes: AssetDto[];
};

export type MatchTeamSummaryResponse = {
  teamId: number;
  win: boolean;
  totalKills: number;
  totalGold: number;
  baronKills: number;
  dragonKills: number;
  riftHeraldKills: number;
  hordeKills: number;
  towerKills: number;
  inhibitorKills: number;
  bans: AssetDto[];
};

export type MatchSummaryResponse = {
  matchId: string;

  gameStartTimestamp: number;
  playedAtText: string;

  championName: string;
  championImageUrl: string;

  win: boolean;
  resultText: string;

  kills: number;
  deaths: number;
  assists: number;
  kda: number;

  killParticipation: number;

  position: string;
  gameMode: string;
  queueType: string;
  queueId: number;

  gameDurationSeconds: number;
  gameDurationText: string;

  totalCs: number;
  csPerMinute: number;

  goldEarned: number;
  totalDamageDealtToChampions: number;
  totalDamageTaken: number;
  visionScore: number;

  wardsPlaced: number;
  wardsKilled: number;
  controlWardsPlaced: number;

  opScore: number;
  opScoreRank: number;
  opScoreBadge: string;

  rankTier: string;

  items: AssetDto[];
  summonerSpells: AssetDto[];
  runes: AssetDto[];

  blueTeam: MatchParticipantResponse[];
  redTeam: MatchParticipantResponse[];

  blueTeamTotalKills: number;
  redTeamTotalKills: number;
  blueTeamTotalGold: number;
  redTeamTotalGold: number;

  maxDamage: number;

  blueTeamSummary: MatchTeamSummaryResponse;
  redTeamSummary: MatchTeamSummaryResponse;
};

export type MatchSearchResponse = {
  gameName: string;
  tagLine: string;
  puuid: string;

  totalMatches: number;
  wins: number;
  losses: number;
  winRate: number;
  averageKda: number;

  matches: MatchSummaryResponse[];
};