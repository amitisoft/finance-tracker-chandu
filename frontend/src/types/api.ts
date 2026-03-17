export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  user: AuthUser;
};

export type Account = {
  id: string;
  name: string;
  type: string;
  openingBalance: number;
  currentBalance: number;
  institutionName?: string | null;
  updatedAt: string;
};

export type Category = {
  id: string;
  name: string;
  type: "INCOME" | "EXPENSE";
  color?: string | null;
  icon?: string | null;
  archived: boolean;
};

export type Transaction = {
  id: string;
  type: "INCOME" | "EXPENSE" | "TRANSFER";
  amount: number;
  date: string;
  accountId: string;
  accountName: string;
  destinationAccountId?: string | null;
  destinationAccountName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  merchant?: string | null;
  note?: string | null;
  paymentMethod?: string | null;
  tags: string[];
  createdAt: string;
};

export type Budget = {
  id: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  actualSpent: number;
  remaining: number;
  percentageUsed: number;
  month: number;
  year: number;
  alertThresholdPercent: number;
};

export type Goal = {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  progressPercentage: number;
  targetDate?: string | null;
  linkedAccountId?: string | null;
  icon?: string | null;
  color?: string | null;
  status: string;
};

export type RecurringItem = {
  id: string;
  title: string;
  type: "INCOME" | "EXPENSE" | "TRANSFER";
  amount: number;
  categoryId?: string | null;
  categoryName?: string | null;
  accountId: string;
  accountName: string;
  frequency: string;
  startDate: string;
  endDate?: string | null;
  nextRunDate: string;
  autoCreateTransaction: boolean;
  paused: boolean;
};

export type CategorySpendItem = {
  categoryName: string;
  total: number;
};

export type IncomeExpenseTrendItem = {
  month: string;
  income: number;
  expense: number;
};

export type DashboardResponse = {
  currentMonthIncome: number;
  currentMonthExpense: number;
  netBalance: number;
  spendingByCategory: CategorySpendItem[];
  incomeVsExpenseTrend: IncomeExpenseTrendItem[];
  recentTransactions: Transaction[];
  upcomingRecurringPayments: RecurringItem[];
  goals: Goal[];
};
