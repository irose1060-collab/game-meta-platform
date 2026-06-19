"use client";

import { useEffect, useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { apiFetch } from "@/lib/api";
import WinAnalysisCard from "@/components/WinAnalysisCard";
import type {
  AssetDto,
  ItemBuildStepResponse,
  MatchParticipantResponse,
  MatchSearchResponse,
  MatchSummaryResponse,
  MatchTeamSummaryResponse,
} from "@/types";

const TEAM_BLUE = "#38bdf8";
const TEAM_RED = "#fb7185";
const GOLD = "#f5c542";
const TEXT_DIM = "#94a3b8";

type SavedSearch = {
  gameName: string;
  tagLine: string;
  searchedAt: number;
};

const RECENT_SEARCH_KEY = "meta_gg_recent_searches";
const FAVORITE_SEARCH_KEY = "meta_gg_favorite_searches";

const POSITION_LABELS: Record<string, string> = {
  TOP: "탑",
  JUNGLE: "정글",
  MID: "미드",
  MIDDLE: "미드",
  ADC: "원딜",
  BOTTOM: "원딜",
  SUPPORT: "서포터",
  UTILITY: "서포터",
};

export default function HeroSearch() {
  const [gameName, setGameName] = useState("");
  const [tagLine, setTagLine] = useState("KR1");
  const [result, setResult] = useState<MatchSearchResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeSearchText, setActiveSearchText] = useState("");
  const [recentSearches, setRecentSearches] = useState<SavedSearch[]>([]);
  const [favoriteSearches, setFavoriteSearches] = useState<SavedSearch[]>([]);

  useEffect(() => {
    setRecentSearches(readSavedSearches(RECENT_SEARCH_KEY));
    setFavoriteSearches(readSavedSearches(FAVORITE_SEARCH_KEY));
  }, []);

  const saveRecentSearch = (item: SavedSearch) => {
    setRecentSearches((prev) => {
      const next = dedupeSearches([item, ...prev]).slice(0, 8);
      writeSavedSearches(RECENT_SEARCH_KEY, next);
      return next;
    });
  };

  const toggleFavoriteSearch = (item: SavedSearch) => {
    setFavoriteSearches((prev) => {
      const exists = prev.some(
        (saved) =>
          saved.gameName.toLowerCase() === item.gameName.toLowerCase() &&
          saved.tagLine.toLowerCase() === item.tagLine.toLowerCase()
      );

      const next = exists
        ? prev.filter(
            (saved) =>
              !(
                saved.gameName.toLowerCase() === item.gameName.toLowerCase() &&
                saved.tagLine.toLowerCase() === item.tagLine.toLowerCase()
              )
          )
        : dedupeSearches([item, ...prev]).slice(0, 8);

      writeSavedSearches(FAVORITE_SEARCH_KEY, next);
      return next;
    });
  };

  const isFavoriteSearch = (name: string, tag: string) =>
    favoriteSearches.some(
      (saved) =>
        saved.gameName.toLowerCase() === name.toLowerCase() &&
        saved.tagLine.toLowerCase() === tag.toLowerCase()
    );

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
        )}&tagLine=${encodeURIComponent(cleanTag)}&count=20`
      );

      if (!data.puuid) {
        setError("Riot 계정 정보를 찾을 수 없습니다.");
        setResult(null);
        return;
      }

      setGameName(data.gameName || cleanName);
      setTagLine(data.tagLine || cleanTag);
      setResult(data);
      saveRecentSearch({
        gameName: data.gameName || cleanName,
        tagLine: data.tagLine || cleanTag,
        searchedAt: Date.now(),
      });

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

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
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

        {(favoriteSearches.length > 0 || recentSearches.length > 0) && (
          <div style={savedSearchBoxStyle}>
            {favoriteSearches.length > 0 && (
              <SavedSearchLine
                title="즐겨찾기"
                searches={favoriteSearches}
                onSearch={searchByRiotId}
                onToggleFavorite={toggleFavoriteSearch}
                favorites={favoriteSearches}
              />
            )}
            {recentSearches.length > 0 && (
              <SavedSearchLine
                title="최근 검색"
                searches={recentSearches}
                onSearch={searchByRiotId}
                onToggleFavorite={toggleFavoriteSearch}
                favorites={favoriteSearches}
              />
            )}
          </div>
        )}

        {error && <p className="error-message">{error}</p>}

        {loading && activeSearchText && (
          <div className="search-result-box" id="search-result-section">
            <h3>{activeSearchText}</h3>
            <p>최근 솔로랭크 20경기와 참가자 정보를 불러오는 중입니다...</p>
          </div>
        )}

        {!loading && result && (
          <div
            className="search-result-box"
            id="search-result-section"
            style={{ maxWidth: 1180 }}
          >
            <div style={resultHeaderStyle}>
              <div>
                <h3>
                  {result.gameName} #{result.tagLine}
                </h3>
                <p>
                  최근 솔로랭크 {result.totalMatches}경기 · {result.wins}승{" "}
                  {result.losses}패 · 승률{" "}
                  <strong>{result.winRate.toFixed(1)}%</strong> · 평균 KDA{" "}
                  <strong>{result.averageKda.toFixed(2)}</strong>
                </p>
                <p style={{ color: TEXT_DIM }}>
                  상세 전적은 아이템, 룬, 스펠, 시야, 오브젝트, 팀별 딜량을 함께 표시합니다.
                </p>
              </div>

              <div style={resultActionPanelStyle}>
                <button
                  type="button"
                  style={favoriteButtonStyle}
                  onClick={() =>
                    toggleFavoriteSearch({
                      gameName: result.gameName,
                      tagLine: result.tagLine,
                      searchedAt: Date.now(),
                    })
                  }
                >
                  {isFavoriteSearch(result.gameName, result.tagLine)
                    ? "★ 즐겨찾기 해제"
                    : "☆ 즐겨찾기"}
                </button>

                <WinAnalysisCard
                  gameName={result.gameName}
                tagLine={result.tagLine}
                totalMatches={result.totalMatches}
                wins={result.wins}
                losses={result.losses}
                  matches={result.matches ?? []}
                />
              </div>
            </div>

            <p className="success-message">
              참가자 닉네임을 클릭하면 해당 유저의 최근 솔로랭크 20경기를 다시 조회합니다.
            </p>

            <div style={{ marginTop: 22, display: "grid", gap: 16 }}>
              {(result.matches ?? []).map((match) => (
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


function SavedSearchLine({
  title,
  searches,
  favorites,
  onSearch,
  onToggleFavorite,
}: {
  title: string;
  searches: SavedSearch[];
  favorites: SavedSearch[];
  onSearch: (gameName: string, tagLine: string) => void;
  onToggleFavorite: (item: SavedSearch) => void;
}) {
  return (
    <div style={savedSearchLineStyle}>
      <strong style={{ color: GOLD, fontSize: 12 }}>{title}</strong>
      <div style={savedSearchChipWrapStyle}>
        {searches.map((item) => {
          const activeFavorite = favorites.some(
            (favorite) =>
              favorite.gameName.toLowerCase() === item.gameName.toLowerCase() &&
              favorite.tagLine.toLowerCase() === item.tagLine.toLowerCase()
          );

          return (
            <span key={`${title}-${item.gameName}-${item.tagLine}`} style={savedSearchChipStyle}>
              <button
                type="button"
                style={savedSearchButtonStyle}
                onClick={() => onSearch(item.gameName, item.tagLine)}
              >
                {item.gameName} #{item.tagLine}
              </button>
              <button
                type="button"
                aria-label="즐겨찾기"
                style={savedSearchStarStyle}
                onClick={() => onToggleFavorite({ ...item, searchedAt: Date.now() })}
              >
                {activeFavorite ? "★" : "☆"}
              </button>
            </span>
          );
        })}
      </div>
    </div>
  );
}

function readSavedSearches(key: string): SavedSearch[] {
  if (typeof window === "undefined") return [];

  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return [];

    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];

    return parsed
      .filter(
        (item) =>
          item &&
          typeof item.gameName === "string" &&
          typeof item.tagLine === "string"
      )
      .slice(0, 8);
  } catch {
    return [];
  }
}

function writeSavedSearches(key: string, value: SavedSearch[]) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(key, JSON.stringify(value));
}

function dedupeSearches(searches: SavedSearch[]) {
  const map = new Map<string, SavedSearch>();

  for (const search of searches) {
    const key = `${search.gameName.trim().toLowerCase()}#${search.tagLine.trim().toLowerCase()}`;
    if (!map.has(key)) {
      map.set(key, {
        gameName: search.gameName.trim(),
        tagLine: search.tagLine.trim().replace("#", "") || "KR1",
        searchedAt: search.searchedAt || Date.now(),
      });
    }
  }

  return Array.from(map.values());
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
  const teamColor = match.win ? "#34d399" : "#f87171";
  const resultBg = match.win
    ? "rgba(52, 211, 153, 0.12)"
    : "rgba(248, 113, 113, 0.12)";
  const [expanded, setExpanded] = useState(false);

  return (
    <article style={matchCardStyle}>
      <div style={{ ...matchTopStyle, borderLeft: `5px solid ${teamColor}` }}>
        <div style={mainChampionBlockStyle}>
          <div style={{ position: "relative", flexShrink: 0 }}>
            <img
              src={match.championImageUrl}
              alt={match.championName}
              style={championPortraitStyle}
            />
            {match.opScoreBadge && match.opScoreBadge !== "-" && (
              <span style={opBadgeStyle}>{match.opScoreBadge}</span>
            )}
          </div>

          <div style={{ minWidth: 0 }}>
            <div style={{ ...resultTextStyle, color: teamColor }}>
              {match.resultText} · {match.queueType} · {match.playedAtText}
            </div>

            <div style={championTitleStyle}>
              {match.championName} · {positionLabel(match.position)}
            </div>

            <div style={kdaTextStyle}>
              {match.kills} / {match.deaths} / {match.assists} · KDA{" "}
              {match.kda.toFixed(2)} · CS {match.totalCs} ({match.csPerMinute.toFixed(1)}/분)
            </div>

            <div style={assetLineStyle}>
              <AssetIconList assets={match.summonerSpells ?? []} size={27} max={2} />
              <AssetIconList assets={match.runes ?? []} size={27} max={6} />
            </div>
          </div>
        </div>

        <div style={mainStatsPanelStyle}>
          <div style={{ ...resultPillStyle, background: resultBg, color: teamColor }}>
            {match.resultText}
          </div>
          <StatChip label="킬관여" value={`${match.killParticipation ?? 0}%`} />
          <StatChip label="딜량" value={formatNumber(match.totalDamageDealtToChampions)} />
          <StatChip label="받은 피해" value={formatNumber(match.totalDamageTaken)} />
          <StatChip label="골드" value={formatNumber(match.goldEarned)} />
          <StatChip label="시야" value={String(match.visionScore ?? 0)} />
          <StatChip label="제어 와드" value={String(match.controlWardsPlaced ?? 0)} />
          <StatChip label="OP 점수" value={`${(match.opScore ?? 0).toFixed(1)}점`} />
        </div>
      </div>

      <div style={matchSummaryActionStyle}>
        <div style={matchSummaryTextStyle}>
          <strong style={{ color: teamColor }}>{match.resultText}</strong> · {match.championName} · {match.kills}/{match.deaths}/{match.assists} · {match.gameDurationText}
        </div>
        <button
          type="button"
          style={detailToggleButtonStyle}
          aria-expanded={expanded}
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? "상세 접기" : "상세보기"}
        </button>
      </div>

      {expanded && (
        <>
          <div style={itemBuildStyle}>
            <div style={sectionLabelStyle}>최종 아이템</div>
            <AssetIconList assets={match.items ?? []} size={34} max={7} showEmpty />
            <div style={{ marginLeft: "auto", color: TEXT_DIM, fontSize: 12 }}>
              게임 시간 {match.gameDurationText}
            </div>
          </div>

          <div style={timelineGridStyle}>
            <ItemBuildTimeline itemBuild={match.itemBuild ?? []} />
            <SkillOrderLine
              skillOrder={match.skillOrder ?? []}
              skillOrderText={match.skillOrderText}
            />
          </div>

          <div style={objectivesStyle}>
            <TeamObjectiveSummary
              label="블루팀"
              color={TEAM_BLUE}
              summary={match.blueTeamSummary}
              totalKills={match.blueTeamTotalKills}
              totalGold={match.blueTeamTotalGold}
            />
            <TeamObjectiveSummary
              label="레드팀"
              color={TEAM_RED}
              summary={match.redTeamSummary}
              totalKills={match.redTeamTotalKills}
              totalGold={match.redTeamTotalGold}
            />
          </div>

          <div style={teamsGridStyle}>
            <TeamList
              title={`블루팀 · ${match.blueTeamTotalKills}킬`}
              color={TEAM_BLUE}
              participants={match.blueTeam ?? []}
              maxDamage={match.maxDamage}
              currentPuuid={currentPuuid}
              onParticipantClick={onParticipantClick}
            />

            <TeamList
              title={`레드팀 · ${match.redTeamTotalKills}킬`}
              color={TEAM_RED}
              participants={match.redTeam ?? []}
              maxDamage={match.maxDamage}
              currentPuuid={currentPuuid}
              onParticipantClick={onParticipantClick}
            />
          </div>
        </>
      )}
    </article>
  );
}


