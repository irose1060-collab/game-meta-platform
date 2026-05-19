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
