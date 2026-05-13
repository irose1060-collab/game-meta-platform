"use client";

import type { User } from "@/types";

type HeaderProps = {
  user: User | null;
  onLoginClick: () => void;
  onSignupClick: () => void;
  onLogoutClick: () => void;
};

export default function Header({
  user,
  onLoginClick,
  onSignupClick,
  onLogoutClick,
}: HeaderProps) {
  const scrollToSearch = () => {
    document.getElementById("searchSection")?.scrollIntoView({ behavior: "smooth" });
  };

  const goToPage = (pathName: string) => {
    alert(`[이동] ${pathName} 페이지로 이동합니다.`);
  };

  return (
    <header className="header">
      <div className="container nav">
        <button className="logo" onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}>
          META GG
        </button>

        <nav className="menu">
          <button onClick={scrollToSearch}>전적 검색</button>
          <button onClick={() => goToPage("/champions")}>챔피언 티어</button>
          <button onClick={() => goToPage("/stats")}>통계 분석</button>
          <button onClick={() => goToPage("/patches")}>패치 정보</button>
        </nav>

        <div className="auth">
          {user ? (
            <>
              <span className="user-nickname">{user.nickname}님</span>
              {user.role === "ADMIN" && (
                <button className="btn btn-ghost admin-button" onClick={() => goToPage("/admin")}>
                  관리자
                </button>
              )}
              <button className="btn btn-ghost" onClick={onLogoutClick}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button className="btn btn-ghost" onClick={onLoginClick}>
                로그인
              </button>
              <button className="btn btn-gold" onClick={onSignupClick}>
                회원가입
              </button>
            </>
          )}
          <button className="hamburger" onClick={() => alert("모바일 메뉴 열기 구현 예정")}>
            ☰
          </button>
        </div>
      </div>
    </header>
  );
}