function ItemBuildTimeline({ itemBuild }: { itemBuild: ItemBuildStepResponse[] }) {
  const visibleBuild = (itemBuild ?? []).slice(0, 12);

  return (
    <div style={timelinePanelStyle}>
      <div style={timelineHeaderStyle}>
        <strong>아이템 구매 흐름</strong>
        <span>Timeline API</span>
      </div>

      {visibleBuild.length === 0 ? (
        <p style={timelineEmptyTextStyle}>타임라인 데이터가 없거나 아직 수집되지 않았습니다.</p>
      ) : (
        <div style={itemTimelineStyle}>
          {visibleBuild.map((item, index) => (
            <div key={`${item.itemId}-${item.timestampMs}-${index}`} style={itemTimelineStepStyle}>
              <span style={timelineMinuteStyle}>{item.minute}분</span>
              <img src={item.imageUrl} alt={item.itemName} title={item.itemName} style={timelineItemIconStyle} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SkillOrderLine({
  skillOrder,
  skillOrderText,
}: {
  skillOrder: { skillKey: string; level: number; minute: number }[];
  skillOrderText?: string;
}) {
  const visibleSkills = (skillOrder ?? []).slice(0, 12);

  return (
    <div style={timelinePanelStyle}>
      <div style={timelineHeaderStyle}>
        <strong>스킬 레벨업</strong>
        <span>{skillOrderText && skillOrderText !== "-" ? skillOrderText : "Q/W/E/R 순서"}</span>
      </div>

      {visibleSkills.length === 0 ? (
        <p style={timelineEmptyTextStyle}>스킬 레벨업 데이터가 없습니다.</p>
      ) : (
        <div style={skillOrderWrapStyle}>
          {visibleSkills.map((skill) => (
            <div key={`${skill.level}-${skill.minute}-${skill.skillKey}`} style={skillBadgeStyle}>
              <b>{skill.skillKey}</b>
              <span>Lv.{skill.level}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function TeamObjectiveSummary({
  label,
  color,
  summary,
  totalKills,
  totalGold,
}: {
  label: string;
  color: string;
  summary?: MatchTeamSummaryResponse;
  totalKills: number;
  totalGold: number;
}) {
  const bans = summary?.bans ?? [];

  return (
    <div style={{ ...teamObjectiveCardStyle, borderColor: `${color}55` }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
        <strong style={{ color }}>{label}</strong>
        <span style={{ color: summary?.win ? "#86efac" : "#fca5a5", fontWeight: 900 }}>
          {summary?.win ? "승리" : "패배"}
        </span>
      </div>

      <div style={objectiveStatGridStyle}>
        <SmallMetric label="킬" value={formatNumber(summary?.totalKills ?? totalKills)} />
        <SmallMetric label="골드" value={formatNumber(summary?.totalGold ?? totalGold)} />
        <SmallMetric label="드래곤" value={String(summary?.dragonKills ?? 0)} />
        <SmallMetric label="바론" value={String(summary?.baronKills ?? 0)} />
        <SmallMetric label="타워" value={String(summary?.towerKills ?? 0)} />
        <SmallMetric label="유충" value={String(summary?.hordeKills ?? 0)} />
      </div>

      <div style={banLineStyle}>
        <span style={{ color: TEXT_DIM, fontSize: 12, fontWeight: 800 }}>밴</span>
        {bans.length > 0 ? (
          <AssetIconList assets={bans} size={24} max={5} />
        ) : (
          <span style={{ color: "#64748b", fontSize: 12 }}>밴 데이터 없음</span>
        )}
      </div>
    </div>
  );
}

function TeamList({
  title,
  color,
  participants,
  maxDamage,
  currentPuuid,
  onParticipantClick,
}: {
  title: string;
  color: string;
  participants: MatchParticipantResponse[];
  maxDamage: number;
  currentPuuid: string;
  onParticipantClick: (gameName: string, tagLine: string) => void;
}) {
  return (
    <div>
      <div style={{ marginBottom: 10, fontWeight: 900, color }}>{title}</div>

      <div style={{ display: "grid", gap: 9 }}>
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
            participant.riotGameName || participant.summonerName || "Unknown";

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
                ...participantRowStyle,
                background: isCurrentUser
                  ? "rgba(245,197,66,0.11)"
                  : "rgba(255,255,255,0.025)",
                borderColor: isCurrentUser
                  ? "rgba(245,197,66,0.32)"
                  : "rgba(255,255,255,0.07)",
              }}
            >
              <img
                src={participant.championImageUrl}
                alt={participant.championName}
                style={participantChampionStyle}
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
                    ...participantNameButtonStyle,
                    color: canSearch ? "#f8d978" : "#e5e7eb",
                    cursor: canSearch ? "pointer" : "default",
                  }}
                >
                  {riotGameName}
                  {riotTagLine ? ` #${riotTagLine}` : ""}
                  {isCurrentUser ? " · 현재 검색 유저" : ""}
                </button>

                <div style={{ color: TEXT_DIM, fontSize: 12 }}>
                  {positionLabel(participant.teamPosition)} · {participant.championName} · Lv.{participant.championLevel ?? "-"}
                </div>

                <div style={{ display: "flex", gap: 6, marginTop: 6, alignItems: "center", flexWrap: "wrap" }}>
                  <AssetIconList assets={participant.summonerSpells ?? []} size={22} max={2} />
                  <AssetIconList assets={participant.items ?? []} size={22} max={7} showEmpty />
                </div>
              </div>

              <div style={participantKdaStyle}>
                <b>
                  {participant.kills}/{participant.deaths}/{participant.assists}
                </b>
                <span>KDA {participant.kda.toFixed(2)}</span>
                <span>킬관여 {participant.killParticipation ?? 0}%</span>
              </div>

              <div style={participantDetailStyle}>
                <span>CS {participant.totalCs} ({participant.csPerMinute.toFixed(1)})</span>
                <span>시야 {participant.visionScore}</span>
                <span>OP {participant.opScore?.toFixed(1)} · {participant.opScoreBadge}</span>
              </div>

              <div>
                <div style={damageLabelStyle}>
                  {formatNumber(participant.totalDamageDealtToChampions)}
                </div>

                <div style={damageTrackStyle}>
                  <div
                    style={{
                      width: `${damageWidth}%`,
                      height: "100%",
                      borderRadius: 999,
                      background: color,
                    }}
                  />
                </div>

                <div style={{ marginTop: 4, color: TEXT_DIM, fontSize: 11, textAlign: "right" }}>
                  딜비중 {participant.damageShare?.toFixed(1) ?? "0.0"}%
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function AssetIconList({
  assets,
  size,
  max,
  showEmpty = false,
}: {
  assets: AssetDto[];
  size: number;
  max: number;
  showEmpty?: boolean;
}) {
  const normalized = (assets ?? []).slice(0, max);
  const emptyCount = showEmpty ? Math.max(0, max - normalized.length) : 0;

  return (
    <div style={{ display: "flex", gap: 4, alignItems: "center", flexWrap: "wrap" }}>
      {normalized.map((asset, index) => (
        <AssetIcon key={`${asset.id}-${index}`} asset={asset} size={size} />
      ))}
      {Array.from({ length: emptyCount }).map((_, index) => (
        <AssetIcon key={`empty-${index}`} asset={null} size={size} />
      ))}
    </div>
  );
}

function AssetIcon({ asset, size }: { asset: AssetDto | null; size: number }) {
  if (!asset) {
    return <span style={{ ...assetEmptyStyle, width: size, height: size }} />;
  }

  return (
    <img
      src={asset.imageUrl}
      alt={asset.name || asset.id}
      title={asset.name || asset.id}
      style={{ ...assetIconStyle, width: size, height: size }}
    />
  );
}

function StatChip({ label, value }: { label: string; value: string }) {
  return (
    <div style={statChipStyle}>
      <span>{label}</span>
      <b>{value}</b>
    </div>
  );
}

function SmallMetric({ label, value }: { label: string; value: string }) {
  return (
    <div style={smallMetricStyle}>
      <span>{label}</span>
      <b>{value}</b>
    </div>
  );
}

function positionLabel(position: string) {
  return POSITION_LABELS[position] ?? position ?? "-";
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.round(value || 0));
}

const resultHeaderStyle: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 16,
  alignItems: "center",
  flexWrap: "wrap",
};

const matchCardStyle: CSSProperties = {
  border: "1px solid rgba(255,255,255,0.1)",
  borderRadius: 20,
  background: "rgba(255,255,255,0.035)",
  overflow: "hidden",
  textAlign: "left",
};

const matchTopStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(320px, 1.35fr) minmax(260px, 1fr)",
  gap: 16,
  padding: 16,
};

const mainChampionBlockStyle: CSSProperties = {
  display: "flex",
  gap: 14,
  alignItems: "center",
  minWidth: 0,
};

const championPortraitStyle: CSSProperties = {
  width: 76,
  height: 76,
  borderRadius: 18,
  objectFit: "cover",
  background: "#111827",
  border: "1px solid rgba(255,255,255,0.1)",
};

const opBadgeStyle: CSSProperties = {
  position: "absolute",
  left: "50%",
  bottom: -8,
  transform: "translateX(-50%)",
  padding: "2px 7px",
  borderRadius: 999,
  background: "linear-gradient(180deg,#f8d978,#c89b3c)",
  color: "#1f1305",
  fontSize: 10,
  fontWeight: 950,
  boxShadow: "0 4px 10px rgba(0,0,0,0.35)",
};

const resultTextStyle: CSSProperties = {
  fontWeight: 950,
  fontSize: 13,
};

const championTitleStyle: CSSProperties = {
  marginTop: 4,
  fontWeight: 950,
  color: "#fff",
  fontSize: 17,
};

const kdaTextStyle: CSSProperties = {
  marginTop: 4,
  color: "#cbd5e1",
  fontSize: 14,
};

const assetLineStyle: CSSProperties = {
  display: "flex",
  gap: 10,
  alignItems: "center",
  flexWrap: "wrap",
  marginTop: 10,
};

const mainStatsPanelStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(4, minmax(80px, 1fr))",
  gap: 8,
  alignContent: "center",
};

const resultPillStyle: CSSProperties = {
  borderRadius: 12,
  padding: "8px 10px",
  fontWeight: 950,
  textAlign: "center",
  border: "1px solid rgba(255,255,255,0.08)",
};

const statChipStyle: CSSProperties = {
  borderRadius: 12,
  padding: "8px 10px",
  background: "rgba(255,255,255,0.045)",
  border: "1px solid rgba(255,255,255,0.07)",
  display: "grid",
  gap: 2,
};

const matchSummaryActionStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 12,
  padding: "12px 16px 16px",
  borderTop: "1px solid rgba(255,255,255,0.07)",
  flexWrap: "wrap",
};

const matchSummaryTextStyle: CSSProperties = {
  color: "#cbd5e1",
  fontSize: 13,
  fontWeight: 800,
};

const detailToggleButtonStyle: CSSProperties = {
  border: "1px solid rgba(245,197,66,0.35)",
  background: "rgba(245,197,66,0.1)",
  color: "#f8d978",
  borderRadius: 999,
  padding: "8px 14px",
  fontSize: 13,
  fontWeight: 950,
  cursor: "pointer",
};

const itemBuildStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  padding: "0 16px 16px",
  flexWrap: "wrap",
};

const sectionLabelStyle: CSSProperties = {
  color: GOLD,
  fontWeight: 950,
  fontSize: 12,
  letterSpacing: 1,
};

const objectivesStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: 12,
  padding: 16,
  borderTop: "1px solid rgba(255,255,255,0.08)",
};

const teamObjectiveCardStyle: CSSProperties = {
  border: "1px solid rgba(255,255,255,0.1)",
  borderRadius: 14,
  padding: 12,
  background: "rgba(0,0,0,0.16)",
};

const objectiveStatGridStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(3, 1fr)",
  gap: 8,
  marginTop: 10,
};

const smallMetricStyle: CSSProperties = {
  display: "grid",
  gap: 2,
  padding: 8,
  borderRadius: 10,
  background: "rgba(255,255,255,0.035)",
};

const banLineStyle: CSSProperties = {
  display: "flex",
  gap: 8,
  alignItems: "center",
  marginTop: 10,
  flexWrap: "wrap",
};

const teamsGridStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: 12,
  padding: 16,
  borderTop: "1px solid rgba(255,255,255,0.08)",
};

const participantRowStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "38px minmax(130px, 1fr) 84px 92px 105px",
  alignItems: "center",
  gap: 8,
  color: "#e5e7eb",
  fontSize: 13,
  borderRadius: 12,
  padding: "7px 8px",
  border: "1px solid rgba(255,255,255,0.07)",
};

const participantChampionStyle: CSSProperties = {
  width: 38,
  height: 38,
  borderRadius: 10,
  objectFit: "cover",
};

const participantNameButtonStyle: CSSProperties = {
  display: "block",
  width: "100%",
  border: "none",
  background: "transparent",
  padding: 0,
  fontWeight: 950,
  textAlign: "left",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
};

const participantKdaStyle: CSSProperties = {
  display: "grid",
  gap: 1,
  textAlign: "right",
};

const participantDetailStyle: CSSProperties = {
  display: "grid",
  gap: 1,
  color: TEXT_DIM,
  fontSize: 11,
  textAlign: "right",
};

const damageLabelStyle: CSSProperties = {
  color: TEXT_DIM,
  fontSize: 11,
  textAlign: "right",
};

const damageTrackStyle: CSSProperties = {
  height: 5,
  borderRadius: 999,
  background: "rgba(255,255,255,0.08)",
  overflow: "hidden",
  marginTop: 4,
};

const assetIconStyle: CSSProperties = {
  borderRadius: 7,
  objectFit: "cover",
  background: "#111827",
  border: "1px solid rgba(255,255,255,0.1)",
};

const assetEmptyStyle: CSSProperties = {
  display: "inline-block",
  borderRadius: 7,
  background: "rgba(255,255,255,0.045)",
  border: "1px solid rgba(255,255,255,0.07)",
};

const savedSearchBoxStyle: CSSProperties = {
  marginTop: 14,
  display: "grid",
  gap: 8,
  padding: 12,
  borderRadius: 16,
  background: "rgba(255,255,255,0.035)",
  border: "1px solid rgba(255,255,255,0.08)",
};

const savedSearchLineStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  flexWrap: "wrap",
};

const savedSearchChipWrapStyle: CSSProperties = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
};

const savedSearchChipStyle: CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  gap: 5,
  padding: "5px 7px",
  borderRadius: 999,
  background: "rgba(0,0,0,0.2)",
  border: "1px solid rgba(255,255,255,0.08)",
};

