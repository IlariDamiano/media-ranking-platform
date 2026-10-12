import { Link } from "react-router";

type PlaceholderPageProps = {
  title: string;
  description: string;
};

export default function PlaceholderPage({
  title,
  description,
}: PlaceholderPageProps) {
  return (
    <section className="placeholder-page">
      <p className="eyebrow">UNDER CONSTRUCTION / 001</p>

      <h1>{title}</h1>

      <p>{description}</p>

      <Link to="/" className="hero-link">
        Back to Discovery <span aria-hidden="true">↗</span>
      </Link>
    </section>
  );
}
