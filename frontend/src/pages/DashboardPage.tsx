import {
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { SummaryCard } from "../components/ui/SummaryCard";
import { useDashboardQuery } from "../hooks/useFinanceQueries";
import { formatCurrency, formatDate } from "../utils/format";

const pieColors = ["#0f766e", "#f97316", "#38bdf8", "#8b5cf6", "#f43f5e", "#14b8a6", "#84cc16"];
const feedTabs = [
  { id: "transactions", label: "Transactions" },
  { id: "goals", label: "Goals" },
  { id: "recurring", label: "Recurring" }
] as const;

export function DashboardPage() {
  const { data, isLoading, isError } = useDashboardQuery();
  const navigate = useNavigate();
  const [activeFeed, setActiveFeed] = useState<(typeof feedTabs)[number]["id"]>("transactions");
  const dashboard = data ?? {
    currentMonthIncome: 0,
    currentMonthExpense: 0,
    netBalance: 0,
    spendingByCategory: [],
    incomeVsExpenseTrend: [],
    recentTransactions: [],
    upcomingRecurringPayments: [],
    goals: []
  };

  const feedItems = useMemo(() => {
    if (activeFeed === "goals") {
      return dashboard.goals.slice(0, 5).map((goal) => ({
        id: goal.id,
        title: goal.name,
        meta: `${goal.progressPercentage.toFixed(0)}% complete`,
        value: `${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}`
      }));
    }
    if (activeFeed === "recurring") {
      return dashboard.upcomingRecurringPayments.slice(0, 5).map((item) => ({
        id: item.id,
        title: item.title,
        meta: `${item.frequency} / ${formatDate(item.nextRunDate)}`,
        value: formatCurrency(item.amount)
      }));
    }
    return dashboard.recentTransactions.slice(0, 5).map((transaction) => ({
      id: transaction.id,
      title: transaction.merchant || transaction.categoryName || transaction.type,
      meta: `${transaction.accountName} / ${formatDate(transaction.date)}`,
      value: formatCurrency(transaction.amount)
    }));
  }, [activeFeed, dashboard.goals, dashboard.recentTransactions, dashboard.upcomingRecurringPayments]);

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Dashboard data is unavailable." />;

  const cashRunway = data.currentMonthIncome - data.currentMonthExpense;
  const latestGoal = data.goals[0];
  const latestTransaction = data.recentTransactions[0];
  const spendingTotal = data.spendingByCategory.reduce((sum, item) => sum + item.total, 0);

  return (
    <div className="dashboard-layout">
      <section className="dashboard-hero">
        <div className="dashboard-hero-copy">
          <p className="eyebrow">Financial Pulse</p>
          <h2>{cashRunway >= 0 ? "You are operating in the green this month." : "Expenses are outrunning income this month."}</h2>
          <p>
            Net position is <strong>{formatCurrency(data.netBalance)}</strong> with {data.recentTransactions.length} recent transaction
            {data.recentTransactions.length === 1 ? "" : "s"} and {data.upcomingRecurringPayments.length} recurring item
            {data.upcomingRecurringPayments.length === 1 ? "" : "s"} queued.
          </p>
          <div className="dashboard-hero-actions">
            <button className="primary-button" onClick={() => navigate("/transactions?compose=1")} type="button">
              Add transaction
            </button>
          </div>
          <div className="dashboard-hero-stats">
            <div>
              <span>Cash runway</span>
              <strong>{formatCurrency(cashRunway)}</strong>
            </div>
            <div>
              <span>Goals in motion</span>
              <strong>{data.goals.length}</strong>
            </div>
            <div>
              <span>Recurring next up</span>
              <strong>{data.upcomingRecurringPayments[0] ? formatDate(data.upcomingRecurringPayments[0].nextRunDate) : "None"}</strong>
            </div>
          </div>
        </div>
        <div className="dashboard-hero-panel">
          <p className="eyebrow">Spotlight</p>
          <strong>{latestGoal ? latestGoal.name : "No active goals yet"}</strong>
          <span>
            {latestGoal
              ? `${formatCurrency(latestGoal.currentAmount)} saved of ${formatCurrency(latestGoal.targetAmount)}`
              : "Create a goal to keep progress visible here."}
          </span>
          <div className="progress-track">
            <div className="progress-bar" style={{ width: `${Math.min(latestGoal?.progressPercentage ?? 0, 100)}%` }} />
          </div>
          <div className="dashboard-hero-transaction">
            <label>Latest movement</label>
            <strong>{latestTransaction ? formatCurrency(latestTransaction.amount) : "No transactions"}</strong>
            <span>
              {latestTransaction
                ? `${latestTransaction.merchant || latestTransaction.categoryName || latestTransaction.type} / ${formatDate(latestTransaction.date)}`
                : "Add transactions to unlock live momentum."}
            </span>
          </div>
        </div>
      </section>

      <section className="summary-grid summary-grid-rich">
        <SummaryCard label="Month Income" tone="success" value={data.currentMonthIncome} />
        <SummaryCard label="Month Expense" tone="danger" value={data.currentMonthExpense} />
        <SummaryCard label="Net Balance" tone="primary" value={data.netBalance} />
      </section>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Hover slices to inspect the mix" title="Spending by Category">
          <ResponsiveContainer height={300} width="100%">
            <PieChart>
              <Pie data={data.spendingByCategory} cx="50%" cy="50%" dataKey="total" innerRadius={68} outerRadius={110} paddingAngle={3}>
                {data.spendingByCategory.map((item, index) => (
                  <Cell fill={pieColors[index % pieColors.length]} key={item.categoryName} />
                ))}
              </Pie>
              <Tooltip
                content={({ active, payload }) => {
                  if (!active || !payload?.length) return null;
                  const item = payload[0].payload as { categoryName: string; total: number };
                  const percentage = spendingTotal ? (item.total / spendingTotal) * 100 : 0;
                  return (
                    <div className="chart-tooltip">
                      <strong>{item.categoryName}</strong>
                      <span>{formatCurrency(item.total)} spent</span>
                      <span>{percentage.toFixed(1)}% of monthly spend</span>
                    </div>
                  );
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        <Card className="chart-card" subtitle="Six-month runway" title="Income vs Expense">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={data.incomeVsExpenseTrend}>
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

      <Card subtitle="Switch the feed below to see the latest five entries" title="Latest Activity">
        <div className="feed-switcher">
          {feedTabs.map((tab) => (
            <button
              key={tab.id}
              className={activeFeed === tab.id ? "feed-tab active" : "feed-tab"}
              onClick={() => setActiveFeed(tab.id)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="thin-list">
          {feedItems.map((item) => (
            <div className="thin-list-row" key={item.id}>
              <div>
                <strong>{item.title}</strong>
                <span>{item.meta}</span>
              </div>
              <strong>{item.value}</strong>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
