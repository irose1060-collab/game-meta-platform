"use client";

import { useRouter } from "next/navigation";
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
  const router = useRouter();
  const isAdmin = user?.role?.toUpperCase() === "ADMIN";

  const scrollToSearch = () => {
    if (window.location.pathname !== "/") {
      router.push("/");
      setTimeout(() => {
        document.getElementById("searchSection")?.scrollIntoView({ behavior: "smooth" });
      }, 300);
      return;
    }

    document.getElementById("searchSection")?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <header className="header">
      <div className="container nav">
        <button className="logo" onClick={() => router.push("/")}>
          META GG
        </button>

        <nav className="menu">
          <button onClick={scrollToSearch}>전적 검색</button>
          <button onClick={() => router.push("/champions")}>챔피언 티어</button>
          <button onClick={() => router.push("/stats")}>통계 분석</button>
          <button onClick={() => router.push("/patches")}>패치 정보</button>
        </nav>

        <div className="auth">
          {user ? (
            isAdmin ? (
              <>
                <button className="btn btn-gold" onClick={() => router.push("/admin")}>
                  관리자 페이지
                </button>
                <button className="btn btn-ghost" onClick={onLogoutClick}>
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <span className="user-nickname">{user.nickname}님</span>
                <button className="btn btn-ghost" onClick={() => router.push("/mypage")}>
                  마이페이지
                </button>
                <button className="btn btn-gold" onClick={onLogoutClick}>
                  로그아웃
                </button>
              </>
            )
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
        </div>
      </div>
    </header>
  );
}
