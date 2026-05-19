"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { AuthResponse, User } from "@/types";

export default function OAuthSuccessPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");
    const userParam = searchParams.get("user");

    if (!token || !userParam) {
      alert("Google 로그인 처리 중 오류가 발생했습니다.");
      router.replace("/");
      return;
    }

    try {
      const parsedUser = JSON.parse(userParam) as AuthResponse;

      const user: User = {
        id: parsedUser.id,
        email: parsedUser.email,
        nickname: parsedUser.nickname,
        role: parsedUser.role,
        token,
        provider: parsedUser.provider,
        profileImageUrl: parsedUser.profileImageUrl,
      };

      localStorage.setItem("metagg_token", token);
      localStorage.setItem("metagg_user", JSON.stringify(user));

      router.replace("/");
    } catch {
      alert("Google 로그인 정보를 저장하지 못했습니다.");
      router.replace("/");
    }
  }, [router, searchParams]);

  return (
    <main
      style={{
        minHeight: "100vh",
        background: "#06080d",
        color: "white",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <p>Google 로그인 처리 중...</p>
    </main>
  );
}
