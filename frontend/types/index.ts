export type User = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN" | string;
  token?: string;
};

export type AuthResponse = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN" | string;
  token: string;
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
