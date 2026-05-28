"use client";

import React from "react";
import Link from "next/link";
import { User } from "@/types";

interface HeaderProps {
  user?: User | null;
  onLoginClick?: () => void;
  onSignupClick?: () => void;
  onLogoutClick?: () => void;
}

const Header: React.FC<HeaderProps> = ({
  user = null,
  onLoginClick,
  onSignupClick,
  onLogoutClick,
}) => {
  const handleLoginClick = () => {
    if (onLoginClick) {
      onLoginClick();
      return;
    }

    window.location.href = "/";
  };

  const handleSignupClick = () => {
    if (onSignupClick) {
      onSignupClick();
      return;
    }

    window.location.href = "/";
  };

  const handleLogoutClick = () => {
    if (onLogoutClick) {
      onLogoutClick();
      return;
    }

    localStorage.removeItem("metagg_user");
    localStorage.removeItem("metagg_token");
    window.location.href = "/";
  };

  return (
    <header className="header">
      <div className="container header-content">
        <Link href="/" className="logo">
          META GG
        </Link>

        <nav className="main-nav">
          <ul>
            <li>
              <Link href="/#search-section">전적 검색</Link>
            </li>
            <li>
              <Link href="/champions">챔피언 티어</Link>
            </li>
            <li>
              <Link href="/statistics">통계 분석</Link>
            </li>
            <li>
              <Link href="/patches">패치 정보</Link>
            </li>
          </ul>
        </nav>

        <div className="auth-section">
          {user ? (
            <>
              <span className="user-nickname">{user.nickname}님</span>

              {user.role === "ADMIN" && (
                <Link href="/admin" className="button-secondary admin-button">
                  관리자
                </Link>
              )}

              <button className="button-secondary" onClick={handleLogoutClick}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button className="button-secondary" onClick={handleLoginClick}>
                로그인
              </button>
              <button className="button-primary" onClick={handleSignupClick}>
                회원가입
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;