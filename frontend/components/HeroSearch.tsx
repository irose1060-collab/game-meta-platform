"use client";

import { useState } from "react";
import { apiFetch } from "@/lib/api";
import type {
  MatchParticipantResponse,
  MatchSearchResponse,
  MatchSummaryResponse,
} from "@/types";

export default function HeroSearch() {
  const [gameName, setGameName] = useState("");
  const [tagLine, setTagLine] = useState("KR1");
  const [result, setResult] = useState<MatchSearchResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeSearchText, setActiveSearchText] = useState("");

  const searchByRiotId = async (name: string, tag: string) => {
    const cleanName = name.trim();
    const cleanTag = tag.trim().replace("#", "") || "KR1";

    if (!cleanName) {
      setError("소환사명을 입력해주세요.");
      setResult(null);
      return;
    }

    setLoading(true);
    setError("");
    setActiveSearchText(`${cleanName}#${cleanTag}`);

    try {
      const data = await apiFetch<MatchSearchResponse>(
        `/api/matches/search?gameName=${encodeURIComponent(
          cleanName
        )}&tagLine=${encodeURIComponent(cleanTag)}&count=10`
      );

      if (!data.puuid) {
        setError("Riot 계정 정보를 찾을 수 없습니다.");
        setResult(null);
        return;
      }

      setGameName(data.gameName || cleanName);
      setTagLine(data.tagLine || cleanTag);
      setResult(data);

      const target = document.getElementById("search-result-section");
      target?.scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (err) {
      setResult(null);
      setError(
        err instanceof Error
          ? err.message
          : "전적 조회 중 오류가 발생했습니다."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await searchByRiotId(gameName, tagLine);
  };

  const quickFill = (name: string, tag: string) => {
    setGameName(name);
    setTagLine(tag);
    setResult(null);
    setError("");
  };

  return (
    <section className="hero" id="search-section">
      <div className="container">
        <span className="eyebrow">DATA · META · VICTORY</span>
        <h1>META GG</h1>
        <p className="sub">
          전적 검색을 넘어, <b>승리를 위한 데이터</b>를 분석합니다.
        </p>

        <form className="search-wrap" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="소환사명 또는 Riot ID"
            autoComplete="off"
            value={gameName}
            onChange={(event) => setGameName(event.target.value)}
          />

          <input
            className="tag-input"
            type="text"
            placeholder="#KR1"
            maxLength={12}
            autoComplete="off"
            value={tagLine}
            onChange={(event) => setTagLine(event.target.value)}
          />

          <button type="submit" className="search-btn" disabled={loading}>
            {loading ? "SEARCHING..." : "전적 검색"}
          </button>
        </form>

        <div className="quick-tags">
          <button type="button" onClick={() => quickFill("Hide on bush", "KR1")}>
            Hide on bush #KR1
          </button>
          <button type="button" onClick={() => quickFill("Faker", "T1")}>
            Faker #T1
          </button>
          <button type="button" onClick={() => quickFill("ShowMaker", "KR1")}>
            ShowMaker #KR1
          </button>
        </div>

        {error && <p className="error-message">{error}</p>}

        {loading && activeSearchText && (
          <div className="search-result-box" id="search-result-section">
            <h3>{activeSearchText}</h3>
            <p>최근 10경기와 참가자 정보를 불러오는 중입니다...</p>
          </div>
        )}

        {!loading && result && (
          <div className="search-result-box" id="search-result-section">
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                gap: 16,
                alignItems: "center",
                flexWrap: "wrap",
              }}
            >
              <div>
                <h3>
                  {result.gameName} #{result.tagLine}
                </h3>
                <p>
                  최근 {result.totalMatches}경기 · {result.wins}승{" "}
                  {result.losses}패 · 승률{" "}
                  <strong>{result.winRate.toFixed(1)}%</strong> · 평균 KDA{" "}
                  <strong>{result.averageKda.toFixed(2)}</strong>
                </p>
              </div>

              <p className="success-message">
                참가자 닉네임을 클릭하면 해당 유저의 최근 10경기를 다시
                조회합니다.
              </p>
            </div>

            <div style={{ marginTop: 22, display: "grid", gap: 14 }}>
              {result.matches.map((match) => (
                <MatchCard
                  key={match.matchId}
                  match={match}
                  onParticipantClick={searchByRiotId}
                  currentPuuid={result.puuid}
                />
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

function MatchCard({
  match,
  onParticipantClick,
  currentPuuid,
}: {
  match: MatchSummaryResponse;
  onParticipantClick: (gameName: string, tagLine: string) => void;
  currentPuuid: string;
}) {
  return (
    <article
      style={{
        border: "1px solid rgba(255,255,255,0.1)",
        borderRadius: 18,
        background: "rgba(255,255,255,0.035)",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1.3fr 1fr",
          gap: 16,
          padding: 16,
          borderLeft: `5px solid ${match.win ? "#34d399" : "#f87171"}`,
        }}
      >
        <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
          <img
            src={match.championImageUrl}
            alt={match.championName}
            style={{
              width: 62,
              height: 62,
              borderRadius: 16,
              objectFit: "cover",
              background: "#111827",
            }}
          />

          <div>
            <div
              style={{
                fontWeight: 900,
                color: match.win ? "#86efac" : "#fca5a5",
              }}
            >
              {match.resultText} · {match.queueType} · {match.playedAtText}
            </div>
            <div style={{ marginTop: 4, fontWeight: 900, color: "#fff" }}>
              {match.championName} · {match.position}
            </div>
            <div style={{ marginTop: 4, color: "#cbd5e1", fontSize: 14 }}>
              {match.kills} / {match.deaths} / {match.assists} · KDA{" "}
              {match.kda.toFixed(2)} · CS {match.totalCs}
            </div>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "center",
            gap: 14,
            color: "#cbd5e1",
            fontSize: 13,
            flexWrap: "wrap",
          }}
        >
          <div>딜량 {formatNumber(match.totalDamageDealtToChampions)}</div>
          <div>골드 {formatNumber(match.goldEarned)}</div>
          <div>게임 시간 {match.gameDurationText}</div>
        </div>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 12,
          padding: 16,
          borderTop: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <TeamList
          title={`블루팀 · ${match.blueTeamTotalKills}킬`}
          participants={match.blueTeam}
          maxDamage={match.maxDamage}
          currentPuuid={currentPuuid}
          onParticipantClick={onParticipantClick}
        />

        <TeamList
          title={`레드팀 · ${match.redTeamTotalKills}킬`}
          participants={match.redTeam}
          maxDamage={match.maxDamage}
          currentPuuid={currentPuuid}
          onParticipantClick={onParticipantClick}
        />
      </div>
    </article>
  );
}

function TeamList({
  title,
  participants,
  maxDamage,
  currentPuuid,
  onParticipantClick,
}: {
  title: string;
  participants: MatchParticipantResponse[];
  maxDamage: number;
  currentPuuid: string;
  onParticipantClick: (gameName: string, tagLine: string) => void;
}) {
  return (
    <div>
      <div style={{ marginBottom: 10, fontWeight: 900, color: "#f5c542" }}>
        {title}
      </div>

      <div style={{ display: "grid", gap: 8 }}>
        {participants.map((participant) => {
          const damageWidth =
            maxDamage > 0
              ? Math.max(
                  6,
                  Math.round(
                    (participant.totalDamageDealtToChampions / maxDamage) * 100
                  )
                )
              : 0;

          const riotGameName =
            participant.riotGameName ||
            participant.summonerName ||
            "Unknown";

          const riotTagLine = participant.riotTagLine || "";

          const canSearch = Boolean(
            riotGameName &&
              riotGameName !== "Unknown" &&
              riotTagLine &&
              participant.puuid !== currentPuuid
          );

          const isCurrentUser = participant.puuid === currentPuuid;

          return (
            <div
              key={`${participant.puuid}-${participant.championName}-${participant.teamPosition}`}
              style={{
                display: "grid",
                gridTemplateColumns: "34px 1fr 70px 90px",
                alignItems: "center",
                gap: 8,
                color: "#e5e7eb",
                fontSize: 13,
                borderRadius: 10,
                padding: "4px 6px",
                background: isCurrentUser
                  ? "rgba(245,197,66,0.08)"
                  : "transparent",
              }}
            >
              <img
                src={participant.championImageUrl}
                alt={participant.championName}
                style={{
                  width: 34,
                  height: 34,
                  borderRadius: 10,
                  objectFit: "cover",
                }}
              />

              <div style={{ minWidth: 0 }}>
                <button
                  type="button"
                  disabled={!canSearch}
                  onClick={() => onParticipantClick(riotGameName, riotTagLine)}
                  title={
                    canSearch
                      ? `${riotGameName}#${riotTagLine} 전적 검색`
                      : "검색 가능한 Riot ID 정보가 없습니다."
                  }
                  style={{
                    display: "block",
                    width: "100%",
                    border: "none",
                    background: "transparent",
                    padding: 0,
                    color: canSearch ? "#f8d978" : "#e5e7eb",
                    fontWeight: 900,
                    textAlign: "left",
                    cursor: canSearch ? "pointer" : "default",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {riotGameName}
                  {riotTagLine ? ` #${riotTagLine}` : ""}
                  {isCurrentUser ? " · 현재 검색 유저" : ""}
                </button>

                <div style={{ color: "#94a3b8", fontSize: 12 }}>
                  {participant.teamPosition} · {participant.championName}
                </div>
              </div>

              <div style={{ textAlign: "right", fontWeight: 800 }}>
                {participant.kills}/{participant.deaths}/{participant.assists}
              </div>

              <div>
                <div
                  style={{
                    color: "#94a3b8",
                    fontSize: 11,
                    textAlign: "right",
                  }}
                >
                  {formatNumber(participant.totalDamageDealtToChampions)}
                </div>

                <div
                  style={{
                    height: 5,
                    borderRadius: 999,
                    background: "rgba(255,255,255,0.08)",
                    overflow: "hidden",
                    marginTop: 4,
                  }}
                >
                  <div
                    style={{
                      width: `${damageWidth}%`,
                      height: "100%",
                      borderRadius: 999,
                      background: "#f5c542",
                    }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.round(value));
}