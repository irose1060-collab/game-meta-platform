"use client";

import { useCallback, useEffect, useState } from "react";
import type { User } from "@/types";

export function useCurrentUser() {
  const [user, setUser] = useState<User | null>(null);

  const refreshUser = useCallback(() => {
    const savedUser = localStorage.getItem("metagg_user");

    if (!savedUser) {
      setUser(null);
      return;
    }

    try {
      setUser(JSON.parse(savedUser));
    } catch {
      localStorage.removeItem("metagg_user");
      localStorage.removeItem("metagg_token");
      setUser(null);
    }
  }, []);

  useEffect(() => {
    refreshUser();

    const handleStorageChange = () => {
      refreshUser();
    };

    window.addEventListener("storage", handleStorageChange);
    window.addEventListener("pageshow", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
      window.removeEventListener("pageshow", handleStorageChange);
    };
  }, [refreshUser]);

  const logout = useCallback(() => {
    localStorage.removeItem("metagg_user");
    localStorage.removeItem("metagg_token");
    setUser(null);
    window.location.href = "/";
  }, []);

  const goHomeForAuth = useCallback(() => {
    window.location.href = "/";
  }, []);

  return {
    user,
    setUser,
    refreshUser,
    logout,
    goHomeForAuth,
  };
}
