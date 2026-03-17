import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { formatCurrency, formatDate } from "../utils/format";

export function AccountsPage() {
  const { data, isLoading, isError } = useAccountsQuery();
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
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Accounts failed to load." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Create real wallets, cards, and bank accounts" title="Add Account">
        <form
          className="inline-form"
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
      </Card>

      <Card subtitle="Wallets, banks, and cards" title="Accounts">
        <div className="table-list">
          {data.map((account) => (
            <div className="table-row" key={account.id}>
              <div>
                <strong>{account.name}</strong>
                <p>
                  {account.type} · {account.institutionName || "Personal"}
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
