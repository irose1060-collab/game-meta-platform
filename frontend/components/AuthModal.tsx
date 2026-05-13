"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { AuthResponse, User } from "@/types";

type AuthModalProps = {
  isOpen: boolean;
  initialMode: "login" | "signup";
  onClose: () => void;
  onLoginSuccess: (user: User) => void;
};

export default function AuthModal({
  isOpen,
  initialMode,
  onClose,
  onLoginSuccess,
}: AuthModalProps) {
  const [mode, setMode] = useState<"login" | "signup">(initialMode);
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [message, setMessage] = useState("");
  const [isError, setIsError] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setMode(initialMode);
      setMessage("");
      setIsError(false);
    }
  }, [isOpen, initialMode]);

  if (!isOpen) return null;

  const switchMode = () => {
    setMessage("");
    setIsError(false);
    setMode((prev) => (prev === "login" ? "signup" : "login"));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage("");
    setIsError(false);
    setLoading(true);

    try {
      if (mode === "signup") {
        if (!nickname.trim()) {
          setIsError(true);
          setMessage("닉네임을 입력해주세요.");
          return;
        }

        if (password !== passwordConfirm) {
          setIsError(true);
          setMessage("비밀번호가 일치하지 않습니다.");
          return;
        }

        await apiFetch<string | { message?: string }>("/api/auth/signup", {
          method: "POST",
          body: JSON.stringify({ email, nickname, password }),
        });

        setIsError(false);
        setMessage("회원가입이 완료되었습니다. 로그인해주세요.");
        setMode("login");
        return;
      }

      const authData = await apiFetch<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      if (!authData.token) {
        setIsError(true);
        setMessage("로그인 응답에 토큰이 없습니다.");
        return;
      }

      const user: User = {
        id: authData.id,
        email: authData.email,
        nickname: authData.nickname,
        role: authData.role,
        token: authData.token,
      };

      localStorage.setItem("metagg_token", authData.token);
      localStorage.setItem("metagg_user", JSON.stringify(user));

      setIsError(false);
      setMessage(`${user.nickname}님 로그인 성공`);
      onLoginSuccess(user);

      setTimeout(() => {
        onClose();
      }, 600);
    } catch (err) {
      setIsError(true);
      setMessage(err instanceof Error ? err.message : "서버 연결에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(event) => event.stopPropagation()}>
        <button className="close" onClick={onClose}>
          ✕
        </button>

        <h3>{mode === "login" ? "LOGIN" : "SIGN UP"}</h3>
        <div className="modal-sub">
          {mode === "login" ? "소환사의 협곡으로 입장하세요" : "META GG에 합류하세요"}
        </div>

        <form onSubmit={handleSubmit}>
          {mode === "signup" && (
            <div className="field">
              <label>닉네임</label>
              <input
                type="text"
                placeholder="Summoner"
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
              />
            </div>
          )}

          <div className="field">
            <label>이메일</label>
            <input
              type="email"
              placeholder="you@metagg.gg"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="field">
            <label>비밀번호</label>
            <input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>

          {mode === "signup" && (
            <div className="field">
              <label>비밀번호 확인</label>
              <input
                type="password"
                placeholder="••••••••"
                value={passwordConfirm}
                onChange={(event) => setPasswordConfirm(event.target.value)}
                required
              />
            </div>
          )}

          {message && (
            <div className={isError ? "modal-msg error" : "modal-msg ok"}>
              {message}
            </div>
          )}

          <div className="modal-actions">
            <button className="btn btn-gold" type="submit" disabled={loading}>
              {loading ? "처리 중..." : mode === "login" ? "로그인" : "가입하기"}
            </button>
          </div>
        </form>

        <div className="modal-foot">
          {mode === "login" ? "계정이 없으신가요?" : "이미 계정이 있으신가요?"}{" "}
          <button type="button" className="button-link" onClick={switchMode}>
            {mode === "login" ? "회원가입" : "로그인"}
          </button>
        </div>
      </div>
    </div>
  );
}
