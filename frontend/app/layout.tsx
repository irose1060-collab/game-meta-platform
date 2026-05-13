import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "META GG — 승리를 위한 LoL 데이터 분석 플랫폼",
  description: "리그 오브 레전드 전적 검색, 메타 분석, 팀 조합 분석, AI 피드백 플랫폼",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
