import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { useMemo, useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useAccountsQuery, useCategoriesQuery, useDashboardQuery, useTransactionsQuery } from "../hooks/useFinanceQueries";
import { formatCurrency, formatDate } from "../utils/format";

export function ReportsPage() {
  const { data: dashboard, isLoading: dashboardLoading, isError: dashboardError } = useDashboardQuery();
  const { data: transactions, isLoading: transactionsLoading, isError: transactionsError } = useTransactionsQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const [search, setSearch] = useState("");
  const [accountFilter, setAccountFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [monthFilter, setMonthFilter] = useState("ALL");
  const [yearFilter, setYearFilter] = useState("ALL");
  const safeDashboard = dashboard ?? {
    currentMonthIncome: 0,
    currentMonthExpense: 0,
    netBalance: 0,
    spendingByCategory: [],
    incomeVsExpenseTrend: [],
    recentTransactions: [],
    upcomingRecurringPayments: [],
    goals: []
  };
  const safeTransactions = transactions ?? [];
  const availableYears = Array.from(new Set(safeTransactions.map((transaction) => new Date(transaction.date).getFullYear().toString()))).sort().reverse();
  const filteredTransactions = safeTransactions.filter((transaction) => {
    const date = new Date(transaction.date);
    const matchesSearch = [transaction.merchant, transaction.note, transaction.categoryName, transaction.accountName]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(search.toLowerCase()));
    const matchesAccount = accountFilter === "ALL" || transaction.accountId === accountFilter;
    const matchesType = typeFilter === "ALL" || transaction.type === typeFilter;
    const matchesCategory = categoryFilter === "ALL" || transaction.categoryId === categoryFilter;
    const matchesMonth = monthFilter === "ALL" || String(date.getMonth() + 1).padStart(2, "0") === monthFilter;
    const matchesYear = yearFilter === "ALL" || date.getFullYear().toString() === yearFilter;
    return (search ? matchesSearch : true) && matchesAccount && matchesType && matchesCategory && matchesMonth && matchesYear;
  });

  const reportSummary = useMemo(() => {
    const income = filteredTransactions.filter((item) => item.type === "INCOME").reduce((sum, item) => sum + item.amount, 0);
    const expense = filteredTransactions.filter((item) => item.type === "EXPENSE").reduce((sum, item) => sum + item.amount, 0);
    const transfer = filteredTransactions.filter((item) => item.type === "TRANSFER").reduce((sum, item) => sum + item.amount, 0);
    const byCategory = filteredTransactions.reduce<Record<string, number>>((acc, item) => {
      const key = item.categoryName || item.type;
      acc[key] = (acc[key] ?? 0) + item.amount;
      return acc;
    }, {});
    const categorySpend = Object.entries(byCategory)
      .map(([name, total]) => ({ name, total }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 6);

    return {
      income,
      expense,
      transfer,
      net: income - expense,
      categorySpend
    };
  }, [filteredTransactions]);

  if (dashboardLoading || transactionsLoading) return <LoadingState />;
  if (dashboardError || transactionsError || !dashboard || !transactions) return <ErrorState message="Reports are unavailable." />;

  return (
    <div className="stack-layout">
      <Card subtitle="Slice by account, category, type, month, and year" title="Reports">
        <div className="filter-bar filter-bar-reports">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search merchant, note, category..." value={search} />
          <select onChange={(event) => setAccountFilter(event.target.value)} value={accountFilter}>
            <option value="ALL">All accounts</option>
            {(accounts ?? []).map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          <select onChange={(event) => setTypeFilter(event.target.value)} value={typeFilter}>
            <option value="ALL">All types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
            <option value="TRANSFER">Transfer</option>
          </select>
          <select onChange={(event) => setCategoryFilter(event.target.value)} value={categoryFilter}>
            <option value="ALL">All categories</option>
            {(categories ?? []).map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <select onChange={(event) => setMonthFilter(event.target.value)} value={monthFilter}>
            <option value="ALL">All months</option>
            {Array.from({ length: 12 }, (_, index) => {
              const value = String(index + 1).padStart(2, "0");
              return (
                <option key={value} value={value}>
                  {value}
                </option>
              );
            })}
          </select>
          <select onChange={(event) => setYearFilter(event.target.value)} value={yearFilter}>
            <option value="ALL">All years</option>
            {availableYears.map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
        </div>

        <div className="summary-grid summary-grid-rich">
          <div className="summary-card summary-card-success">
            <span>Filtered Income</span>
            <strong>{formatCurrency(reportSummary.income)}</strong>
          </div>
          <div className="summary-card summary-card-danger">
            <span>Filtered Expense</span>
            <strong>{formatCurrency(reportSummary.expense)}</strong>
          </div>
          <div className="summary-card summary-card-primary">
            <span>Net Position</span>
            <strong>{formatCurrency(reportSummary.net)}</strong>
          </div>
        </div>
      </Card>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Filtered category totals" title="Spend Mix">
          <ResponsiveContainer height={300} width="100%">
            <BarChart data={reportSummary.categorySpend}>
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="name" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Bar dataKey="total" fill="#0ea5e9" radius={[10, 10, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card className="chart-card" subtitle="Overall monthly trend" title="Income vs Expense">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={dashboard.incomeVsExpenseTrend}>
            
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="month" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Line dataKey="income" dot={false} stroke="#14b8a6" strokeWidth={3.2} type="monotone" />
              <Line dataKey="expense" dot={false} stroke="#f97316" strokeWidth={3.2} type="monotone" />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <Card subtitle="Latest results after filters are applied" title="Report Entries">
        <div className="thin-list">
          {filteredTransactions.slice(0, 12).map((transaction) => (
            <div className="thin-list-row" key={transaction.id}>
              <div>
                <strong>{transaction.merchant || transaction.categoryName || transaction.type}</strong>
                <span>
                  {transaction.accountName} / {transaction.categoryName || transaction.type} / {formatDate(transaction.date)}
                </span>
              </div>
              <strong>{formatCurrency(transaction.amount)}</strong>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
