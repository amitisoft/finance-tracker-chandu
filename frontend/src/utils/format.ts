export const currencyFormatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2
});

export function formatCurrency(value: number) {
  return currencyFormatter.format(value ?? 0);
}

export function formatDate(value?: string | null) {
  if (!value) return "Not set";
  return new Date(value).toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

export function getBudgetStatus(percentage: number) {
  if (percentage >= 120) return "critical";
  if (percentage >= 100) return "danger";
  if (percentage >= 80) return "warning";
  return "healthy";
}
