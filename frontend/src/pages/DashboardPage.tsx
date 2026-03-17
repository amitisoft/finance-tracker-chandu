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
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { SummaryCard } from "../components/ui/SummaryCard";
import { useDashboardQuery } from "../hooks/useFinanceQueries";
import { formatCurrency, formatDate } from "../utils/format";

export function DashboardPage() {
  const { data, isLoading, isError } = useDashboardQuery();

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Dashboard data is unavailable." />;

  return (
    <div className="page-grid">
      <section className="summary-grid">
        <SummaryCard label="Month Income" tone="success" value={data.currentMonthIncome} />
        <SummaryCard label="Month Expense" tone="danger" value={data.currentMonthExpense} />
        <SummaryCard label="Net Balance" tone="primary" value={data.netBalance} />
      </section>

      <Card className="chart-card" subtitle="Where the money is moving" title="Spending by Category">
        <ResponsiveContainer height={280} width="100%">
          <BarChart data={data.spendingByCategory}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="categoryName" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="total" fill="#1d4ed8" radius={[8, 8, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <Card className="chart-card" subtitle="Six-month runway" title="Income vs Expense">
        <ResponsiveContainer height={280} width="100%">
          <LineChart data={data.incomeVsExpenseTrend}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis />
            <Tooltip />
            <Line dataKey="income" stroke="#16a34a" strokeWidth={3} type="monotone" />
            <Line dataKey="expense" stroke="#dc2626" strokeWidth={3} type="monotone" />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      <Card subtitle="Freshest entries first" title="Recent Transactions">
        <div className="table-list">
          {data.recentTransactions.map((transaction) => (
            <div className="table-row" key={transaction.id}>
              <div>
                <strong>{transaction.merchant || transaction.categoryName || transaction.type}</strong>
                <p>{transaction.accountName}</p>
              </div>
              <div>
                <strong>{formatCurrency(transaction.amount)}</strong>
                <p>{formatDate(transaction.date)}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card subtitle="What needs attention soon" title="Upcoming Recurring">
        <div className="table-list">
          {data.upcomingRecurringPayments.map((item) => (
            <div className="table-row" key={item.id}>
              <div>
                <strong>{item.title}</strong>
                <p>{item.accountName}</p>
              </div>
              <div>
                <strong>{formatCurrency(item.amount)}</strong>
                <p>{formatDate(item.nextRunDate)}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card subtitle="Savings momentum" title="Goals">
        <div className="goal-list">
          {data.goals.map((goal) => (
            <div className="goal-item" key={goal.id}>
              <div className="goal-item-top">
                <strong>{goal.name}</strong>
                <span>{goal.progressPercentage.toFixed(0)}%</span>
              </div>
              <div className="progress-track">
                <div className="progress-bar" style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }} />
              </div>
              <p>
                {formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)}
              </p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
