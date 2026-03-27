import { useMutation } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountMembersQuery, useAccountsQuery } from "../hooks/useFinanceQueries";
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
  const [selectedAccountId, setSelectedAccountId] = useState<string>("");
  const [form, setForm] = useState({
    name: "",
    type: "BANK_ACCOUNT",
    openingBalance: "0",
    institutionName: ""
  });
  const [inviteForm, setInviteForm] = useState({
    email: "",
    role: "EDITOR" as "EDITOR" | "VIEWER"
  });
  const { data: members } = useAccountMembersQuery(selectedAccountId || undefined);

  useEffect(() => {
    if (!selectedAccountId && data?.length) {
      setSelectedAccountId(data[0].id);
    }
  }, [data, selectedAccountId]);

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

  const inviteMutation = useMutation({
    mutationFn: () => financeApi.inviteAccountMember(selectedAccountId, inviteForm),
    onSuccess: async () => {
      setInviteForm({ email: "", role: "EDITOR" });
      pushToast("Member invited successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["account-members", selectedAccountId] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
    }
  });

  const roleMutation = useMutation({
    mutationFn: (payload: { userId: string; role: "EDITOR" | "VIEWER" }) =>
      financeApi.updateAccountMemberRole(selectedAccountId, payload.userId, { role: payload.role }),
    onSuccess: async () => {
      pushToast("Member role updated.", "success");
      await queryClient.invalidateQueries({ queryKey: ["account-members", selectedAccountId] });
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Accounts failed to load." />;

  const visibleAccounts = data.filter((account) => {
    const matchesSearch = [account.name, account.institutionName, account.type, account.ownerDisplayName]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    const matchesType = typeFilter === "ALL" || account.type === typeFilter;
    return (search ? matchesSearch : true) && matchesType;
  });

  const selectedAccount = data.find((account) => account.id === selectedAccountId);
  const isOwner = selectedAccount?.accessRole === "OWNER";

  return (
    <div className="stack-layout">
      <Card
        action={
          <button className="secondary-button" onClick={() => setShowForm((current) => !current)} type="button">
            {showForm ? "Close" : "Add account"}
          </button>
        }
        subtitle="Wallets, banks, and shared finance containers"
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
                  {account.type} / {account.institutionName || "Personal"} / Owner {account.ownerDisplayName}
                </p>
              </div>
              <div>
                <strong>{formatCurrency(account.currentBalance)}</strong>
                <p>
                  {account.accessRole} access / {account.memberCount} member{account.memberCount === 1 ? "" : "s"} / Updated{" "}
                  {formatDate(account.updatedAt)}
                </p>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card subtitle="Invite collaborators and manage shared access" title="Shared With">
        <div className="filter-bar filter-bar-goals">
          <select onChange={(event) => setSelectedAccountId(event.target.value)} value={selectedAccountId}>
            {data.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <input value={selectedAccount?.accessRole ?? ""} disabled />
        </div>

        {isOwner ? (
          <form
            className="inline-form sheet-form"
            onSubmit={(event) => {
              event.preventDefault();
              inviteMutation.mutate();
            }}
          >
            <input value={inviteForm.email} onChange={(event) => setInviteForm({ ...inviteForm, email: event.target.value })} placeholder="Invite email" />
            <select value={inviteForm.role} onChange={(event) => setInviteForm({ ...inviteForm, role: event.target.value as "EDITOR" | "VIEWER" })}>
              <option value="EDITOR">Editor</option>
              <option value="VIEWER">Viewer</option>
            </select>
            <button className="primary-button" disabled={inviteMutation.isPending || !selectedAccountId} type="submit">
              {inviteMutation.isPending ? "Inviting..." : "Invite member"}
            </button>
          </form>
        ) : (
          <div className="empty-state compact-empty">Only the account owner can invite members or change roles.</div>
        )}

        <div className="thin-list">
          {(members ?? []).map((member) => (
            <div className="thin-list-row" key={member.userId}>
              <div>
                <strong>{member.displayName}</strong>
                <span>
                  {member.email} / {member.owner ? "Owner" : member.role}
                </span>
              </div>
              {isOwner && !member.owner ? (
                <select
                  onChange={(event) => roleMutation.mutate({ userId: member.userId, role: event.target.value as "EDITOR" | "VIEWER" })}
                  value={member.role}
                >
                  <option value="EDITOR">Editor</option>
                  <option value="VIEWER">Viewer</option>
                </select>
              ) : (
                <strong>{member.role}</strong>
              )}
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
