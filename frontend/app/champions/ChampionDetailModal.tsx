"use client";

import { useEffect, useState } from "react";
import { fetchChampionDetail } from "@/lib/championDetailApi";
import type { ChampionDetail } from "@/types/champion-detail";
import type { ChampionStat } from "@/types/champion-stat";
import styles from "./ChampionsPage.module.css";
import {
  getKoreanChampionName,
  type ChampionNameMap,
} from "@/lib/ddragonChampionApi";

const DDRAGON_VERSION =
  process.env.NEXT_PUBLIC_DDRAGON_VERSION ?? "15.24.1";

const SPELL_MAP: Record<number, { key: string; name: string }> = {
  1: { key: "SummonerBoost", name: "정화" },
  3: { key: "SummonerExhaust", name: "탈진" },
  4: { key: "SummonerFlash", name: "점멸" },
  6: { key: "SummonerHaste", name: "유체화" },
  7: { key: "SummonerHeal", name: "회복" },
  11: { key: "SummonerSmite", name: "강타" },
  12: { key: "SummonerTeleport", name: "순간이동" },
  13: { key: "SummonerMana", name: "총명" },
  14: { key: "SummonerDot", name: "점화" },
  21: { key: "SummonerBarrier", name: "방어막" },
  32: { key: "SummonerSnowball", name: "표식" },
};

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.round(value));
}

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

function championImage(championName: string) {
  return `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/champion/${championName}.png`;
}

function itemImage(itemId: number) {
  return `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/item/${itemId}.png`;
}

function spellImage(spellId: number) {
  const spell = SPELL_MAP[spellId];
  if (!spell) return "";
  return `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/spell/${spell.key}.png`;
}

function spellName(spellId: number) {
  return SPELL_MAP[spellId]?.name ?? `Spell ${spellId}`;
}

