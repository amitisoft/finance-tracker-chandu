import { api } from "./api";
import type {
  Account,
  AuthResponse,
  Budget,
  Category,
  CategorySpendItem,
  DashboardResponse,
  Goal,
  IncomeExpenseTrendItem,
  RecurringItem,
  Transaction
} from "../types/api";

export const financeApi = {
  login: async (payload: { email: string; password: string }) =>
    (await api.post<AuthResponse>("/api/auth/login", payload)).data,
  register: async (payload: { email: string; password: string; displayName: string }) =>
    (await api.post<AuthResponse>("/api/auth/register", payload)).data,
  dashboard: async () => (await api.get<DashboardResponse>("/api/dashboard")).data,
  accounts: async () => (await api.get<Account[]>("/api/accounts")).data,
  createAccount: async (payload: { name: string; type: string; openingBalance: number; institutionName?: string }) =>
    (await api.post<Account>("/api/accounts", payload)).data,
  categories: async () => (await api.get<Category[]>("/api/categories")).data,
  transactions: async () => (await api.get<Transaction[]>("/api/transactions")).data,
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
  budgets: async (month: number, year: number) =>
    (await api.get<Budget[]>("/api/budgets", { params: { month, year } })).data,
  createBudget: async (payload: {
    categoryId: string;
    month: number;
    year: number;
    amount: number;
    alertThresholdPercent: number;
  }) => (await api.post<Budget>("/api/budgets", payload)).data,
  goals: async () => (await api.get<Goal[]>("/api/goals")).data,
  createGoal: async (payload: {
    name: string;
    targetAmount: number;
    targetDate?: string;
    linkedAccountId?: string;
    icon?: string;
    color?: string;
  }) => (await api.post<Goal>("/api/goals", payload)).data,
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
    (await api.get<IncomeExpenseTrendItem[]>("/api/reports/income-vs-expense", { params: { fromDate, toDate } })).data
};
