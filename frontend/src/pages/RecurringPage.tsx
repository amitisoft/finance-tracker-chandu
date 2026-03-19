import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useCategoriesQuery, useRecurringQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";
import { formatCurrency, formatDate } from "../utils/format";

export function RecurringPage() {
  const { data, isLoading, isError } = useRecurringQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [frequencyFilter, setFrequencyFilter] = useState("ALL");
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
      setShowForm(false);
      pushToast("Recurring item created successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["recurring"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Recurring items failed to load." />;

  const visibleRecurring = data.filter((item) => {
    const matchesSearch = [item.title, item.accountName, item.categoryName].filter(Boolean).some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    const matchesType = typeFilter === "ALL" || item.type === typeFilter;
    const matchesFrequency = frequencyFilter === "ALL" || item.frequency === frequencyFilter;
    return (search ? matchesSearch : true) && matchesType && matchesFrequency;
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
          subtitle="Create recurring rules in a dedicated section"
          title="Add Recurring Item"
        >
          <form
            className="inline-form wide"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate();
            }}
          >
            <textarea
              className="inline-textarea"
              maxLength={120}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              placeholder="Title"
              rows={2}
              value={form.title}
            />
            <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
            </select>
            <input min="0.01" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="Amount" type="number" />
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
      ) : null}

      <Card
        action={
          <button className="secondary-button" onClick={() => setShowForm(true)} type="button">
            Add recurring
          </button>
        }
        subtitle="Subscriptions, salaries, and bills"
        title="Recurring List"
      >
        <div className="filter-bar filter-bar-recurring">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search recurring items..." value={search} />
          <select onChange={(event) => setTypeFilter(event.target.value)} value={typeFilter}>
            <option value="ALL">All types</option>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
            <option value="TRANSFER">Transfer</option>
          </select>
          <select onChange={(event) => setFrequencyFilter(event.target.value)} value={frequencyFilter}>
            <option value="ALL">All frequencies</option>
            <option value="DAILY">Daily</option>
            <option value="WEEKLY">Weekly</option>
            <option value="MONTHLY">Monthly</option>
            <option value="YEARLY">Yearly</option>
          </select>
        </div>

        <div className="table-list">
          {visibleRecurring.map((item) => (
            <div className="table-row" key={item.id}>
              <div className="cell-stack">
                <strong className="cell-copy">{item.title}</strong>
                <p>
                  {item.frequency} / {item.accountName}
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
