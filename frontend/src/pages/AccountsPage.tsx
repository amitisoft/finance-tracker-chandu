import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";
import { formatCurrency, formatDate } from "../utils/format";

export function AccountsPage() {
  const { data, isLoading, isError } = useAccountsQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [form, setForm] = useState({
    name: "",
    type: "BANK_ACCOUNT",
    openingBalance: "0",
    institutionName: ""
  });
  const mutation = useMutation({
    mutationFn: () =>
      financeApi.createAccount({
        name: form.name,
        type: form.type,
        openingBalance: Number(form.openingBalance),
        institutionName: form.institutionName
      }),
    onSuccess: async () => {
      setForm({ name: "", type: "BANK_ACCOUNT", openingBalance: "0", institutionName: "" });
      setShowForm(false);
      pushToast("Account created successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Accounts failed to load." />;

  const visibleAccounts = data.filter((account) => {
    const matchesSearch = [account.name, account.institutionName, account.type]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    const matchesType = typeFilter === "ALL" || account.type === typeFilter;
    return (search ? matchesSearch : true) && matchesType;
  });

  return (
    <div className="stack-layout">
      <Card
        action={
          <button className="secondary-button" onClick={() => setShowForm((current) => !current)} type="button">
            {showForm ? "Close" : "Add account"}
          </button>
        }
        subtitle="Wallets, banks, and cards"
        title="Accounts"
      >
        <div className="filter-bar">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search account or institution..." value={search} />
          <select onChange={(event) => setTypeFilter(event.target.value)} value={typeFilter}>
            <option value="ALL">All account types</option>
            <option value="BANK_ACCOUNT">Bank account</option>
            <option value="CREDIT_CARD">Credit card</option>
            <option value="CASH_WALLET">Cash wallet</option>
            <option value="SAVINGS_ACCOUNT">Savings account</option>
          </select>
        </div>

        {showForm ? (
          <form
            className="inline-form sheet-form"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate();
            }}
          >
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Account name" />
            <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
              <option value="BANK_ACCOUNT">Bank account</option>
              <option value="CREDIT_CARD">Credit card</option>
              <option value="CASH_WALLET">Cash wallet</option>
              <option value="SAVINGS_ACCOUNT">Savings account</option>
            </select>
            <input
              value={form.openingBalance}
              onChange={(event) => setForm({ ...form, openingBalance: event.target.value })}
              placeholder="Opening balance"
              type="number"
            />
            <input
              value={form.institutionName}
              onChange={(event) => setForm({ ...form, institutionName: event.target.value })}
              placeholder="Institution"
            />
            <button className="primary-button" disabled={mutation.isPending} type="submit">
              {mutation.isPending ? "Saving..." : "Add account"}
            </button>
          </form>
        ) : null}

        <div className="table-list">
          {visibleAccounts.map((account) => (
            <div className="table-row" key={account.id}>
              <div>
                <strong>{account.name}</strong>
                <p>
                  {account.type} / {account.institutionName || "Personal"}
                </p>
              </div>
              <div>
                <strong>{formatCurrency(account.currentBalance)}</strong>
                <p>Updated {formatDate(account.updatedAt)}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
