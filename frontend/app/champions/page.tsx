"use client";

import { useEffect, useMemo, useState } from "react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { fetchChampionStats } from "@/lib/championStatsApi";
import type { ChampionPosition, ChampionStat } from "@/types/champion-stat";
import ChampionDetailModal from "./ChampionDetailModal";
import styles from "./ChampionsPage.module.css";
import {
  fetchKoreanChampionNameMap,
  getKoreanChampionName,
  type ChampionNameMap,
} from "@/lib/ddragonChampionApi";


const POSITIONS: { key: ChampionPosition; label: string; kor: string }[] = [
  { key: "TOP", label: "TOP", kor: "탑" },
  { key: "JUNGLE", label: "JUNGLE", kor: "정글" },
  { key: "MIDDLE", label: "MID", kor: "미드" },
  { key: "BOTTOM", label: "ADC", kor: "원딜" },
  { key: "UTILITY", label: "SUP", kor: "서포터" },
];

type SortKey =
  | "tierScore"
  | "games"
  | "winRate"
  | "pickRate"
  | "avgKda"
  | "avgDamage";

const SORT_OPTIONS: { key: SortKey; label: string }[] = [
  { key: "tierScore", label: "티어 점수순" },
  { key: "games", label: "게임 수순" },
  { key: "winRate", label: "승률순" },
  { key: "pickRate", label: "픽률순" },
  { key: "avgKda", label: "KDA순" },
  { key: "avgDamage", label: "딜량순" },
];

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.round(value));
}

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

function tierRank(tier: string) {
  const order: Record<string, number> = {
    S: 1,
    A: 2,
    B: 3,
    C: 4,
    D: 5,
    "N/A": 9,
  };

  return order[tier] ?? 9;
}

function getChampionImageUrl(championName: string) {
  return `https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${championName}.png`;
}

function getTierClass(tier: string) {
  if (tier === "S") return styles.tierS;
  if (tier === "A") return styles.tierA;
  if (tier === "B") return styles.tierB;
  if (tier === "C") return styles.tierC;
  if (tier === "D") return styles.tierD;
  return styles.tierNA;
}

