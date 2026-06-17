"use client";

import { useState } from "react";
import type { MatchSummaryResponse } from "@/types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

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
  actionItems: string[];
  generatedBy?: string;
};

type WinAnalysisCardProps = {
  gameName: string;
  tagLine: string;
  totalMatches?: number;
  wins?: number;
  losses?: number;
  matches?: MatchSummaryResponse[];
};

export default function WinAnalysisCard({
  gameName,
  tagLine,
  totalMatches,
  wins,
  losses,
  matches,
}: WinAnalysisCardProps) {
  const [analysis, setAnalysis] = useState<AiWinAnalysisResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const hasMatches = Array.isArray(matches) && matches.length > 0;

  const analyze = async () => {
    const localStats = buildLocalStats({
      gameName,
      tagLine,
      totalMatches,
      wins,
      losses,
      matches,
    });

    if (!hasMatches) {
      setAnalysis(localStats);
      setErrorMessage("전적 검색 결과가 있어야 AI 분석을 실행할 수 있습니다.");
      return;
    }

    try {
      setLoading(true);
      setErrorMessage("");

      const response = await fetch(`${API_BASE_URL}/api/ai/gemini-win-analysis`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          gameName,
          tagLine,
          totalMatches: localStats.totalMatches,
          wins: localStats.wins,
          losses: localStats.losses,
          winRate: localStats.winRate,
          averageKda: localStats.averageKda,
          averageDeaths: localStats.averageDeaths,
          averageCs: localStats.averageCs,
          averageDamage: localStats.averageDamage,
          matches: matches ?? [],
        }),
      });

      if (!response.ok) {
        throw new Error(`AI 분석 API 오류: ${response.status}`);
      }

      const data = await response.json();
      setAnalysis(mergeGeminiAnalysis(localStats, data));
    } catch (error) {
      console.error(error);
      setAnalysis(localStats);
      setErrorMessage(
        "Gemini 분석 호출에 실패해서 기본 전적 분석으로 표시했습니다."
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
            GEMINI WIN ANALYSIS
          </p>
          <h3 style={{ margin: "6px 0 0", color: "#fff" }}>
            Gemini 승률 분석 리포트
          </h3>
          <p style={{ margin: "6px 0 0", color: "#cbd5e1", fontSize: 14 }}>
            최근 전적 데이터를 기반으로 승률 개선 포인트를 분석합니다.
          </p>
        </div>

        <button
          type="button"
          onClick={analyze}
          disabled={loading || !hasMatches}
          style={{
            border: "none",
            borderRadius: 999,
            padding: "11px 18px",
            background: "#f5c542",
            color: "#111827",
            fontWeight: 900,
            cursor: loading || !hasMatches ? "not-allowed" : "pointer",
            opacity: loading || !hasMatches ? 0.65 : 1,
          }}
        >
          {loading ? "분석 중..." : "Gemini 분석 실행"}
        </button>
      </div>

      {!hasMatches && (
        <p style={{ margin: "12px 0 0", color: "#fca5a5", fontSize: 13 }}>
          전적 검색 결과가 있어야 Gemini 분석을 실행할 수 있습니다.
        </p>
      )}

      {errorMessage && (
        <p style={{ margin: "12px 0 0", color: "#fca5a5", fontSize: 13 }}>
          {errorMessage}
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
            <AnalysisList title="다음 판 행동" items={analysis.actionItems} />
          </div>
        </div>
      )}
    </section>
  );
}

