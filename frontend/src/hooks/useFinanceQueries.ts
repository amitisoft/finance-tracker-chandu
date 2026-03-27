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
  return useQuery({ queryKey: ["transactions"], queryFn: () => financeApi.transactions(), enabled: hydrated && Boolean(token) });
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

export function useCashFlowForecastQuery(months: number) {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({
    queryKey: ["cash-flow-forecast", months],
    queryFn: () => financeApi.cashFlowForecast(months),
    enabled: hydrated && Boolean(token)
  });
}

export function useMonthlyForecastQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["monthly-forecast"], queryFn: financeApi.monthlyForecast, enabled: hydrated && Boolean(token) });
}

export function useDailyForecastQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["daily-forecast"], queryFn: financeApi.dailyForecast, enabled: hydrated && Boolean(token) });
}

export function useHealthScoreQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["health-score"], queryFn: financeApi.healthScore, enabled: hydrated && Boolean(token) });
}

export function useInsightsQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["insights"], queryFn: financeApi.insights, enabled: hydrated && Boolean(token) });
}

export function useRulesQuery() {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({ queryKey: ["rules"], queryFn: financeApi.rules, enabled: hydrated && Boolean(token) });
}

export function useAccountMembersQuery(accountId?: string) {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({
    queryKey: ["account-members", accountId],
    queryFn: () => financeApi.accountMembers(accountId as string),
    enabled: hydrated && Boolean(token) && Boolean(accountId)
  });
}

export function useTrendsReportQuery(fromDate: string, toDate: string) {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({
    queryKey: ["trends-report", fromDate, toDate],
    queryFn: () => financeApi.trends(fromDate, toDate),
    enabled: hydrated && Boolean(token)
  });
}

export function useNetWorthQuery(fromDate: string, toDate: string) {
  const token = useAuthStore((state) => state.accessToken);
  const hydrated = useAuthStore((state) => state.hydrated);
  return useQuery({
    queryKey: ["net-worth", fromDate, toDate],
    queryFn: () => financeApi.netWorth(fromDate, toDate),
    enabled: hydrated && Boolean(token)
  });
}
