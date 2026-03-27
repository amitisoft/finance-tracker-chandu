import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useMemo, useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { useHealthScoreQuery, useInsightsQuery, useNetWorthQuery, useTrendsReportQuery } from "../hooks/useFinanceQueries";

function isoDate(value: Date) {
  return value.toISOString().slice(0, 10);
}

export function InsightsPage() {
  const today = new Date();
  const defaultFrom = new Date(today.getFullYear(), today.getMonth() - 5, 1);
  const [fromDate, setFromDate] = useState(isoDate(defaultFrom));
  const [toDate, setToDate] = useState(isoDate(today));
  const { data: healthScore, isLoading: healthLoading, isError: healthError } = useHealthScoreQuery();
  const { data: insights, isLoading: insightsLoading, isError: insightsError } = useInsightsQuery();
  const { data: trends, isLoading: trendsLoading, isError: trendsError } = useTrendsReportQuery(fromDate, toDate);
  const { data: netWorth, isLoading: netWorthLoading, isError: netWorthError } = useNetWorthQuery(fromDate, toDate);

  const categoryHighlights = useMemo(() => {
    return (trends?.categoryTrends ?? []).slice().sort((left, right) => right.total - left.total).slice(0, 8);
  }, [trends?.categoryTrends]);

  if (healthLoading || insightsLoading || trendsLoading || netWorthLoading) return <LoadingState />;
  if (healthError || insightsError || trendsError || netWorthError || !healthScore || !insights || !trends || !netWorth) {
    return <ErrorState message="Insights are unavailable." />;
  }

  return (
    <div className="stack-layout">
      <Card subtitle="Health score, insights, and trend breakdowns" title="Financial Insights">
        <div className="filter-bar filter-bar-goals">
          <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
          <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
        </div>

        <div className="summary-grid summary-grid-rich">
          <div className="summary-card summary-card-primary">
            <span>Health Score</span>
            <strong>{healthScore.score.toFixed(1)}</strong>
          </div>
          <div className="summary-card summary-card-success">
            <span>Score Band</span>
            <strong>{healthScore.band}</strong>
          </div>
          <div className="summary-card summary-card-warning">
            <span>Suggestions</span>
            <strong>{healthScore.suggestions.length}</strong>
          </div>
        </div>
      </Card>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Weighted score by factor" title="Score Breakdown">
          <div className="thin-list">
            {healthScore.breakdown.map((item) => (
              <div className="thin-list-row" key={item.factor}>
                <div>
                  <strong>{item.factor}</strong>
                  <span>{item.summary}</span>
                </div>
                <strong>{item.score.toFixed(1)}</strong>
              </div>
            ))}
          </div>
        </Card>

        <Card className="chart-card" subtitle="Actions suggested by the scoring model" title="Suggestions">
          <div className="thin-list">
            {healthScore.suggestions.length ? (
              healthScore.suggestions.map((suggestion) => (
                <div className="thin-list-row" key={suggestion}>
                  <div>
                    <strong>Recommendation</strong>
                    <span>{suggestion}</span>
                  </div>
                </div>
              ))
            ) : (
              <div className="empty-state compact-empty">No immediate suggestions. Current trends look stable.</div>
            )}
          </div>
        </Card>
      </div>

      <Card subtitle="Backend-generated findings from recent activity" title="Highlights">
        <div className="thin-list">
          {insights.map((item) => (
            <div className="thin-list-row" key={item.title}>
              <div>
                <strong>{item.title}</strong>
                <span>{item.message}</span>
              </div>
              <strong>{item.tone.toUpperCase()}</strong>
            </div>
          ))}
        </div>
      </Card>

      <div className="chart-row">
        <Card className="chart-card" subtitle="Income retained after expenses" title="Savings Rate Trend">
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

        <Card className="chart-card" subtitle="Rolling total account value over time" title="Net Worth Trend">
          <ResponsiveContainer height={300} width="100%">
            <LineChart data={netWorth}>
              <CartesianGrid stroke="rgba(148, 163, 184, 0.18)" strokeDasharray="4 4" vertical={false} />
              <XAxis axisLine={false} dataKey="month" tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip />
              <Line dataKey="netWorth" dot={false} stroke="#f97316" strokeWidth={3.2} type="monotone" />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <Card subtitle="Highest category totals in the selected range" title="Category Trend Highlights">
        <div className="thin-list">
          {categoryHighlights.map((item, index) => (
            <div className="thin-list-row" key={`${item.month}-${item.categoryName}-${index}`}>
              <div>
                <strong>{item.categoryName}</strong>
                <span>{item.month}</span>
              </div>
              <strong>{item.total.toFixed(2)}</strong>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
