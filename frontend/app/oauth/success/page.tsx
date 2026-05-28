import { Suspense } from "react";
import OAuthSuccessClient from "./OAuthSuccessClient";

export default function OAuthSuccessPage() {
  return (
    <Suspense
      fallback={
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
      }
    >
      <OAuthSuccessClient />
    </Suspense>
  );
}