export default function ChampionDetailModal({
  champion,
  championNameMap,
  onClose,
}: {
  champion: ChampionStat;
  championNameMap: ChampionNameMap;
  onClose: () => void;
}) {
  const displayName = getKoreanChampionName(
    champion.championName,
    championNameMap
  );

  const [detail, setDetail] = useState<ChampionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;

    async function load() {
      try {
        setLoading(true);
        setError("");

        const data = await fetchChampionDetail({
          championId: champion.championId,
          position: champion.position,
          patch: champion.patch,
        });

        if (!ignore) {
          setDetail(data);
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err instanceof Error
              ? err.message
              : "챔피언 상세 정보를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      ignore = true;
    };
  }, [champion]);

  return (
    <div className={styles.modalBackdrop} onClick={onClose}>
      <section
        className={styles.detailModal}
        onClick={(event) => event.stopPropagation()}
      >
        <button type="button" className={styles.modalClose} onClick={onClose}>
          ×
        </button>

        <div className={styles.detailHeader}>
          <img
            src={championImage(champion.championName)}
            alt={displayName}
            className={styles.detailChampionImage}
            onError={(event) => {
              event.currentTarget.src = `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/profileicon/29.png`;
            }}
          />

          <div>
            <div className={styles.detailEyebrow}>
              {champion.position} · Patch {champion.patch}
            </div>
            <h2 className={styles.detailTitle}>{displayName}</h2>
            <p className={styles.detailDesc}>
              {champion.championName} · 수집된 매치 데이터를 기반으로 스펠,
              아이템, 상대 챔피언 통계를 분석합니다.
            </p>
          </div>
        </div>

        {loading && (
          <div className={styles.detailState}>상세 정보를 불러오는 중입니다.</div>
        )}

        {!loading && error && <div className={styles.detailState}>{error}</div>}

        {!loading && detail && (
          <div className={styles.detailBody}>
            <div className={styles.basicGrid}>
              <DetailMetric label="티어" value={detail.basic.tier} emphasis />
              <DetailMetric
                label="게임 수"
                value={formatNumber(detail.basic.games)}
              />
              <DetailMetric
                label="승률"
                value={formatPercent(detail.basic.winRate)}
                emphasis
              />
              <DetailMetric
                label="픽률"
                value={formatPercent(detail.basic.pickRate)}
              />
              <DetailMetric
                label="KDA"
                value={detail.basic.avgKda.toFixed(2)}
              />
              <DetailMetric
                label="평균 딜량"
                value={formatNumber(detail.basic.avgDamage)}
              />
              <DetailMetric
                label="평균 골드"
                value={formatNumber(detail.basic.avgGold)}
              />
              <DetailMetric
                label="평균 CS"
                value={detail.basic.avgCs.toFixed(1)}
              />
            </div>

            <div className={styles.detailSectionGrid}>
              <DetailPanel title="추천 스펠">
                {detail.spells.length === 0 ? (
                  <EmptyText />
                ) : (
                  detail.spells.map((spell) => (
                    <div
                      key={`${spell.spell1Id}-${spell.spell2Id}`}
                      className={styles.spellRow}
                    >
                      <div className={styles.iconPair}>
                        {[spell.spell1Id, spell.spell2Id].map((id) => (
                          <img
                            key={id}
                            src={spellImage(id)}
                            alt={spellName(id)}
                            title={spellName(id)}
                            className={styles.smallIcon}
                          />
                        ))}
                      </div>

                      <div className={styles.rowText}>
                        {spellName(spell.spell1Id)} +{" "}
                        {spellName(spell.spell2Id)}
                      </div>

                      <div className={styles.rowMeta}>
                        {spell.games}게임 · 승률 {formatPercent(spell.winRate)}
                      </div>
                    </div>
                  ))
                )}
              </DetailPanel>

              <DetailPanel title="많이 사용한 코어템">
                {detail.items.length === 0 ? (
                  <EmptyText />
                ) : (
                  <div className={styles.itemGrid}>
                    {detail.items.map((item) => (
                      <div key={item.itemId} className={styles.itemCard}>
                        <img
                          src={itemImage(item.itemId)}
                          alt={`item-${item.itemId}`}
                          className={styles.itemIcon}
                        />
                        <div className={styles.itemMeta}>
                          <strong>{item.games}회</strong>
                          <span>{formatPercent(item.winRate)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </DetailPanel>

              <DetailPanel title="상대하기 어려운 챔피언">
                {detail.hardCounters.length === 0 ? (
                  <EmptyText />
                ) : (
                  detail.hardCounters.map((counter) => (
                    <CounterRow
                      key={counter.enemyChampionId}
                      counter={counter}
                      championNameMap={championNameMap}
                    />
                  ))
                )}
              </DetailPanel>

              <DetailPanel title="상대하기 좋은 챔피언">
                {detail.easyMatchups.length === 0 ? (
                  <EmptyText />
                ) : (
                  detail.easyMatchups.map((counter) => (
                    <CounterRow
                      key={counter.enemyChampionId}
                      counter={counter}
                      championNameMap={championNameMap}
                    />
                  ))
                )}
              </DetailPanel>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function DetailMetric({
  label,
  value,
  emphasis = false,
}: {
  label: string;
  value: string;
  emphasis?: boolean;
}) {
  return (
    <div className={styles.detailMetric}>
      <div className={styles.detailMetricLabel}>{label}</div>
      <div
        className={`${styles.detailMetricValue} ${
          emphasis ? styles.detailMetricEmphasis : ""
        }`}
      >
        {value}
      </div>
    </div>
  );
}

function DetailPanel({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className={styles.detailPanel}>
      <h3>{title}</h3>
      <div className={styles.detailPanelBody}>{children}</div>
    </section>
  );
}

function CounterRow({
  counter,
  championNameMap,
}: {
  counter: {
    enemyChampionName: string;
    games: number;
    winRate: number;
  };
  championNameMap: ChampionNameMap;
}) {
  const displayName = getKoreanChampionName(
    counter.enemyChampionName,
    championNameMap
  );

  return (
    <div className={styles.counterRow}>
      <img
        src={championImage(counter.enemyChampionName)}
        alt={displayName}
        className={styles.smallIcon}
      />
      <div className={styles.rowText}>{displayName}</div>
      <div className={styles.rowMeta}>
        {counter.games}게임 · 내 승률 {formatPercent(counter.winRate)}
      </div>
    </div>
  );
}

function EmptyText() {
  return <div className={styles.emptyText}>표시할 데이터가 부족합니다.</div>;
}