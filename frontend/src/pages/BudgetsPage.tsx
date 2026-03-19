import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useBudgetsQuery, useCategoriesQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";
import { formatCurrency, getBudgetStatus } from "../utils/format";

const today = new Date();

export function BudgetsPage() {
  const [selectedMonth, setSelectedMonth] = useState(today.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(today.getFullYear());
  const { data, isLoading, isError } = useBudgetsQuery(selectedMonth, selectedYear);
  const { data: categories } = useCategoriesQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [form, setForm] = useState({
    categoryId: "",
    amount: "",
    alertThresholdPercent: "80"
  });

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        categoryId: form.categoryId,
        month: selectedMonth,
        year: selectedYear,
        amount: Number(form.amount),
        alertThresholdPercent: Number(form.alertThresholdPercent)
      };
      return editingId ? financeApi.updateBudget(editingId, payload) : financeApi.createBudget(payload);
    },
    onSuccess: async () => {
      setForm({ categoryId: "", amount: "", alertThresholdPercent: "80" });
      setShowForm(false);
      setEditingId(null);
      pushToast(editingId ? "Budget updated successfully." : "Budget saved successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (budgetId: string) => financeApi.deleteBudget(budgetId),
    onSuccess: async () => {
      pushToast("Budget deleted successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Budgets are unavailable." />;

  const visibleBudgets = data.filter((budget) => {
    const matchesSearch = budget.categoryName.toLowerCase().includes(search.toLowerCase());
    const matchesCategory = categoryFilter === "ALL" || budget.categoryName === categoryFilter;
    return matchesSearch && matchesCategory;
  });

  const toggleForm = () => {
    setEditingId(null);
    setForm({ categoryId: "", amount: "", alertThresholdPercent: "80" });
    setShowForm((current) => !current);
  };

  const startEdit = (budgetId: string) => {
    const budget = data.find((item) => item.id === budgetId);
    if (!budget) return;
    setEditingId(budget.id);
    setForm({
      categoryId: budget.categoryId,
      amount: String(budget.amount),
      alertThresholdPercent: String(budget.alertThresholdPercent)
    });
    setShowForm(true);
  };

  return (
    <div className="stack-layout">
      {showForm ? (
        <Card
          action={
            <button
              className="secondary-button"
              onClick={() => {
                setShowForm(false);
                setEditingId(null);
                setForm({ categoryId: "", amount: "", alertThresholdPercent: "80" });
              }}
              type="button"
            >
              Close
            </button>
          }
          subtitle="Keep the editor separate from the monthly list"
          title={editingId ? "Edit Budget" : "Add Budget"}
        >
          <form
            className="inline-form"
            onSubmit={(event) => {
              event.preventDefault();
              saveMutation.mutate();
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
            <button className="primary-button" disabled={saveMutation.isPending} type="submit">
              {saveMutation.isPending ? "Saving..." : editingId ? "Update budget" : "Set budget"}
            </button>
          </form>
        </Card>
      ) : null}

      <Card
        action={
          <button className="secondary-button" onClick={toggleForm} type="button">
            Add budget
          </button>
        }
        subtitle="Filter the current month and edit budgets in place"
        title="Budgets List"
      >
        <div className="filter-bar filter-bar-budgets">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search category..." value={search} />
          <select onChange={(event) => setCategoryFilter(event.target.value)} value={categoryFilter}>
            <option value="ALL">All categories</option>
            {Array.from(new Set(data.map((budget) => budget.categoryName))).map((categoryName) => (
              <option key={categoryName} value={categoryName}>
                {categoryName}
              </option>
            ))}
          </select>
          <select onChange={(event) => setSelectedMonth(Number(event.target.value))} value={selectedMonth}>
            {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
              <option key={month} value={month}>
                {String(month).padStart(2, "0")}
              </option>
            ))}
          </select>
          <select onChange={(event) => setSelectedYear(Number(event.target.value))} value={selectedYear}>
            {[today.getFullYear() - 1, today.getFullYear(), today.getFullYear() + 1].map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-list">
          {visibleBudgets.map((budget) => (
            <div className="goal-item" key={budget.id}>
              <div className="goal-item-top goal-item-top-actions">
                <strong>{budget.categoryName}</strong>
                <div className="row-actions">
                  <span>{budget.percentageUsed.toFixed(0)}%</span>
                  <button className="inline-link-button" onClick={() => startEdit(budget.id)} type="button">
                    Edit
                  </button>
                  <button className="inline-link-button danger" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate(budget.id)} type="button">
                    Delete
                  </button>
                </div>
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
