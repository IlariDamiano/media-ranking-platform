import { useEffect, useState } from "react";

import {
  getMedia,
  searchMedia,
  type MediaSummary,
  type MediaType,
} from "../lib/mediaApi";

type Category = "All" | MediaType;
type LoadState = "loading" | "ready" | "error";

const categories: { label: string; value: Category }[] = [
  { label: "All", value: "All" },
  { label: "Films", value: "MOVIE" },
  { label: "TV Shows", value: "TV_SHOW" },
  { label: "Anime", value: "ANIME" },
  { label: "Manga", value: "MANGA" },
  { label: "Manhwa", value: "MANHWA" },
  { label: "Manhua", value: "MANHUA" },
];

const categoryLabels: Record<MediaType, string> = {
  MOVIE: "Film",
  TV_SHOW: "TV Show",
  ANIME: "Anime",
  MANGA: "Manga",
  MANHWA: "Manhwa",
  MANHUA: "Manhua",
};

const posterTones = ["crimson", "violet", "blue", "rose"] as const;

function posterTone(id: string) {
  const hash = Array.from(id).reduce(
    (total, character) => total + character.charCodeAt(0),
    0,
  );

  return posterTones[hash % posterTones.length];
}

export default function DiscoverPage() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [category, setCategory] = useState<Category>("All");
  const [page, setPage] = useState(0);

  const [media, setMedia] = useState<MediaSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loadState, setLoadState] = useState<LoadState>("loading");

  const [errorMessage, setErrorMessage] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  // Wait 350ms after typing stops before updating the query.
  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(search.trim());
    }, 350);

    return () => window.clearTimeout(timeoutId);
  }, [search]);

  // Fetch again when the query, category, page, or retry changes.
  useEffect(() => {
    const controller = new AbortController();

    async function loadMedia() {
      setLoadState("loading");
      setErrorMessage("");

      try {
        if (debouncedSearch.length > 0) {
          const results = await searchMedia(debouncedSearch, controller.signal);

          if (controller.signal.aborted) return;

          // Search currently returns up to 20 matches.
          // Category filtering happens on those matches.
          const filtered =
            category === "All"
              ? results
              : results.filter((item) => item.mediaType === category);

          setMedia(filtered);
          setTotalElements(filtered.length);
          setTotalPages(0);
        } else {
          const result = await getMedia(
            page,
            category === "All" ? undefined : category,
            controller.signal,
          );

          if (controller.signal.aborted) return;

          setMedia(result.content);
          setTotalElements(result.totalElements);
          setTotalPages(result.totalPages);
        }

        setLoadState("ready");
      } catch (error) {
        if (controller.signal.aborted) return;

        setErrorMessage(
          error instanceof Error
            ? error.message
            : "An unexpected error occurred.",
        );

        setLoadState("error");
      }
    }

    void loadMedia();

    // Cancel old requests when filters change or the page unmounts.
    return () => controller.abort();
  }, [debouncedSearch, category, page, retryKey]);

  const isWaitingForDebounce = search.trim() !== debouncedSearch;

  const isLoading = isWaitingForDebounce || loadState === "loading";

  const isSearching = debouncedSearch.length > 0;

  function updateSearch(value: string) {
    setSearch(value);
    setPage(0);
  }

  function updateCategory(value: Category) {
    setCategory(value);
    setPage(0);
  }

  return (
    <>
      <section className="discovery-hero">
        <div className="hero-copy">
          <p className="eyebrow">THE MEDIA ARCHIVE / VOL. 001</p>

          <h1>
            DISCOVER
            <span>BEYOND</span>
            THE FRAME.
          </h1>

          <p className="hero-description">
            Movies, series, anime, manga and beyond. Explore stories worth
            experiencing, and build a universe of your own.
          </p>

          <a className="hero-link" href="#catalog">
            Explore the catalog
            <span aria-hidden="true">↘</span>
          </a>
        </div>

        <div className="hero-art" aria-hidden="true">
          <div className="hero-orbit orbit-one" />
          <div className="hero-orbit orbit-two" />
          <div className="hero-monogram">M/</div>
          <span className="hero-art-label">SIGNAL / 001</span>
        </div>
      </section>

      <section
        id="catalog"
        className="catalog-section"
        aria-labelledby="catalog-title"
      >
        <div className="section-heading">
          <div>
            <p className="eyebrow">EXPLORE THE ARCHIVE</p>
            <h2 id="catalog-title">
              THE INDEX<span>.</span>
            </h2>
          </div>

          <p className="section-description">Find something worth your time.</p>
        </div>

        <div className="discovery-toolbar">
          <label className="search-box">
            <span aria-hidden="true">⌕</span>

            <span className="sr-only">Search media titles</span>

            <input
              type="search"
              value={search}
              onChange={(event) => updateSearch(event.target.value)}
              placeholder="Search titles..."
            />
          </label>

          <div
            className="category-filters"
            role="group"
            aria-label="Media categories"
          >
            {categories.map((item) => (
              <button
                key={item.value}
                type="button"
                aria-pressed={category === item.value}
                onClick={() => updateCategory(item.value)}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className="catalog-meta">
          <span>
            {isLoading
              ? "LOADING ARCHIVE"
              : isSearching
                ? `${totalElements} MATCHES`
                : `${totalElements} TITLES`}
          </span>

          <span>
            {isSearching
              ? "SEARCH / UP TO 20 RESULTS"
              : "LIVE CATALOG / POSTER ART COMING SOON"}
          </span>
        </div>

        {isLoading ? (
          <div className="media-state" role="status">
            <span className="loading-indicator" aria-hidden="true" />
            <h3>SEARCHING THE ARCHIVE...</h3>
            <p>Finding your next story.</p>
          </div>
        ) : loadState === "error" ? (
          <div className="media-state" role="alert">
            <h3>CONNECTION LOST.</h3>
            <p>
              We couldn't load the media catalog.
              {errorMessage && ` ${errorMessage}`}
            </p>

            <button
              type="button"
              className="retry-button"
              onClick={() => setRetryKey((value) => value + 1)}
            >
              Try again ↗
            </button>
          </div>
        ) : media.length === 0 ? (
          <div className="empty-state">
            <h3>NO TITLES FOUND.</h3>
            <p>
              {isSearching
                ? "Try another search or category. Search results are currently limited to 20 matches."
                : "There are no catalog titles in this category yet."}
            </p>
          </div>
        ) : (
          <div className="media-grid">
            {media.map((item) => (
              <article className="media-card" key={item.id}>
                <div
                  className={`media-poster poster-${posterTone(item.id)}`}
                  aria-hidden="true"
                >
                  <span className="poster-code">
                    M/ {item.id.slice(0, 6).toUpperCase()}
                  </span>

                  <span className="poster-text">{item.title}</span>
                </div>

                <div className="media-card-info">
                  <h3>{item.title}</h3>
                  <span>{categoryLabels[item.mediaType]}</span>
                </div>
              </article>
            ))}
          </div>
        )}

        {!isLoading &&
          loadState === "ready" &&
          !isSearching &&
          totalPages > 1 && (
            <nav className="catalog-pagination" aria-label="Catalog pages">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                ← Previous
              </button>

              <span>
                PAGE {page + 1} / {totalPages}
              </span>

              <button
                type="button"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                Next →
              </button>
            </nav>
          )}
      </section>
    </>
  );
}
