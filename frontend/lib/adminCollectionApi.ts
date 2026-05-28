import { apiFetch } from "@/lib/api";
import type { AdminCollectionStatus } from "@/types/admin-collection";

export async function fetchAdminCollectionStatus(): Promise<AdminCollectionStatus> {
  return apiFetch<AdminCollectionStatus>("/api/admin/collection/status");
}
