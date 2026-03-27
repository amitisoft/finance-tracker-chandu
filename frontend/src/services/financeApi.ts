import { api } from "./api";
import type {
  Account,
  AccountMember,
  AssistantResponse,
  AuthResponse,
  Budget,
  CashFlowForecast,
  Category,
  CategorySpendItem,
  DailyForecastPoint,
  DashboardResponse,
  Goal,
  HealthScore,
  IncomeExpenseTrendItem,
  InsightItem,
  MonthlyForecast,
  NetWorthPoint,
  RecurringItem,
  Rule,
  Transaction
  ,
  TrendsReport
} from "../types/api";

export const financeApi = {
  login: async (payload: { email: string; password: string }) =>
    (await api.post<AuthResponse>("/api/auth/login", payload)).data,
  assistantMessage: async (payload: { message: string }) =>
    (await api.post<AssistantResponse>("/api/assistant/message", payload)).data,
  register: async (payload: { email: string; password: string; displayName: string }) =>
    (await api.post<AuthResponse>("/api/auth/register", payload)).data,
  dashboard: async () => (await api.get<DashboardResponse>("/api/dashboard")).data,
  accounts: async () => (await api.get<Account[]>("/api/accounts")).data,
  accountMembers: async (accountId: string) => (await api.get<AccountMember[]>(`/api/accounts/${accountId}/members`)).data,
  createAccount: async (payload: { name: string; type: string; openingBalance: number; institutionName?: string }) =>
    (await api.post<Account>("/api/accounts", payload)).data,
  inviteAccountMember: async (accountId: string, payload: { email: string; role: "EDITOR" | "VIEWER" }) =>
    (await api.post<AccountMember>(`/api/accounts/${accountId}/invite`, payload)).data,
  updateAccountMemberRole: async (accountId: string, userId: string, payload: { role: "EDITOR" | "VIEWER" }) =>
    (await api.put<AccountMember>(`/api/accounts/${accountId}/members/${userId}`, payload)).data,
  categories: async () => (await api.get<Category[]>("/api/categories")).data,
  transactions: async (params?: {
    fromDate?: string;
    toDate?: string;
    accountId?: string;
    categoryId?: string;
    type?: string;
    search?: string;
  }) => (await api.get<Transaction[]>("/api/transactions", { params })).data,
  createTransaction: async (payload: {
    type: string;
    amount: number;
    date: string;
    accountId: string;
    destinationAccountId?: string;
    categoryId?: string;
    merchant?: string;
    note?: string;
    paymentMethod?: string;
    tags?: string[];
  }) => (await api.post<Transaction>("/api/transactions", payload)).data,
  updateTransaction: async (
    transactionId: string,
    payload: {
      type: string;
      amount: number;
      date: string;
      accountId: string;
      destinationAccountId?: string;
      categoryId?: string;
      merchant?: string;
      note?: string;
      paymentMethod?: string;
      tags?: string[];
    }
  ) => (await api.put<Transaction>(`/api/transactions/${transactionId}`, payload)).data,
  deleteTransaction: async (transactionId: string) => (await api.delete(`/api/transactions/${transactionId}`)).data,
  budgets: async (month: number, year: number) =>
    (await api.get<Budget[]>("/api/budgets", { params: { month, year } })).data,
  createBudget: async (payload: {
    categoryId: string;
    month: number;
    year: number;
    amount: number;
    alertThresholdPercent: number;
  }) => (await api.post<Budget>("/api/budgets", payload)).data,
  updateBudget: async (
    budgetId: string,
    payload: {
      categoryId: string;
      month: number;
      year: number;
      amount: number;
      alertThresholdPercent: number;
    }
  ) => (await api.put<Budget>(`/api/budgets/${budgetId}`, payload)).data,
  deleteBudget: async (budgetId: string) => (await api.delete(`/api/budgets/${budgetId}`)).data,
  goals: async () => (await api.get<Goal[]>("/api/goals")).data,
  createGoal: async (payload: {
    name: string;
    targetAmount: number;
    targetDate?: string;
    linkedAccountId?: string;
    icon?: string;
    color?: string;
  }) => (await api.post<Goal>("/api/goals", payload)).data,
  contributeGoal: async (goalId: string, payload: { amount: number; sourceAccountId?: string }) =>
    (await api.post<Goal>(`/api/goals/${goalId}/contribute`, payload)).data,
  recurring: async () => (await api.get<RecurringItem[]>("/api/recurring")).data,
  createRecurring: async (payload: {
    title: string;
    type: string;
    amount: number;
    categoryId?: string;
    accountId: string;
    frequency: string;
    startDate: string;
    endDate?: string;
    autoCreateTransaction: boolean;
    paused: boolean;
  }) => (await api.post<RecurringItem>("/api/recurring", payload)).data,
  categorySpend: async (fromDate: string, toDate: string) =>
    (await api.get<CategorySpendItem[]>("/api/reports/category-spend", { params: { fromDate, toDate } })).data,
  incomeExpense: async (fromDate: string, toDate: string) =>
    (await api.get<IncomeExpenseTrendItem[]>("/api/reports/income-vs-expense", { params: { fromDate, toDate } })).data,
  trends: async (fromDate: string, toDate: string) =>
    (await api.get<TrendsReport>("/api/reports/trends", { params: { fromDate, toDate } })).data,
  netWorth: async (fromDate: string, toDate: string) =>
    (await api.get<NetWorthPoint[]>("/api/reports/net-worth", { params: { fromDate, toDate } })).data,
  insights: async () => (await api.get<InsightItem[]>("/api/insights")).data,
  healthScore: async () => (await api.get<HealthScore>("/api/insights/health-score")).data,
  rules: async () => (await api.get<Rule[]>("/api/rules")).data,
  createRule: async (payload: {
    conditionField: string;
    conditionOperator: string;
    conditionValue: string;
    actionType: string;
    actionValue: string;
    active?: boolean;
    priority?: number;
  }) => (await api.post<Rule>("/api/rules", payload)).data,
  updateRule: async (
    ruleId: string,
    payload: {
      conditionField: string;
      conditionOperator: string;
      conditionValue: string;
      actionType: string;
      actionValue: string;
      active?: boolean;
      priority?: number;
    }
  ) => (await api.put<Rule>(`/api/rules/${ruleId}`, payload)).data,
  deleteRule: async (ruleId: string) => (await api.delete(`/api/rules/${ruleId}`)).data,
  monthlyForecast: async () => (await api.get<MonthlyForecast>("/api/forecast/month")).data,
  dailyForecast: async () => (await api.get<DailyForecastPoint[]>("/api/forecast/daily")).data,
  cashFlowForecast: async (months: number) =>
    (await api.get<CashFlowForecast>("/api/forecast/cash-flow", { params: { months } })).data
};
