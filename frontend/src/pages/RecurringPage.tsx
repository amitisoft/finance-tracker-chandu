import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useCategoriesQuery, useRecurringQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { formatCurrency, formatDate } from "../utils/format";

export function RecurringPage() {
  const { data, isLoading, isError } = useRecurringQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const [form, setForm] = useState({
    title: "",
    type: "EXPENSE",
    amount: "",
    categoryId: "",
    accountId: "",
    frequency: "MONTHLY",
    startDate: new Date().toISOString().slice(0, 10)
  });
  const mutation = useMutation({
    mutationFn: () =>
      financeApi.createRecurring({
        title: form.title,
        type: form.type,
        amount: Number(form.amount),
        categoryId: form.categoryId || undefined,
        accountId: form.accountId,
        frequency: form.frequency,
        startDate: form.startDate,
        autoCreateTransaction: true,
        paused: false
      }),
    onSuccess: async () => {
      setForm({
        title: "",
        type: "EXPENSE",
        amount: "",
        categoryId: "",
        accountId: "",
        frequency: "MONTHLY",
        startDate: new Date().toISOString().slice(0, 10)
      });
      await queryClient.invalidateQueries({ queryKey: ["recurring"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Recurring items failed to load." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Add subscriptions, salaries, and bills" title="Create Recurring Item">
        <form
          className="inline-form wide"
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
          }}
        >
          <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Title" />
          <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
          <input value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="Amount" type="number" />
          <select value={form.accountId} onChange={(event) => setForm({ ...form, accountId: event.target.value })}>
            <option value="">Select account</option>
            {(accounts ?? []).map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
            <option value="">Select category</option>
            {(categories ?? []).map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <select value={form.frequency} onChange={(event) => setForm({ ...form, frequency: event.target.value })}>
            <option value="DAILY">Daily</option>
            <option value="WEEKLY">Weekly</option>
            <option value="MONTHLY">Monthly</option>
            <option value="YEARLY">Yearly</option>
          </select>
          <input value={form.startDate} onChange={(event) => setForm({ ...form, startDate: event.target.value })} type="date" />
          <button className="primary-button" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Saving..." : "Add recurring"}
          </button>
        </form>
      </Card>

      <Card subtitle="Subscriptions, salaries, and bills" title="Recurring Transactions">
        <div className="table-list">
          {data.map((item) => (
            <div className="table-row" key={item.id}>
              <div>
                <strong>{item.title}</strong>
                <p>
                  {item.frequency} · {item.accountName}
                </p>
              </div>
              <div>
                <strong>{formatCurrency(item.amount)}</strong>
                <p>Next run {formatDate(item.nextRunDate)}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
