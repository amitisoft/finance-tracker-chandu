import {
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
import { useDashboardQuery } from "../hooks/useFinanceQueries";

export function ReportsPage() {
  const { data, isLoading, isError } = useDashboardQuery();

  if (isLoading) return <LoadingState />;
  if (isError || !data) return <ErrorState message="Reports are unavailable." />;

  return (
    <Card subtitle="Trend visibility for audits and reviews" title="Reports">
      <ResponsiveContainer height={320} width="100%">
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
  );
}
