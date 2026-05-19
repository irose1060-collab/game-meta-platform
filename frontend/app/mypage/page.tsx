"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { User } from "@/types";

export default function MyPage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const savedUser = localStorage.getItem("metagg_user");

    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem("metagg_user");
        localStorage.removeItem("metagg_token");
      }
    }

    setLoaded(true);
  }, []);

  if (!loaded) {
    return (
      <main className="notice-page">
        <div className="admin-panel">
          <h1>마이페이지 로딩 중...</h1>
        </div>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="notice-page">
        <div className="admin-panel">
          <h1>로그인이 필요합니다</h1>
          <p>마이페이지는 로그인 후 이용할 수 있습니다.</p>
          <button className="btn btn-gold" onClick={() => router.push("/")}>
            홈으로
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="notice-page">
      <section className="notice-hero">
        <span className="eyebrow">MY PAGE</span>
        <h1>마이페이지</h1>
        <p>{user.nickname}님의 개인화 정보를 확인하는 페이지입니다.</p>
      </section>

      <section className="notice-layout">
        <div className="notice-list-panel">
          <div className="card-tag">Profile</div>
          <h2>회원 정보</h2>
          <p>이메일: {user.email}</p>
          <p>닉네임: {user.nickname}</p>
          <p>권한: {user.role}</p>
          <p>가입 방식: {user.provider || "LOCAL"}</p>

          <button className="btn btn-gold" onClick={() => router.push("/")}>
            홈으로
          </button>
        </div>

        <div className="notice-detail-panel">
          <h2>검색 기록 / 즐겨찾기</h2>
          <p className="notice-detail-content">
            추후 search_history, favorites 테이블을 기반으로 최근 검색 기록과 즐겨찾기 소환사를 표시합니다.
          </p>
        </div>
      </section>
    </main>
  );
}
