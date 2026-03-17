import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useGoalsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { formatCurrency, formatDate } from "../utils/format";

export function GoalsPage() {
  const { data, isLoading, isError } = useGoalsQuery();
  const { data: accounts } = useAccountsQuery();
  const [form, setForm] = useState({
    name: "",
    targetAmount: "",
    targetDate: "",
    linkedAccountId: ""
  });
  const mutation = useMutation({
    mutationFn: () =>
      financeApi.createGoal({
        name: form.name,
        targetAmount: Number(form.targetAmount),
        targetDate: form.targetDate || undefined,
        linkedAccountId: form.linkedAccountId || undefined,
        color: "#1d4ed8",
        icon: "target"
      }),
    onSuccess: async () => {
      setForm({ name: "", targetAmount: "", targetDate: "", linkedAccountId: "" });
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Goals failed to load." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Savings goals should be easy to set up" title="Create Goal">
        <form
          className="inline-form"
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
          }}
        >
          <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Goal name" />
          <input
            value={form.targetAmount}
            onChange={(event) => setForm({ ...form, targetAmount: event.target.value })}
            placeholder="Target amount"
            type="number"
          />
          <input value={form.targetDate} onChange={(event) => setForm({ ...form, targetDate: event.target.value })} type="date" />
          <select value={form.linkedAccountId} onChange={(event) => setForm({ ...form, linkedAccountId: event.target.value })}>
            <option value="">Optional linked account</option>
            {(accounts ?? []).map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <button className="primary-button" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Saving..." : "Add goal"}
          </button>
        </form>
      </Card>

      <Card subtitle="Milestones that stay visible" title="Goals">
        <div className="goal-list">
          {data.map((goal) => (
            <div className="goal-item" key={goal.id}>
              <div className="goal-item-top">
                <strong>{goal.name}</strong>
                <span>{goal.status}</span>
              </div>
              <div className="progress-track">
                <div className="progress-bar" style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }} />
              </div>
              <p>
                {formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)} · Due {formatDate(goal.targetDate)}
              </p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