export default function ChampionsPage() {
  const [selectedPosition, setSelectedPosition] =
    useState<ChampionPosition>("TOP");

  const [stats, setStats] = useState<ChampionStat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [keyword, setKeyword] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("tierScore");
  const [showLowSample, setShowLowSample] = useState(false);
  const [championNameMap, setChampionNameMap] = useState<ChampionNameMap>({});
  const [selectedChampion, setSelectedChampion] =
    useState<ChampionStat | null>(null);

  useEffect(() => {
    let ignore = false;

    async function loadStats() {
      try {
        setLoading(true);
        setError("");

        const data = await fetchChampionStats(selectedPosition);

        if (!ignore) {
          setStats(data);
        }
      } catch (err) {
        if (!ignore) {
          setStats([]);
          setError(
            err instanceof Error
              ? err.message
              : "챔피언 통계를 불러오는 중 오류가 발생했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadStats();

    return () => {
      ignore = true;
    };
  }, [selectedPosition]);

  useEffect(() => {
  let ignore = false;

  async function loadChampionNames() {
    try {
      const map = await fetchKoreanChampionNameMap();

      if (!ignore) {
        setChampionNameMap(map);
      }
    } catch {
      if (!ignore) {
        setChampionNameMap({});
      }
    }
  }

  loadChampionNames();

  return () => {
    ignore = true;
  };
}, []);

  const latestPatch = useMemo(() => {
    return (
      stats
        .map((item) => item.patch)
        .filter(Boolean)
        .sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))[0] ??
      ""
    );
  }, [stats]);

  const filteredStats = useMemo(() => {
    const q = keyword.trim().toLowerCase();

    return [...stats]
      .filter((item) => !latestPatch || item.patch === latestPatch)
      .filter((item) => showLowSample || item.games >= 3)
      .filter((item) => {
        if (!q) return true;
          const koreanName = getKoreanChampionName(item.championName, championNameMap);

        return (
          item.championName.toLowerCase().includes(q) ||
          koreanName.toLowerCase().includes(q)
      );
      })
      .sort((a, b) => {
        if (sortKey === "tierScore") {
          const tierDiff = tierRank(a.tier) - tierRank(b.tier);
          if (tierDiff !== 0) return tierDiff;
        }

        const diff = b[sortKey] - a[sortKey];
        if (diff !== 0) return diff;

        return b.games - a.games;
      });
  }, [stats, latestPatch, keyword, showLowSample, sortKey, championNameMap]);

  const summary = useMemo(() => {
    const latestStats = latestPatch
      ? stats.filter((item) => item.patch === latestPatch)
      : stats;

    return {
      totalGames: latestStats.reduce((sum, item) => sum + item.games, 0),
      championCount: latestStats.length,
      topChampion: filteredStats[0]
  ? getKoreanChampionName(filteredStats[0].championName, championNameMap)
  : "-",
    };
  }, [stats, latestPatch, filteredStats, championNameMap]);

  return (
    <>
      <Header />

      <main className={styles.page}>
        <section className={styles.hero}>
          <div className={styles.container}>
            <div className={styles.heroInner}>
              <div>
                <p className={styles.eyebrow}>META GG CHAMPION TIER</p>
                <h1 className={styles.title}>데이터 기반 챔피언 티어표</h1>
                <p className={styles.description}>
                  Riot API로 수집한 KR 상위권 솔로랭크 데이터를 기반으로
                  포지션별 승률, 픽률, KDA, 딜량, CS를 계산합니다.
                </p>
              </div>

              <div className={styles.summaryGrid}>
                <SummaryCard label="현재 패치" value={latestPatch || "-"} />
                <SummaryCard
                  label="누적 표본"
                  value={formatNumber(summary.totalGames)}
                />
                <SummaryCard label="1위 챔피언" value={summary.topChampion} />
              </div>
            </div>
          </div>
        </section>

        <section className={styles.content}>
          <div className={styles.container}>
            <div className={styles.positionTabs}>
              {POSITIONS.map((position) => {
                const active = selectedPosition === position.key;

                return (
                  <button
                    key={position.key}
                    type="button"
                    onClick={() => setSelectedPosition(position.key)}
                    className={`${styles.positionButton} ${
                      active ? styles.positionButtonActive : ""
                    }`}
                  >
                    <span className={styles.positionLabel}>
                      {position.label}
                    </span>
                    <span className={styles.positionKor}>{position.kor}</span>
                  </button>
                );
              })}
            </div>

            <div className={styles.toolbar}>
              <div className={styles.toolbarLeft}>
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="챔피언 검색"
                  className={styles.searchInput}
                />

                <select
                  value={sortKey}
                  onChange={(event) =>
                    setSortKey(event.target.value as SortKey)
                  }
                  className={styles.select}
                >
                  {SORT_OPTIONS.map((option) => (
                    <option key={option.key} value={option.key}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              <label className={styles.checkboxLabel}>
                <input
                  type="checkbox"
                  checked={showLowSample}
                  onChange={(event) => setShowLowSample(event.target.checked)}
                />
                표본 부족 챔피언 포함
              </label>
            </div>

            {loading && (
              <div className={styles.stateBox}>통계를 불러오는 중입니다.</div>
            )}

            {!loading && error && (
              <div className={styles.stateBox}>{error}</div>
            )}

            {!loading && !error && (
              <div className={styles.tableCard}>
                <div className={styles.tableScroll}>
                  <table className={styles.table}>
                    <colgroup>
                      <col style={{ width: "74px" }} />
                      <col style={{ width: "300px" }} />
                      <col style={{ width: "100px" }} />
                      <col style={{ width: "100px" }} />
                      <col style={{ width: "100px" }} />
                      <col style={{ width: "100px" }} />
                      <col style={{ width: "130px" }} />
                      <col style={{ width: "100px" }} />
                      <col style={{ width: "100px" }} />
                    </colgroup>

                    <thead>
                      <tr>
                        <th className={styles.left}>순위</th>
                        <th className={styles.left}>챔피언</th>
                        <th>게임 수</th>
                        <th>승률</th>
                        <th>픽률</th>
                        <th>KDA</th>
                        <th>평균 딜량</th>
                        <th>CS</th>
                        <th>티어</th>
                      </tr>
                    </thead>

                    <tbody>
                      {filteredStats.length === 0 ? (
                        <tr>
                          <td colSpan={9} className={styles.left}>
                            표시할 데이터가 없습니다.
                          </td>
                        </tr>
                      ) : (
                        filteredStats.map((champion, index) => (
                          <ChampionRow
                            key={`${champion.patch}-${champion.position}-${champion.championId}-${champion.championName}`}
                            champion={champion}
                            rank={index + 1}
                            championNameMap={championNameMap}
                            onClick={() => setSelectedChampion(champion)}
                          />
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>

      <Footer />

      {selectedChampion && (
        <ChampionDetailModal
          champion={selectedChampion}
          championNameMap={championNameMap}
          onClose={() => setSelectedChampion(null)}
        />
      )}
    </>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.summaryCard}>
      <div className={styles.summaryLabel}>{label}</div>
      <div className={styles.summaryValue}>{value}</div>
    </div>
  );
}

function ChampionRow({
  champion,
  rank,
  championNameMap,
  onClick,
}: {
  champion: ChampionStat;
  rank: number;
  championNameMap: ChampionNameMap;
  onClick: () => void;
}) {
  const displayName = getKoreanChampionName(
  champion.championName,
  championNameMap
);
  return (
    <tr className={styles.clickableRow} onClick={onClick}>
      <td className={styles.left}>
        <span className={styles.rank}>#{rank}</span>
      </td>

      <td className={styles.left}>
        <div className={styles.championCell}>
          <img
            src={getChampionImageUrl(champion.championName)}
            alt={champion.championName}
            className={styles.championImage}
            onError={(event) => {
              event.currentTarget.src =
                "https://ddragon.leagueoflegends.com/cdn/15.24.1/img/profileicon/29.png";
            }}
          />

          <div className={styles.championInfo}>
            <div className={styles.championName}>{displayName}</div>
            <div className={styles.championMeta}>
              {champion.position} · Patch {champion.patch}
            </div>
          </div>
        </div>
      </td>

      <td>{formatNumber(champion.games)}</td>
      <td className={styles.winRate}>{formatPercent(champion.winRate)}</td>
      <td>{formatPercent(champion.pickRate)}</td>
      <td>{champion.avgKda.toFixed(2)}</td>
      <td>{formatNumber(champion.avgDamage)}</td>
      <td>{champion.avgCs.toFixed(1)}</td>

      <td>
        <span
          className={`${styles.tierBadge} ${getTierClass(champion.tier)}`}
        >
          {champion.tier}
        </span>
      </td>
    </tr>
  );
}