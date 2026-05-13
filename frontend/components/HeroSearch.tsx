"use client";

import { useState } from "react";
import { apiFetch } from "@/lib/api";
import type { RiotAccountResponse } from "@/types";

export default function HeroSearch() {
  const [gameName, setGameName] = useState("");
  const [tagLine, setTagLine] = useState("KR1");
  const [result, setResult] = useState<RiotAccountResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const quickFill = (name: string, tag: string) => {
    setGameName(name);
    setTagLine(tag);
    setResult(null);
    setError("");
  };

  const handleSearch = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const name = gameName.trim();
    const tag = tagLine.trim().replace("#", "") || "KR1";

    if (!name) {
      setError("소환사명을 입력해주세요.");
      setResult(null);
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const data = await apiFetch<RiotAccountResponse>(
        `/api/riot/account?gameName=${encodeURIComponent(name)}&tagLine=${encodeURIComponent(tag)}`
      );

      if (!data.puuid) {
        setError("Riot ID를 찾을 수 없습니다.");
        return;
      }

      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Riot API 조회 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="hero" id="searchSection">
      <div className="container">
        <span className="eyebrow">DATA · META · VICTORY</span>
        <h1>META GG</h1>
        <p className="sub">
          전적 검색을 넘어, <b>승리를 위한 데이터</b>를 분석합니다.
        </p>

        <form className="search-wrap" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="소환사명을 입력하세요"
            autoComplete="off"
            value={gameName}
            onChange={(e) => setGameName(e.target.value)}
          />
          <input
            className="tag-input"
            type="text"
            placeholder="#KR1"
            maxLength={6}
            value={tagLine}
            onChange={(e) => setTagLine(e.target.value)}
          />
          <button type="submit" className="search-btn" disabled={loading}>
            {loading ? "SEARCHING" : "SEARCH"}
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

        {result && (
          <div className="search-result-box">
            <h3>RIOT API 조회 성공</h3>
            <p>
              <strong>gameName:</strong> {result.gameName}
            </p>
            <p>
              <strong>tagLine:</strong> {result.tagLine}
            </p>
            <p>
              <strong>puuid:</strong> {result.puuid}
            </p>
          </div>
        )}
      </div>
    </section>
  );
}
