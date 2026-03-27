import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useRulesQuery } from "../hooks/useFinanceQueries";
import { financeApi } from "../services/financeApi";
import { queryClient } from "../services/queryClient";
import { useToastStore } from "../store/toastStore";

type RuleFormState = {
  conditionField: "MERCHANT" | "CATEGORY" | "TYPE" | "AMOUNT";
  conditionOperator: "EQUALS" | "CONTAINS" | "GREATER_THAN" | "LESS_THAN";
  conditionValue: string;
  actionType: "SET_CATEGORY" | "ADD_TAG" | "TRIGGER_ALERT";
  actionValue: string;
  active: "true" | "false";
  priority: string;
};

const defaultForm: RuleFormState = {
  conditionField: "MERCHANT",
  conditionOperator: "EQUALS",
  conditionValue: "",
  actionType: "SET_CATEGORY",
  actionValue: "",
  active: "true",
  priority: "100"
};

export function RulesPage() {
  const { data, isLoading, isError } = useRulesQuery();
  const pushToast = useToastStore((state) => state.pushToast);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<RuleFormState>(defaultForm);

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        conditionField: form.conditionField,
        conditionOperator: form.conditionOperator,
        conditionValue: form.conditionValue,
        actionType: form.actionType,
        actionValue: form.actionValue,
        active: form.active === "true",
        priority: Number(form.priority)
      };
      return editingId ? financeApi.updateRule(editingId, payload) : financeApi.createRule(payload);
    },
    onSuccess: async () => {
      setEditingId(null);
      setForm(defaultForm);
      pushToast(editingId ? "Rule updated successfully." : "Rule created successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["rules"] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: string) => financeApi.deleteRule(ruleId),
    onSuccess: async () => {
      pushToast("Rule deleted successfully.", "success");
      await queryClient.invalidateQueries({ queryKey: ["rules"] });
    }
  });

  const toggleMutation = useMutation({
    mutationFn: (payload: {
      id: string;
      conditionField: string;
      conditionOperator: string;
      conditionValue: string;
      actionType: string;
      actionValue: string;
      active: boolean;
      priority: number;
    }) => financeApi.updateRule(payload.id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["rules"] });
    }
  });

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Rules failed to load." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Create form-based automation for categorization, tags, and alerts" title="Rules Engine">
        <form
          className="inline-form wide"
          onSubmit={(event) => {
            event.preventDefault();
            saveMutation.mutate();
          }}
        >
          <select value={form.conditionField} onChange={(event) => setForm({ ...form, conditionField: event.target.value as RuleFormState["conditionField"] })}>
            <option value="MERCHANT">Merchant</option>
            <option value="CATEGORY">Category</option>
            <option value="TYPE">Type</option>
            <option value="AMOUNT">Amount</option>
          </select>
          <select
            value={form.conditionOperator}
            onChange={(event) => setForm({ ...form, conditionOperator: event.target.value as RuleFormState["conditionOperator"] })}
          >
            <option value="EQUALS">Equals</option>
            <option value="CONTAINS">Contains</option>
            <option value="GREATER_THAN">Greater than</option>
            <option value="LESS_THAN">Less than</option>
          </select>
          <input value={form.conditionValue} onChange={(event) => setForm({ ...form, conditionValue: event.target.value })} placeholder="Condition value" />
          <select value={form.actionType} onChange={(event) => setForm({ ...form, actionType: event.target.value as RuleFormState["actionType"] })}>
            <option value="SET_CATEGORY">Set category</option>
            <option value="ADD_TAG">Add tag</option>
            <option value="TRIGGER_ALERT">Trigger alert</option>
          </select>
          <input value={form.actionValue} onChange={(event) => setForm({ ...form, actionValue: event.target.value })} placeholder="Action value" />
          <select value={form.active} onChange={(event) => setForm({ ...form, active: event.target.value as RuleFormState["active"] })}>
            <option value="true">Enabled</option>
            <option value="false">Disabled</option>
          </select>
          <input value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value })} placeholder="Priority" type="number" />
          <button className="primary-button" disabled={saveMutation.isPending} type="submit">
            {saveMutation.isPending ? "Saving..." : editingId ? "Update rule" : "Create rule"}
          </button>
        </form>
      </Card>

      <Card subtitle="Rules run in priority order during transaction creation" title="Configured Rules">
        <div className="thin-list">
          {data.map((rule) => (
            <div className="thin-list-row" key={rule.id}>
              <div>
                <strong>
                  If {rule.conditionField} {rule.conditionOperator} {rule.conditionValue}
                </strong>
                <span>
                  Then {rule.actionType} {rule.actionValue} / Priority {rule.priority}
                </span>
              </div>
              <div className="row-actions">
                <button
                  className="inline-link-button"
                  onClick={() => {
                    setForm({
                      conditionField: rule.conditionField,
                      conditionOperator: rule.conditionOperator,
                      conditionValue: rule.conditionValue,
                      actionType: rule.actionType,
                      actionValue: rule.actionValue,
                      active: String(rule.active) as RuleFormState["active"],
                      priority: String(rule.priority)
                    });
                    setEditingId(rule.id);
                  }}
                  type="button"
                >
                  Edit
                </button>
                <button
                  className="inline-link-button"
                  onClick={() =>
                    toggleMutation.mutate({
                      id: rule.id,
                      conditionField: rule.conditionField,
                      conditionOperator: rule.conditionOperator,
                      conditionValue: rule.conditionValue,
                      actionType: rule.actionType,
                      actionValue: rule.actionValue,
                      active: !rule.active,
                      priority: rule.priority
                    })
                  }
                  type="button"
                >
                  {rule.active ? "Disable" : "Enable"}
                </button>
                <button className="inline-link-button danger" disabled={deleteMutation.isPending} onClick={() => deleteMutation.mutate(rule.id)} type="button">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
