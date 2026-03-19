import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useGoalsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";
import { formatCurrency, formatDate } from "../utils/format";

type ContributionFormState = Record<string, { amount: string; sourceAccountId: string; open: boolean }>;

export function GoalsPage() {
  const { data, isLoading, isError } = useGoalsQuery();
  const { data: accounts } = useAccountsQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [form, setForm] = useState({
    name: "",
    targetAmount: "",
    targetDate: "",
    linkedAccountId: ""
  });
  const [contributionForm, setContributionForm] = useState<ContributionFormState>({});

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
      setShowForm(false);
      pushToast("Goal created successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  const contributeMutation = useMutation({
    mutationFn: ({ goalId, amount, sourceAccountId }: { goalId: string; amount: number; sourceAccountId?: string }) =>
      financeApi.contributeGoal(goalId, { amount, sourceAccountId }),
    onSuccess: async (_, variables) => {
      setContributionForm((current) => ({
        ...current,
        [variables.goalId]: {
          amount: "",
          sourceAccountId: current[variables.goalId]?.sourceAccountId ?? "",
          open: false
        }
      }));
      pushToast("Contribution added to goal.", "success");
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Goals failed to load." />;

  const visibleGoals = data.filter((goal) => {
    const matchesSearch = goal.name.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === "ALL" || goal.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="stack-layout">
      {showForm ? (
        <Card
          action={
            <button className="secondary-button" onClick={() => setShowForm(false)} type="button">
              Close
            </button>
          }
          subtitle="Create the goal in a dedicated section"
          title="Add Goal"
        >
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
      ) : null}

      <Card
        action={
          <button className="secondary-button" onClick={() => setShowForm(true)} type="button">
            Add goal
          </button>
        }
        subtitle="Search milestones and fund them directly"
        title="Goals List"
      >
        <div className="filter-bar filter-bar-goals">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search goals..." value={search} />
          <select onChange={(event) => setStatusFilter(event.target.value)} value={statusFilter}>
            <option value="ALL">All statuses</option>
            {Array.from(new Set(data.map((goal) => goal.status))).map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-list">
          {visibleGoals.map((goal) => {
            const contributionState = contributionForm[goal.id] ?? {
              amount: "",
              sourceAccountId: goal.linkedAccountId ?? "",
              open: false
            };

            return (
              <div className="goal-item" key={goal.id}>
                <div className="goal-item-top goal-item-top-actions">
                  <div>
                    <strong>{goal.name}</strong>
                    <p className="goal-status">{goal.status}</p>
                  </div>
                  <div className="row-actions">
                    <span>{goal.progressPercentage.toFixed(0)}%</span>
                    <button
                      className="inline-link-button"
                      onClick={() =>
                        setContributionForm((current) => ({
                          ...current,
                          [goal.id]: {
                            amount: current[goal.id]?.amount ?? "",
                            sourceAccountId: current[goal.id]?.sourceAccountId ?? goal.linkedAccountId ?? "",
                            open: !current[goal.id]?.open
                          }
                        }))
                      }
                      type="button"
                    >
                      {contributionState.open ? "Close" : "Contribute"}
                    </button>
                  </div>
                </div>
                <div className="progress-track">
                  <div className="progress-bar" style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }} />
                </div>
                <p>
                  {formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)} / Due {formatDate(goal.targetDate)}
                </p>
                {contributionState.open ? (
                  <form
                    className="goal-contribution-form"
                    onSubmit={(event) => {
                      event.preventDefault();
                      contributeMutation.mutate({
                        goalId: goal.id,
                        amount: Number(contributionState.amount),
                        sourceAccountId: contributionState.sourceAccountId || undefined
                      });
                    }}
                  >
                    <input
                      value={contributionState.amount}
                      onChange={(event) =>
                        setContributionForm((current) => ({
                          ...current,
                          [goal.id]: {
                            amount: event.target.value,
                            sourceAccountId: current[goal.id]?.sourceAccountId ?? goal.linkedAccountId ?? "",
                            open: true
                          }
                        }))
                      }
                      min="0.01"
                      placeholder="Contribution amount"
                      step="0.01"
                      type="number"
                    />
                    <select
                      value={contributionState.sourceAccountId}
                      onChange={(event) =>
                        setContributionForm((current) => ({
                          ...current,
                          [goal.id]: {
                            amount: current[goal.id]?.amount ?? "",
                            sourceAccountId: event.target.value,
                            open: true
                          }
                        }))
                      }
                    >
                      <option value="">Choose funding account</option>
                      {(accounts ?? []).map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.name}
                        </option>
                      ))}
                    </select>
                    <button className="primary-button" disabled={contributeMutation.isPending} type="submit">
                      {contributeMutation.isPending && contributeMutation.variables?.goalId === goal.id ? "Adding..." : "Save contribution"}
                    </button>
                  </form>
                ) : null}
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
