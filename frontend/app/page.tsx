"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import HeroSearch from "@/components/HeroSearch";
import SummaryCards from "@/components/SummaryCards";
import NoticePreview from "@/components/NoticePreview";
import AuthModal from "@/components/AuthModal";
import NoticeModal from "@/components/NoticeModal";
import Footer from "@/components/Footer";
import type { Notice, User } from "@/types";

export default function HomePage() {
  const [user, setUser] = useState<User | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "signup">("login");
  const [selectedNotice, setSelectedNotice] = useState<Notice | null>(null);

  useEffect(() => {
    const savedUser = localStorage.getItem("metagg_user");
    if (!savedUser) return;

    try {
      setUser(JSON.parse(savedUser));
    } catch {
      localStorage.removeItem("metagg_user");
      localStorage.removeItem("metagg_token");
    }
  }, []);

  const openLogin = () => {
    setAuthMode("login");
    setAuthOpen(true);
  };

  const openSignup = () => {
    setAuthMode("signup");
    setAuthOpen(true);
  };

  const handleLogout = () => {
    localStorage.removeItem("metagg_user");
    localStorage.removeItem("metagg_token");
    setUser(null);
  };

  return (
    <>
      <Header
        user={user}
        onLoginClick={openLogin}
        onSignupClick={openSignup}
        onLogoutClick={handleLogout}
      />

      <main>
        <HeroSearch />
        <SummaryCards />
        <NoticePreview onNoticeClick={setSelectedNotice} />
      </main>

      <Footer />

      <AuthModal
        isOpen={authOpen}
        initialMode={authMode}
        onClose={() => setAuthOpen(false)}
        onLoginSuccess={setUser}
      />

      <NoticeModal notice={selectedNotice} onClose={() => setSelectedNotice(null)} />
    </>
  );
}
