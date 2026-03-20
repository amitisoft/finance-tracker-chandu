import { useQuery } from "@tanstack/react-query";
import { financeApi } from "../services/financeApi";
import { useAuthStore } from "../store/authStore";

export function useDashboardQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["dashboard"], queryFn: financeApi.dashboard, enabled: hydrated && Boolean(token) });
}

export function useAccountsQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["accounts"], queryFn: financeApi.accounts, enabled: hydrated && Boolean(token) });
}

export function useCategoriesQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["categories"], queryFn: financeApi.categories, enabled: hydrated && Boolean(token) });
}

export function useTransactionsQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["transactions"], queryFn: financeApi.transactions, enabled: hydrated && Boolean(token) });
}

export function useBudgetsQuery(month: number, year: number) {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({
    queryKey: ["budgets", month, year],
    queryFn: () => financeApi.budgets(month, year),
    enabled: hydrated && Boolean(token)
  });
}

export function useGoalsQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["goals"], queryFn: financeApi.goals, enabled: hydrated && Boolean(token) });
}

export function useRecurringQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["recurring"], queryFn: financeApi.recurring, enabled: hydrated && Boolean(token) });
}
