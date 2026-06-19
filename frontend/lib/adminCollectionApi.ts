import { apiFetch } from "@/lib/api";
import type {
  AdminCollectionStatus,
  AutoCollectionRunResult,
  StatsRebuildResult,
} from "@/types/admin-collection";

export async function fetchAdminCollectionStatus(): Promise<AdminCollectionStatus> {
  return apiFetch<AdminCollectionStatus>("/api/admin/collection/status");
}

export async function runAdminCollectionNow(): Promise<AutoCollectionRunResult> {
  return apiFetch<AutoCollectionRunResult>("/api/admin/collection/run", {
    method: "POST",
  });
}

export async function rebuildLatestChampionStats(): Promise<StatsRebuildResult> {
  return apiFetch<StatsRebuildResult>("/api/admin/collection/stats/rebuild", {
    method: "POST",
  });
}
