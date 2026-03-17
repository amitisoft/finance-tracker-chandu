import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useCategoriesQuery, useTransactionsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { formatCurrency, formatDate } from "../utils/format";

export function TransactionsPage() {
  const { data, isLoading, isError } = useTransactionsQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const [form, setForm] = useState({
    type: "EXPENSE",
    amount: "",
    date: new Date().toISOString().slice(0, 10),
    accountId: "",
    categoryId: "",
    merchant: "",
    note: ""
  });
  const mutation = useMutation({
    mutationFn: () =>
      financeApi.createTransaction({
        type: form.type,
        amount: Number(form.amount),
        date: form.date,
        accountId: form.accountId,
        categoryId: form.type === "TRANSFER" ? undefined : form.categoryId,
        merchant: form.merchant,
        note: form.note
      }),
    onSuccess: async () => {
      setForm({
        type: "EXPENSE",
        amount: "",
        date: new Date().toISOString().slice(0, 10),
        accountId: accounts?.[0]?.id ?? "",
        categoryId: "",
        merchant: "",
        note: ""
      });
      await queryClient.invalidateQueries({ queryKey: ["transactions"] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Transactions failed to load." />;

  const filteredCategories = (categories ?? []).filter((category) =>
    form.type === "INCOME" ? category.type === "INCOME" : category.type === "EXPENSE"
  );

  return (
    <div className="stack-layout">
      <Card subtitle="Fast data entry matters in real usage" title="Add Transaction">
        <form
          className="inline-form wide"
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
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
          <button className="primary-button" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Saving..." : "Save transaction"}
          </button>
        </form>
      </Card>

      <Card subtitle="Auditable and searchable" title="Transactions">
        <div className="table-header">
          <span>Date</span>
          <span>Merchant</span>
          <span>Category</span>
          <span>Account</span>
          <span>Amount</span>
        </div>
        {data.map((transaction) => (
          <div className="table-grid-row" key={transaction.id}>
            <span>{formatDate(transaction.date)}</span>
            <span>{transaction.merchant || "-"}</span>
            <span>{transaction.categoryName || transaction.type}</span>
            <span>{transaction.accountName}</span>
            <strong>{formatCurrency(transaction.amount)}</strong>
          </div>
        ))}
      </Card>
    </div>
  );
}
