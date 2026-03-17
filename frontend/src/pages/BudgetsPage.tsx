import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useBudgetsQuery, useCategoriesQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { formatCurrency, getBudgetStatus } from "../utils/format";

const now = new Date();

export function BudgetsPage() {
  const { data, isLoading, isError } = useBudgetsQuery(now.getMonth() + 1, now.getFullYear());
  const { data: categories } = useCategoriesQuery();
  const [form, setForm] = useState({
    categoryId: "",
    amount: "",
    alertThresholdPercent: "80"
  });
  const mutation = useMutation({
    mutationFn: () =>
      financeApi.createBudget({
        categoryId: form.categoryId,
        month: now.getMonth() + 1,
        year: now.getFullYear(),
        amount: Number(form.amount),
        alertThresholdPercent: Number(form.alertThresholdPercent)
      }),
    onSuccess: async () => {
      setForm({ categoryId: "", amount: "", alertThresholdPercent: "80" });
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Budgets are unavailable." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Set monthly control limits by category" title="Create Budget">
        <form
          className="inline-form"
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
          }}
        >
          <select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
            <option value="">Select expense category</option>
            {(categories ?? [])
              .filter((category) => category.type === "EXPENSE")
              .map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
          </select>
          <input value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="Budget amount" type="number" />
          <input
            value={form.alertThresholdPercent}
            onChange={(event) => setForm({ ...form, alertThresholdPercent: event.target.value })}
            placeholder="Alert %"
            type="number"
          />
          <button className="primary-button" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Saving..." : "Set budget"}
          </button>
        </form>
      </Card>

      <Card subtitle="Current month guardrails" title="Budgets">
        <div className="goal-list">
          {data.map((budget) => (
            <div className="goal-item" key={budget.id}>
              <div className="goal-item-top">
                <strong>{budget.categoryName}</strong>
                <span>{budget.percentageUsed.toFixed(0)}%</span>
              </div>
              <div className="progress-track">
                <div className={`progress-bar ${getBudgetStatus(budget.percentageUsed)}`} style={{ width: `${Math.min(budget.percentageUsed, 100)}%` }} />
              </div>
              <p>
                {formatCurrency(budget.actualSpent)} spent of {formatCurrency(budget.amount)} budget
              </p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
