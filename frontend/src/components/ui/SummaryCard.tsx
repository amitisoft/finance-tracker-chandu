import clsx from "clsx";
import { formatCurrency } from "../../utils/format";

type SummaryCardProps = {
  label: string;
  value: number;
  tone?: "primary" | "success" | "danger" | "neutral";
};

export function SummaryCard({ label, value, tone = "primary" }: SummaryCardProps) {
  return (
    <article className={clsx("summary-card", `summary-card-${tone}`)}>
      <span>{label}</span>
      <strong>{formatCurrency(value)}</strong>
    </article>
  );
}