function buildLocalStats({
  gameName,
  tagLine,
  totalMatches,
  wins,
  losses,
  matches,
}: WinAnalysisCardProps): AiWinAnalysisResult {
  const matchList = Array.isArray(matches) ? matches : [];
  const safeTotalMatches = matchList.length || numberOrZero(totalMatches);
  const safeWins =
    matchList.length > 0
      ? matchList.filter((match) => Boolean(match.win)).length
      : numberOrZero(wins);
  const safeLosses =
    matchList.length > 0
      ? Math.max(0, safeTotalMatches - safeWins)
      : numberOrZero(losses);

  if (safeTotalMatches === 0) {
    return {
      gameName,
      tagLine,
      totalMatches: 0,
      wins: 0,
      losses: 0,
      winRate: 0,
      averageKda: 0,
      averageKills: 0,
      averageDeaths: 0,
      averageAssists: 0,
      averageDamage: 0,
      averageCs: 0,
      averageGold: 0,
      summary: "분석 가능한 최근 경기 데이터가 없습니다.",
      strengths: ["전적 검색 후 분석을 실행해주세요."],
      weaknesses: ["최근 경기 데이터가 부족합니다."],
      recommendations: ["소환사명과 태그를 확인한 뒤 다시 검색해주세요."],
      actionItems: ["먼저 전적 검색을 완료하세요."],
      generatedBy: "local",
    };
  }

  const totalKills = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.kills),
    0
  );
  const totalDeaths = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.deaths),
    0
  );
  const totalAssists = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.assists),
    0
  );
  const totalKda = matchList.reduce((sum, match) => {
    const kills = numberOrZero(match.kills);
    const deaths = numberOrZero(match.deaths);
    const assists = numberOrZero(match.assists);
    const kda = numberOrZero(match.kda);

    return sum + (kda > 0 ? kda : (kills + assists) / Math.max(1, deaths));
  }, 0);

  const totalDamage = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.totalDamageDealtToChampions),
    0
  );
  const totalCs = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.totalCs),
    0
  );
  const totalGold = matchList.reduce(
    (sum, match) => sum + numberOrZero(match.goldEarned),
    0
  );

  const winRate = round1((safeWins * 100) / safeTotalMatches);
  const averageKills = round1(totalKills / safeTotalMatches);
  const averageDeaths = round1(totalDeaths / safeTotalMatches);
  const averageAssists = round1(totalAssists / safeTotalMatches);
  const averageKda = round2(totalKda / safeTotalMatches);
  const averageDamage = round0(totalDamage / safeTotalMatches);
  const averageCs = round0(totalCs / safeTotalMatches);
  const averageGold = round0(totalGold / safeTotalMatches);

  return {
    gameName,
    tagLine,
    totalMatches: safeTotalMatches,
    wins: safeWins,
    losses: safeLosses,
    winRate,
    averageKda,
    averageKills,
    averageDeaths,
    averageAssists,
    averageDamage,
    averageCs,
    averageGold,
    summary: buildSummary(winRate, averageKda, averageDeaths, averageDamage, averageCs),
    strengths: buildStrengths(winRate, averageKda, averageDeaths, averageDamage, averageCs),
    weaknesses: buildWeaknesses(winRate, averageKda, averageDeaths, averageDamage, averageCs),
    recommendations: buildRecommendations(winRate, averageKda, averageDeaths, averageDamage, averageCs),
    actionItems: [
      "오브젝트 1분 전에는 시야를 먼저 확보하세요.",
      "불리한 교전은 피하고 데스 수를 줄이세요.",
      "승률 높은 챔피언 2~3개를 고정해서 플레이하세요.",
    ],
    generatedBy: "local",
  };
}

function mergeGeminiAnalysis(
  localStats: AiWinAnalysisResult,
  data: unknown
): AiWinAnalysisResult {
  const objectData =
    data && typeof data === "object" ? (data as Record<string, unknown>) : {};

  const summary =
    stringOrEmpty(objectData.summary) ||
    stringOrEmpty(objectData.overview) ||
    localStats.summary;

  return {
    ...localStats,
    summary,
    strengths: nonEmptyArray(objectData.strengths, localStats.strengths),
    weaknesses: nonEmptyArray(objectData.weaknesses, localStats.weaknesses),
    recommendations: nonEmptyArray(
      objectData.recommendations,
      localStats.recommendations
    ),
    actionItems: nonEmptyArray(objectData.actionItems, localStats.actionItems),
    generatedBy: stringOrEmpty(objectData.generatedBy) || "gemini",
  };
}

function buildSummary(
  winRate: number,
  averageKda: number,
  averageDeaths: number,
  averageDamage: number,
  averageCs: number
) {
  if (winRate >= 60 && averageKda >= 3.0) {
    return "최근 경기 기준 승률과 KDA가 모두 안정적입니다. 현재 흐름은 좋은 편이며 데스 관리만 유지하면 됩니다.";
  }

  if (winRate < 50 && averageDeaths >= 7.0) {
    return "패배 원인은 과도한 데스와 성장 손실 가능성이 큽니다. 생존과 오브젝트 타이밍 관리가 먼저 필요합니다.";
  }

  if (averageDamage >= 20000 && averageCs < 150) {
    return "교전 기여도는 괜찮지만 CS와 성장 안정성이 부족합니다. 파밍 안정성을 높이면 승률 개선 가능성이 큽니다.";
  }

  if (averageKda < 2.0) {
    return "최근 경기에서 KDA가 낮게 나타납니다. 무리한 교전 진입을 줄이고 불리한 상황에서는 데스를 줄여야 합니다.";
  }

  return "전반적인 지표는 보통 수준입니다. 승률을 올리려면 데스 관리, CS 안정화, 오브젝트 전 시야 확보가 중요합니다.";
}

