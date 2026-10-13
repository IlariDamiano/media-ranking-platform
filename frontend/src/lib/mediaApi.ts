export type MediaType =
  | "MOVIE"
  | "TV_SHOW"
  | "ANIME"
  | "MANGA"
  | "MANHWA"
  | "MANHUA";

export interface MediaSummary {
  id: string;
  title: string;
  mediaType: MediaType;
  posterUrl: string | null;
}

export interface MediaPage {
  content: MediaSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal });

  if (!response.ok) {
    throw new Error(`API request failed: HTTP ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getMedia(
  page = 0,
  mediaType?: MediaType,
  signal?: AbortSignal,
): Promise<MediaPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: "24",
  });

  if (mediaType) {
    params.set("mediaType", mediaType);
  }

  return getJson<MediaPage>(`/api/media?${params.toString()}`, signal);
}

export function searchMedia(
  query: string,
  signal?: AbortSignal,
): Promise<MediaSummary[]> {
  const params = new URLSearchParams({ query });

  return getJson<MediaSummary[]>(
    `/api/media/search?${params.toString()}`,
    signal,
  );
}
