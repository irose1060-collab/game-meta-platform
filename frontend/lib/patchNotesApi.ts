import { apiFetch } from "@/lib/api";
import type { PatchNote } from "@/types/patch-note";

export async function fetchPatchNotes(): Promise<PatchNote[]> {
  return apiFetch<PatchNote[]>("/api/patches");
}
