import { useState } from "react";

interface PosterArtworkProps {
  url: string | null;
}

export default function PosterArtwork({ url }: PosterArtworkProps) {
  const [loadedUrl, setLoadedUrl] = useState<string | null>(null);
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  if (!url || failedUrl === url) {
    return null;
  }

  return (
    <img
      className={`poster-image${loadedUrl === url ? " is-loaded" : ""}`}
      src={url}
      alt=""
      loading="lazy"
      decoding="async"
      onLoad={() => setLoadedUrl(url)}
      onError={() => setFailedUrl(url)}
    />
  );
}
