import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useMemo, useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import {
  useAccountsQuery,
  useCashFlowForecastQuery,
  useCategoriesQuery,
  useNetWorthQuery,
  useTransactionsQuery,
  useTrendsReportQuery
} from "../hooks/useFinanceQueries";
import { formatCurrency, formatDate } from "../utils/format";

function isoDate(value: Date) {
  return value.toISOString().slice(0, 10);
}

export function ReportsPage() {
  const today = new Date();
  const defaultFrom = new Date(today.getFullYear(), today.getMonth() - 5, 1);
  const [forecastMonths, setForecastMonths] = useState(6);
  const [fromDate, setFromDate] = useState(isoDate(defaultFrom));
  const [toDate, setToDate] = useState(isoDate(today));
  const { data: transactions, isLoading: transactionsLoading, isError: transactionsError } = useTransactionsQuery();
  const { data: accounts } = useAccountsQuery();
  const { data: categories } = useCategoriesQuery();
  const { data: trends, isLoading: trendsLoading, isError: trendsError } = useTrendsReportQuery(fromDate, toDate);
  const { data: netWorth, isLoading: netWorthLoading, isError: netWorthError } = useNetWorthQuery(fromDate, toDate);
  const { data: cashFlowForecast, isLoading: forecastLoading, isError: forecastError } = useCashFlowForecastQuery(forecastMonths);
  const [search, setSearch] = useState("");
  const [accountFilter, setAccountFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [monthFilter, setMonthFilter] = useState("ALL");
  const [yearFilter, setYearFilter] = useState("ALL");
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
    const byCategory = filteredTransactions.reduce<Record<string, number>>((acc, item) => {
      const key = item.categoryName || item.type;
      acc[key] = (acc[key] ?? 0) + item.amount;
      return acc;
    }, {});
    const categorySpend = Object.entries(byCategory)
      .map(([name, total]) => ({ name, total }))
      .sort((left, right) => right.total - left.total)
      .slice(0, 6);
    return { income, expense, net: income - expense, categorySpend };
  }, [filteredTransactions]);

  if (transactionsLoading || trendsLoading || netWorthLoading || forecastLoading) return <LoadingState />;
  if (transactionsError || trendsError || netWorthError || forecastError || !trends || !netWorth || !cashFlowForecast) {
    return <ErrorState message="Reports are unavailable." />;
  }

  const forecastToneClass =
    cashFlowForecast.healthSignal === "CRITICAL"
      ? "summary-card-danger"
      : cashFlowForecast.healthSignal === "WATCH"
        ? "summary-card-warning"
        : "summary-card-success";

  return (
    <div className="stack-layout">
      <Card subtitle="Slice by date, account, category, and type" title="Reports">
        <div className="filter-bar filter-bar-reports">
          <input onChange={(event) => setSearch(event.target.value)} placeholder="Search merchant, note, category..." value={search} />
          <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
          <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
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

      <Card
        subtitle="Projection blends recurring items with recent historical behavior"
        title="Cash Flow Forecast"
        action={
          <select onChange={(event) => setForecastMonths(Number(event.target.value))} value={forecastMonths}>
            <option value={3}>3 months</option>
            <option value={6}>6 months</option>
            <option value={12}>12 months</option>
          </select>
        }
      >
        <div className="summary-grid summary-grid-rich">
          <div className="summary-card summary-card-primary">
            <span>Current Balance</span>
            <strong>{formatCurrency(cashFlowForecast.currentBalance)}</strong>
          </div>
          <div className="summary-card summary-card-success">
            <span>Projected Close</span>
            <strong>{formatCurrency(cashFlowForecast.projectedClosingBalance)}</strong>
          </div>
          <div className={`summary-card ${forecastToneClass}`}>
            <span>Forecast Signal</span>
            <strong>{cashFlowForecast.healthSignal}</strong>
          </div>
        </div>
      </Card>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Income vs expense across the selected range" title="Income vs Expense">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={trends.incomeExpenseTrend}>
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="month" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Line dataKey="income" dot={false} stroke="#14b8a6" strokeWidth={3.2} type="monotone" />
              <Line dataKey="expense" dot={false} stroke="#f97316" strokeWidth={3.2} type="monotone" />
            </LineChart>
          </ResponsiveContainer>
        </Card>

        <Card className="chart-card" subtitle="Savings retained from income each month" title="Savings Rate Trend">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={trends.savingsRateTrend}>
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="month" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Line dataKey="savingsRate" dot={false} stroke="#0f766e" strokeWidth={3.2} type="monotone" />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Top filtered categories in the visible data slice" title="Spend Mix">
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

        <Card className="chart-card" subtitle="Trend points built by the backend insights service" title="Net Worth Tracking">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={netWorth}>
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="month" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Line dataKey="netWorth" dot={false} stroke="#8b5cf6" strokeWidth={3.2} type="monotone" />
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