function buildStrengths(
  winRate: number,
  averageKda: number,
  averageDeaths: number,
  averageDamage: number,
  averageCs: number
) {
  const strengths: string[] = [];

  if (winRate >= 60) {
    strengths.push("최근 승률이 높아 현재 플레이 흐름이 좋습니다.");
  }

  if (averageKda >= 3.0) {
    strengths.push("평균 KDA가 안정적이라 교전 손실이 적은 편입니다.");
  }

  if (averageDeaths <= 5.0) {
    strengths.push("평균 데스가 낮아 생존 관리가 잘 되고 있습니다.");
  }

  if (averageDamage >= 20000) {
    strengths.push("평균 딜량이 높아 한타 기여도가 좋은 편입니다.");
  }

  if (averageCs >= 180) {
    strengths.push("평균 CS가 높아 성장 안정성이 좋습니다.");
  }

  if (strengths.length === 0) {
    strengths.push("최근 데이터 기준으로 개선 방향을 잡기 좋은 상태입니다.");
  }

  return strengths;
}

function buildWeaknesses(
  winRate: number,
  averageKda: number,
  averageDeaths: number,
  averageDamage: number,
  averageCs: number
) {
  const weaknesses: string[] = [];

  if (winRate < 50) {
    weaknesses.push("최근 승률이 낮아 경기 운영 패턴 점검이 필요합니다.");
  }

  if (averageKda < 2.0) {
    weaknesses.push("평균 KDA가 낮아 교전에서 손해를 보는 경우가 많을 수 있습니다.");
  }

  if (averageDeaths >= 7.0) {
    weaknesses.push("평균 데스가 높아 성장 손실과 오브젝트 손실로 이어질 수 있습니다.");
  }

  if (averageDamage < 15000) {
    weaknesses.push("평균 딜량이 낮아 한타 영향력이 부족할 수 있습니다.");
  }

  if (averageCs < 150) {
    weaknesses.push("평균 CS가 낮아 중후반 성장 차이가 벌어질 수 있습니다.");
  }

  if (weaknesses.length === 0) {
    weaknesses.push("큰 약점은 적지만 오브젝트 전 시야와 데스 관리를 더 보완하면 좋습니다.");
  }

  return weaknesses;
}

function buildRecommendations(
  winRate: number,
  averageKda: number,
  averageDeaths: number,
  averageDamage: number,
  averageCs: number
) {
  const recommendations: string[] = [];

  if (averageDeaths >= 7.0) {
    recommendations.push("불리한 상황에서는 사이드 욕심보다 데스를 줄이는 운영을 우선하세요.");
  }

  if (averageCs < 150) {
    recommendations.push("10분 CS 70개 이상, 20분 CS 150개 이상을 목표로 하세요.");
  }

  if (averageDamage < 15000) {
    recommendations.push("한타 전 핵심 스킬을 먼저 소모하지 말고 딜 타이밍을 기다리세요.");
  }

  if (averageKda < 2.0) {
    recommendations.push("확실한 수적 우위가 없을 때는 무리한 교전을 피하세요.");
  }

  if (winRate < 50) {
    recommendations.push("패배 경기의 공통 원인을 줄이기 위해 초반 15분 데스 수를 관리하세요.");
  }

  recommendations.push("오브젝트 1분 전에는 제어 와드와 렌즈로 시야를 확보하세요.");

  return recommendations;
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
      <div style={{ color: "#fff", fontSize: 20, fontWeight: 900, marginTop: 5 }}>
        {value}
      </div>
    </div>
  );
}

function AnalysisList({ title, items }: { title: string; items: string[] }) {
  const safeItems = Array.isArray(items) ? items : [];

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
      <ul style={{ margin: 0, paddingLeft: 18, color: "#d1d5db", lineHeight: 1.7 }}>
        {safeItems.map((item, index) => (
          <li key={`${title}-${index}`}>{item}</li>
        ))}
      </ul>
    </div>
  );
}

function numberOrZero(value: unknown) {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  return 0;
}

function stringOrEmpty(value: unknown) {
  return typeof value === "string" ? value : "";
}

function nonEmptyArray(value: unknown, fallback: string[]) {
  if (!Array.isArray(value)) {
    return fallback;
  }

  const result = value
    .filter((item) => item !== null && item !== undefined)
    .map((item) => String(item))
    .filter((item) => item.trim().length > 0);

  return result.length > 0 ? result : fallback;
}

function round0(value: number) {
  return Math.round(value);
}

function round1(value: number) {
  return Math.round(value * 10) / 10;
}

function round2(value: number) {
  return Math.round(value * 100) / 100;
}
