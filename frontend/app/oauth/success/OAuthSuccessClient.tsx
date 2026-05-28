"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

type OAuthUser = {
  id?: number;
  email?: string;
  nickname?: string;
  role?: string;
};

export default function OAuthSuccessClient() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");
    const userParam = searchParams.get("user");

    if (!token) {
      router.replace("/?login=failed");
      return;
    }

    localStorage.setItem("metagg_token", token);

    if (userParam) {
      try {
        const decodedUser = decodeURIComponent(userParam);
        const user = JSON.parse(decodedUser) as OAuthUser;
        localStorage.setItem("metagg_user", JSON.stringify(user));
      } catch {
        // user 파싱 실패해도 token은 저장됐으니 홈으로 이동
      }
    }

    router.replace("/");
  }, [router, searchParams]);

  return (
    <main
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#070a12",
        color: "#f5c542",
        fontWeight: 800,
      }}
    >
      로그인 처리 중입니다...
    </main>
  );
}