const savedSearchButtonStyle: CSSProperties = {
  border: "none",
  background: "transparent",
  color: "#e5e7eb",
  fontSize: 12,
  fontWeight: 800,
  cursor: "pointer",
};

const savedSearchStarStyle: CSSProperties = {
  border: "none",
  background: "transparent",
  color: GOLD,
  cursor: "pointer",
  fontSize: 13,
  lineHeight: 1,
};

const resultActionPanelStyle: CSSProperties = {
  display: "grid",
  gap: 10,
  justifyItems: "end",
  minWidth: 260,
};

const favoriteButtonStyle: CSSProperties = {
  border: "1px solid rgba(245,197,66,0.35)",
  background: "rgba(245,197,66,0.08)",
  color: "#f8d978",
  borderRadius: 999,
  padding: "8px 12px",
  fontWeight: 950,
  cursor: "pointer",
};

const timelineGridStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1.2fr 0.8fr",
  gap: 12,
  padding: "0 16px 16px",
};

const timelinePanelStyle: CSSProperties = {
  border: "1px solid rgba(255,255,255,0.08)",
  borderRadius: 14,
  padding: 12,
  background: "rgba(0,0,0,0.16)",
  minWidth: 0,
};

const timelineHeaderStyle: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  gap: 10,
  alignItems: "center",
  color: "#e5e7eb",
  fontSize: 12,
  marginBottom: 10,
};

const timelineEmptyTextStyle: CSSProperties = {
  margin: 0,
  color: TEXT_DIM,
  fontSize: 12,
};

const itemTimelineStyle: CSSProperties = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
  alignItems: "center",
};

const itemTimelineStepStyle: CSSProperties = {
  display: "grid",
  justifyItems: "center",
  gap: 4,
};

const timelineMinuteStyle: CSSProperties = {
  color: TEXT_DIM,
  fontSize: 10,
  fontWeight: 800,
};

const timelineItemIconStyle: CSSProperties = {
  width: 30,
  height: 30,
  borderRadius: 8,
  objectFit: "cover",
  background: "#111827",
  border: "1px solid rgba(255,255,255,0.1)",
};

const skillOrderWrapStyle: CSSProperties = {
  display: "flex",
  gap: 6,
  flexWrap: "wrap",
};

const skillBadgeStyle: CSSProperties = {
  width: 38,
  height: 38,
  borderRadius: 12,
  background: "rgba(245,197,66,0.1)",
  border: "1px solid rgba(245,197,66,0.18)",
  display: "grid",
  placeItems: "center",
  color: "#f8d978",
  fontSize: 10,
};
