import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { AppShell } from "../components/layout/AppShell";
import { AuthPage } from "../pages/AuthPage";
import { AccountsPage } from "../pages/AccountsPage";
import { BudgetsPage } from "../pages/BudgetsPage";
import { DashboardPage } from "../pages/DashboardPage";
import { GoalsPage } from "../pages/GoalsPage";
import { InsightsPage } from "../pages/InsightsPage";
import { RecurringPage } from "../pages/RecurringPage";
import { ReportsPage } from "../pages/ReportsPage";
import { RulesPage } from "../pages/RulesPage";
import { TransactionsPage } from "../pages/TransactionsPage";
import { useAuthStore } from "../store/authStore";

function ProtectedLayout() {
  const hydrated = useAuthStore((state) => state.hydrated);
  const token = useAuthStore((state) => state.accessToken);
  if (!hydrated) {
    return null;
  }
  if (!token) {
    return <Navigate to="/auth" replace />;
  }
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

export const router = createBrowserRouter([
  {
    path: "/auth",
    element: <AuthPage />
  },
  {
    path: "/",
    element: <ProtectedLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "insights", element: <InsightsPage /> },
      { path: "rules", element: <RulesPage /> },
      { path: "transactions", element: <TransactionsPage /> },
      { path: "budgets", element: <BudgetsPage /> },
      { path: "goals", element: <GoalsPage /> },
      { path: "reports", element: <ReportsPage /> },
      { path: "recurring", element: <RecurringPage /> },
      { path: "accounts", element: <AccountsPage /> }
    ]
  }
]);
