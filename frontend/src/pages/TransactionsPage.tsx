import { useMutation } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useCategoriesQuery, useTransactionsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";
import { formatCurrency, formatDate } from "../utils/format";

type TransactionFormState = {
  type: string;
  amount: string;
  date: string;
  accountId: string;
  categoryId: string;
  merchant: string;
  note: string;
};

function createDefaultForm(accounts?: { id: string }[]): TransactionFormState {
  return {
    type: "EXPENSE",
    amount: "",
    date: new Date().toISOString().slice(0, 10),
    accountId: accounts?.[0]?.id ?? "",
    categoryId: "",
    merchant: "",
    note: ""
  };
}

export function TransactionsPage() {
  const { data, isLoading, isError } = useTransactionsQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [searchParams, setSearchParams] = useSearchParams();
  const [showForm, setShowForm] = useState(searchParams.get("compose") === "1");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [selectedType, setSelectedType] = useState("ALL");
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [selectedAccount, setSelectedAccount] = useState("ALL");
  const [selectedMonth, setSelectedMonth] = useState("ALL");
  const [selectedYear, setSelectedYear] = useState("ALL");
  const [form, setForm] = useState<TransactionFormState>(createDefaultForm());
  const transactions = data ?? [];

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        type: form.type,
        amount: Number(form.amount),
        date: form.date,
        accountId: form.accountId,
        categoryId: form.categoryId || undefined,
        merchant: form.merchant || undefined,
        note: form.note || undefined
      };
      return editingId ? financeApi.updateTransaction(editingId, payload) : financeApi.createTransaction(payload);
    },
    onSuccess: async () => {
      setForm(createDefaultForm(accounts));
      setShowForm(false);
      setEditingId(null);
      setSearchParams({});
      pushToast(editingId ? "Transaction updated successfully." : "Transaction saved successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["transactions"] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (transactionId: string) => financeApi.deleteTransaction(transactionId),
    onSuccess: async () => {
      pushToast("Transaction deleted successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["transactions"] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    }
  });

  const filteredCategories = (categories ?? []).filter((category) => category.type === form.type);
  const availableYears = Array.from(new Set(transactions.map((transaction) => new Date(transaction.date).getFullYear().toString()))).sort().reverse();
  const visibleTransactions = transactions.filter((transaction) => {
    const transactionDate = new Date(transaction.date);
    const matchesSearch = [transaction.merchant, transaction.note, transaction.categoryName, transaction.accountName]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    const matchesType = selectedType === "ALL" || transaction.type === selectedType;
    const matchesCategory = selectedCategory === "ALL" || transaction.categoryName === selectedCategory;
    const matchesAccount = selectedAccount === "ALL" || transaction.accountId === selectedAccount;
    const matchesMonth = selectedMonth === "ALL" || String(transactionDate.getMonth() + 1).padStart(2, "0") === selectedMonth;
    const matchesYear = selectedYear === "ALL" || transactionDate.getFullYear().toString() === selectedYear;
    return (search ? matchesSearch : true) && matchesType && matchesCategory && matchesAccount && matchesMonth && matchesYear;
  });

  const categoryOptions = useMemo(
    () => Array.from(new Set(transactions.map((transaction) => transaction.categoryName).filter((value): value is string => Boolean(value)))),
    [transactions]
  );

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Transactions failed to load." />;

  const startCreate = () => {
    setEditingId(null);
    setForm(createDefaultForm(accounts));
    setShowForm((current) => {
      const next = !current;
      setSearchParams(next ? { compose: "1" } : {});
      return next;
    });
  };

  const startEdit = (transactionId: string) => {
    const transaction = transactions.find((item) => item.id === transactionId);
    if (!transaction) return;
    setEditingId(transaction.id);
    setForm({
      type: transaction.type,
      amount: String(transaction.amount),
      date: transaction.date,
      accountId: transaction.accountId,
      categoryId: transaction.categoryId ?? "",
      merchant: transaction.merchant ?? "",
      note: transaction.note ?? ""
    });
    setShowForm(true);
    setSearchParams({ compose: "1" });
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
                setSearchParams({});
              }}
              type="button"
            >
              Close
            </button>
          }
          subtitle="Use this section only when creating or editing"
          title={editingId ? "Edit Transaction" : "Add Transaction"}
        >
          <form
            className="inline-form wide"
            onSubmit={(event) => {
              event.preventDefault();
              saveMutation.mutate();
            }}
          >
            <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value, categoryId: "" })}>
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
            </select>
            <input value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="Amount" type="number" />
            <input value={form.date} onChange={(event) => setForm({ ...form, date: event.target.value })} type="date" />
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
              {filteredCategories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            <input value={form.merchant} onChange={(event) => setForm({ ...form, merchant: event.target.value })} placeholder="Merchant" />
            <input value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder="Note" />
            <button className="primary-button" disabled={saveMutation.isPending} type="submit">
              {saveMutation.isPending ? "Saving..." : editingId ? "Update transaction" : "Save transaction"}
            </button>
          </form>
        </Card>
      ) : null}

      <Card
        action={
          <button className="secondary-button" onClick={startCreate} type="button">
            Add transaction
          </button>
        }
        subtitle="Search, filter, edit, or remove entries"
        title="Transactions List"
      >
        <div className="filter-bar">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search merchant, note, category..." value={search} />
          <select onChange={(event) => setSelectedType(event.target.value)} value={selectedType}>
            <option value="ALL">All types</option>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
          <select onChange={(event) => setSelectedCategory(event.target.value)} value={selectedCategory}>
            <option value="ALL">All categories</option>
            {categoryOptions.map((categoryName) => (
              <option key={categoryName} value={categoryName}>
                {categoryName}
              </option>
            ))}
          </select>
          <select onChange={(event) => setSelectedAccount(event.target.value)} value={selectedAccount}>
            <option value="ALL">All accounts</option>
            {(accounts ?? []).map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <select onChange={(event) => setSelectedMonth(event.target.value)} value={selectedMonth}>
            <option value="ALL">All months</option>
            {Array.from({ length: 12 }, (_, index) => {
              const value = String(index + 1).padStart(2, "0");
              return (
                <option key={value} value={value}>
                  {value}
                </option>
              );
            })}
          </select>
          <select onChange={(event) => setSelectedYear(event.target.value)} value={selectedYear}>
            <option value="ALL">All years</option>
            {availableYears.map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
        </div>

        <div className="table-header table-header-actions">
          <span>Date</span>
          <span>Merchant</span>
          <span>Category</span>
          <span>Account</span>
          <span>Amount</span>
          <span>Actions</span>
        </div>
        {visibleTransactions.map((transaction) => (
          <div className="table-grid-row table-grid-row-actions" key={transaction.id}>
            <span>{formatDate(transaction.date)}</span>
            <div className="cell-stack">
              <strong>{transaction.merchant || "-"}</strong>
              <span>{transaction.createdByDisplayName ? `Added by ${transaction.createdByDisplayName}` : ""}</span>
            </div>
            <div className="cell-stack">
              <strong>{transaction.categoryName || transaction.type}</strong>
              <span>{transaction.alerts.length ? transaction.alerts.join(" / ") : transaction.type}</span>
            </div>
            <div className="cell-stack">
              <strong>{transaction.accountName}</strong>
              <span>{transaction.note || "No note"}</span>
            </div>
            <strong>{formatCurrency(transaction.amount)}</strong>
            <div className="row-actions">
              <button className="inline-link-button" onClick={() => startEdit(transaction.id)} type="button">
                Edit
              </button>
              <button className="inline-link-button danger" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate(transaction.id)} type="button">
                Delete
              </button>
            </div>
          </div>
        ))}
      </Card>
    </div>
  );
}
