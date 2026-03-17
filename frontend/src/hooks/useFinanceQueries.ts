import { useQuery } from "@tanstack/react-query";
import { financeApi } from "../services/financeApi";

export function useDashboardQuery() {
  return useQuery({ queryKey: ["dashboard"], queryFn: financeApi.dashboard });
}

export function useAccountsQuery() {
  return useQuery({ queryKey: ["accounts"], queryFn: financeApi.accounts });
}

export function useCategoriesQuery() {
  return useQuery({ queryKey: ["categories"], queryFn: financeApi.categories });
}

export function useTransactionsQuery() {
  return useQuery({ queryKey: ["transactions"], queryFn: financeApi.transactions });
}

export function useBudgetsQuery(month: number, year: number) {
  return useQuery({ queryKey: ["budgets", month, year], queryFn: () => financeApi.budgets(month, year) });
}

export function useGoalsQuery() {
  return useQuery({ queryKey: ["goals"], queryFn: financeApi.goals });
}

export function useRecurringQuery() {
  return useQuery({ queryKey: ["recurring"], queryFn: financeApi.recurring });
}
