"use client";

import { useState } from "react";
import type { MatchSummaryResponse } from "@/types";

type AiWinAnalysisResult = {
  gameName: string;
  tagLine: string;
  totalMatches: number;
  wins: number;
  losses: number;
  winRate: number;
  averageKda: number;
  averageKills: number;
  averageDeaths: number;
  averageAssists: number;
  averageDamage: number;
  averageCs: number;
  averageGold: number;
  summary: string;
  strengths: string[];
  weaknesses: string[];
  recommendations: string[];
};

type WinAnalysisCardProps = {
  gameName: string;
  tagLine: string;
  totalMatches?: number;
  wins?: number;
  losses?: number;
  matches?: MatchSummaryResponse[];
};

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function WinAnalysisCard({
  gameName,
  tagLine,
  totalMatches = 0,
  wins = 0,
  losses = 0,
  matches = [],
}: WinAnalysisCardProps) {
  const safeMatches = matches ?? [];

  const [analysis, setAnalysis] = useState<AiWinAnalysisResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const analyze = async () => {
    if (safeMatches.length === 0) {
      setError("분석할 최근 경기 데이터가 없습니다.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await fetch(`${API_BASE_URL}/api/ai/gemini-win-analysis`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          gameName,
          tagLine,
          totalMatches,
          wins,
          losses,
          matches: safeMatches.map((match) => ({
            win: match.win,
            championName: match.championName,
            position: match.position,
            kills: match.kills,
            deaths: match.deaths,
            assists: match.assists,
            kda: match.kda,
            totalDamageDealtToChampions: match.totalDamageDealtToChampions,
            totalCs: match.totalCs,
            goldEarned: match.goldEarned,
            queueType: match.queueType,
            playedAtText: match.playedAtText,
            gameDurationText: match.gameDurationText,
          })),
        }),
      });

      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Gemini 분석 요청에 실패했습니다.");
      }

      const data = (await response.json()) as AiWinAnalysisResult;
      setAnalysis(data);
    } catch (err) {
      setAnalysis(null);
      setError(
        err instanceof Error
          ? err.message
          : "Gemini AI 분석 중 오류가 발생했습니다."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <section
      style={{
        marginTop: 20,
        padding: 18,
        borderRadius: 18,
        border: "1px solid rgba(245,197,66,0.24)",
        background:
          "linear-gradient(135deg, rgba(245,197,66,0.1), rgba(255,255,255,0.035))",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 14,
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <div>
          <p
            style={{
              margin: 0,
              color: "#f5c542",
              fontSize: 13,
              fontWeight: 900,
              letterSpacing: "0.08em",
            }}
          >
            GEMINI AI ANALYSIS
          </p>

          <h3 style={{ margin: "6px 0 0", color: "#fff" }}>
            Gemini 승률 분석 리포트
          </h3>

          <p style={{ margin: "6px 0 0", color: "#cbd5e1", fontSize: 14 }}>
            최근 경기 데이터를 Gemini가 분석해 강점, 약점, 개선 방향을 생성합니다.
          </p>
        </div>

        <button
          type="button"
          onClick={analyze}
          disabled={loading || safeMatches.length === 0}
          style={{
            border: "none",
            borderRadius: 999,
            padding: "11px 18px",
            background: "#f5c542",
            color: "#111827",
            fontWeight: 900,
            cursor:
              loading || safeMatches.length === 0 ? "not-allowed" : "pointer",
            opacity: loading || safeMatches.length === 0 ? 0.65 : 1,
          }}
        >
          {loading ? "Gemini 분석 중..." : "Gemini 분석 실행"}
        </button>
      </div>

      {safeMatches.length === 0 && (
        <p style={{ marginTop: 14, color: "#fca5a5", fontWeight: 700 }}>
          전적 검색 결과가 있어야 Gemini 분석을 실행할 수 있습니다.
        </p>
      )}

      {error && (
        <p style={{ marginTop: 14, color: "#fca5a5", fontWeight: 700 }}>
          {error}
        </p>
      )}

      {analysis && (
        <div style={{ marginTop: 18 }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))",
              gap: 10,
              marginBottom: 16,
            }}
          >
            <Metric label="최근 경기" value={`${analysis.totalMatches}판`} />
            <Metric label="승률" value={`${analysis.winRate}%`} />
            <Metric
              label="승패"
              value={`${analysis.wins}승 ${analysis.losses}패`}
            />
            <Metric label="평균 KDA" value={analysis.averageKda.toFixed(2)} />
            <Metric
              label="평균 CS"
              value={Math.round(analysis.averageCs).toString()}
            />
            <Metric
              label="평균 딜량"
              value={Math.round(analysis.averageDamage).toLocaleString("ko-KR")}
            />
          </div>

          <div
            style={{
              padding: 14,
              borderRadius: 14,
              background: "rgba(0,0,0,0.22)",
              color: "#e5e7eb",
              lineHeight: 1.7,
              marginBottom: 14,
            }}
          >
            {analysis.summary}
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              gap: 12,
            }}
          >
            <AnalysisList title="강점" items={analysis.strengths} />
            <AnalysisList title="약점" items={analysis.weaknesses} />
            <AnalysisList title="개선 제안" items={analysis.recommendations} />
          </div>
        </div>
      )}
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        padding: 12,
        borderRadius: 14,
        background: "rgba(255,255,255,0.06)",
        border: "1px solid rgba(255,255,255,0.08)",
      }}
    >
      <div style={{ color: "#94a3b8", fontSize: 12, fontWeight: 800 }}>
        {label}
      </div>
      <div
        style={{
          color: "#fff",
          fontSize: 20,
          fontWeight: 900,
          marginTop: 5,
        }}
      >
        {value}
      </div>
    </div>
  );
}

function AnalysisList({ title, items }: { title: string; items: string[] }) {
  return (
    <div
      style={{
        padding: 14,
        borderRadius: 14,
        background: "rgba(255,255,255,0.045)",
        border: "1px solid rgba(255,255,255,0.08)",
      }}
    >
      <h4 style={{ margin: "0 0 10px", color: "#f5c542" }}>{title}</h4>

      <ul
        style={{
          margin: 0,
          paddingLeft: 18,
          color: "#d1d5db",
          lineHeight: 1.7,
        }}
      >
        {items.map((item, index) => (
          <li key={`${title}-${index}`}>{item}</li>
        ))}
      </ul>
    </div>
  );
}