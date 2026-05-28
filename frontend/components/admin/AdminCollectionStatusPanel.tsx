"use client";

import { useEffect, useState } from "react";
import { fetchAdminCollectionStatus } from "@/lib/adminCollectionApi";
import type { AdminCollectionStatus } from "@/types/admin-collection";

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(value ?? 0);
}

function formatDateTime(value: string | null) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("ko-KR");
}

function formatMs(ms: number) {
  if (!ms) return "-";
  if (ms >= 60000) return `${Math.round(ms / 60000)}분`;
  return `${Math.round(ms / 1000)}초`;
}

export default function AdminCollectionStatusPanel() {
  const [status, setStatus] = useState<AdminCollectionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadStatus = async () => {
    try {
      setError("");
      const data = await fetchAdminCollectionStatus();
      setStatus(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "자동 수집 현황을 불러오지 못했습니다."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();

    const timer = window.setInterval(() => {
      loadStatus();
    }, 10000);

    return () => {
      window.clearInterval(timer);
    };
  }, []);

  return (
    <div className="admin-log-box" style={{ marginTop: "24px" }}>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: "12px",
          marginBottom: "16px",
        }}
      >
        <div>
          <h3 style={{ marginBottom: "6px" }}>실시간 자동 수집 현황</h3>
          <p style={{ margin: 0 }}>
            Riot API 자동 수집 상태와 DB 저장량을 10초마다 갱신합니다.
          </p>
        </div>

        <button className="btn btn-gold" onClick={loadStatus} type="button">
          새로고침
        </button>
      </div>

      {loading && <p className="success-message">수집 현황 불러오는 중...</p>}
      {error && <p className="error-message">{error}</p>}

      {!loading && !error && status && (
        <>
          <div className="status-grid">
            <StatusBox label="수집 경기" value={formatNumber(status.matchCount)} />
            <StatusBox
              label="참가자 데이터"
              value={formatNumber(status.participantCount)}
            />
            <StatusBox
              label="통계 Row"
              value={formatNumber(status.championStatCount)}
            />
            <StatusBox
              label="Seed 유저"
              value={`${formatNumber(status.enabledSeedPlayerCount)} / ${formatNumber(
                status.seedPlayerCount
              )}`}
            />
            <StatusBox
              label="랭킹 유저"
              value={formatNumber(status.rankingPlayerCount)}
            />
            <StatusBox
              label="실패 Seed"
              value={formatNumber(status.failedSeedCount)}
            />
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
              gap: "14px",
              marginTop: "18px",
            }}
          >
            <div className="status-box">
              <strong>자동 수집 설정</strong>
              <InfoLine
                label="자동 수집"
                value={status.autoCollectEnabled ? "ON" : "OFF"}
                accent={status.autoCollectEnabled ? "#5ff0bd" : "#ff8a96"}
              />
              <InfoLine
                label="유저당 수집 경기"
                value={`${status.matchCountPerPlayer}경기`}
              />
              <InfoLine
                label="사이클당 처리 유저"
                value={`${status.maxPlayersPerCycle}명`}
              />
              <InfoLine label="사이클 주기" value={formatMs(status.fixedDelayMs)} />
              <InfoLine
                label="유저 사이 대기"
                value={formatMs(status.delayBetweenPlayersMs)}
              />
            </div>

            <div className="status-box">
              <strong>최근 처리 시간</strong>
              <InfoLine
                label="마지막 Seed 수집"
                value={formatDateTime(status.lastCollectedAt)}
              />
              <InfoLine
                label="마지막 경기 저장"
                value={formatDateTime(status.lastMatchCreatedAt)}
              />
              <InfoLine
                label="마지막 통계 갱신"
                value={formatDateTime(status.lastStatsUpdatedAt)}
              />
              <InfoLine
                label="Seed 누적 저장 경기"
                value={formatNumber(status.totalSavedMatchesBySeeds)}
              />
            </div>
          </div>

          <div className="admin-table" style={{ marginTop: "18px" }}>
            <div
              className="admin-row admin-row-head"
              style={{ gridTemplateColumns: "1.4fr 0.55fr 0.65fr 0.55fr 1.1fr 1.8fr" }}
            >
              <span>Riot ID</span>
              <span>상태</span>
              <span>저장 경기</span>
              <span>실패</span>
              <span>마지막 수집</span>
              <span>결과</span>
            </div>

            {status.recentSeeds.length === 0 ? (
              <div className="admin-row" style={{ gridTemplateColumns: "1fr" }}>
                <span>최근 자동 수집 Seed가 없습니다.</span>
              </div>
            ) : (
              status.recentSeeds.map((seed) => (
                <div
                  className="admin-row"
                  key={seed.id}
                  style={{ gridTemplateColumns: "1.4fr 0.55fr 0.65fr 0.55fr 1.1fr 1.8fr" }}
                >
                  <span>{seed.gameName}#{seed.tagLine}</span>
                  <span style={{ color: seed.enabled ? "#5ff0bd" : "#a7b0c0", fontWeight: 800 }}>
                    {seed.enabled ? "ON" : "OFF"}
                  </span>
                  <span>{formatNumber(seed.totalSavedMatches)}</span>
                  <span style={{ color: seed.totalFailedCount > 0 ? "#ff8a96" : undefined }}>
                    {formatNumber(seed.totalFailedCount)}
                  </span>
                  <span>{formatDateTime(seed.lastCollectedAt)}</span>
                  <span title={seed.lastResultMessage ?? ""} style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {seed.lastResultMessage ?? "-"}
                  </span>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}

function StatusBox({ label, value }: { label: string; value: string }) {
  return (
    <div className="status-box">
      <strong>{label}</strong>
      <span>{value}</span>
    </div>
  );
}

function InfoLine({
  label,
  value,
  accent,
}: {
  label: string;
  value: string;
  accent?: string;
}) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        gap: "12px",
        marginTop: "10px",
        color: "#c5cbd9",
        fontSize: "13px",
      }}
    >
      <span>{label}</span>
      <b style={{ color: accent ?? "var(--gold-2)", textAlign: "right" }}>{value}</b>
    </div>
  );
}
