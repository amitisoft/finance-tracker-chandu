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
  accessRole: "OWNER" | "EDITOR" | "VIEWER";
  ownerDisplayName: string;
  memberCount: number;
};

export type AccountMember = {
  userId: string;
  email: string;
  displayName: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
  owner: boolean;
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
  createdByUserId: string;
  createdByDisplayName: string;
  alerts: string[];
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

export type CashFlowForecastPeriod = {
  label: string;
  periodStart: string;
  periodEnd: string;
  openingBalance: number;
  projectedIncome: number;
  projectedExpense: number;
  netCashFlow: number;
  closingBalance: number;
  recurringItems: number;
};

export type CashFlowForecast = {
  forecastMonths: number;
  currentBalance: number;
  projectedClosingBalance: number;
  projectedNetChange: number;
  averageMonthlyIncome: number;
  averageMonthlyExpense: number;
  lowestProjectedBalance: number;
  healthSignal: "STABLE" | "WATCH" | "CRITICAL";
  periods: CashFlowForecastPeriod[];
};

export type MonthlyForecast = {
  forecastedBalance: number;
  safeToSpend: number;
  warnings: string[];
  periods: CashFlowForecastPeriod[];
};

export type DailyForecastPoint = {
  date: string;
  projectedBalance: number;
  knownExpenses: number;
};

export type HealthScoreBreakdownItem = {
  factor: string;
  score: number;
  summary: string;
};

export type HealthScore = {
  score: number;
  band: string;
  breakdown: HealthScoreBreakdownItem[];
  suggestions: string[];
};

export type InsightItem = {
  title: string;
  message: string;
  tone: "success" | "warning" | "info";
};

export type Rule = {
  id: string;
  conditionField: "MERCHANT" | "CATEGORY" | "TYPE" | "AMOUNT";
  conditionOperator: "EQUALS" | "CONTAINS" | "GREATER_THAN" | "LESS_THAN";
  conditionValue: string;
  actionType: "SET_CATEGORY" | "ADD_TAG" | "TRIGGER_ALERT";
  actionValue: string;
  active: boolean;
  priority: number;
};

export type SavingsRateTrendPoint = {
  month: string;
  savingsRate: number;
};

export type CategoryTrendPoint = {
  month: string;
  categoryName: string;
  total: number;
};

export type TrendsReport = {
  incomeExpenseTrend: IncomeExpenseTrendItem[];
  savingsRateTrend: SavingsRateTrendPoint[];
  categoryTrends: CategoryTrendPoint[];
};

export type NetWorthPoint = {
  month: string;
  netWorth: number;
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

export type AssistantResponse = {
  reply: string;
  intent: string;
  actionTaken: boolean;
  spokenReply: string;
};
