import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { financeApi } from "../services/financeApi";
import { useAuthStore } from "../store/authStore";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8)
});

const registerSchema = loginSchema.extend({
  displayName: z.string().min(2).max(120)
});

type LoginValues = z.infer<typeof loginSchema>;
type RegisterValues = z.infer<typeof registerSchema>;

export function AuthPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const loginForm = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: "", password: "" } });
  const registerForm = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: "", password: "", displayName: "" }
  });

  const mutation = useMutation({
    mutationFn: async (values: LoginValues | RegisterValues) =>
      mode === "login" ? financeApi.login(values as LoginValues) : financeApi.register(values as RegisterValues),
    onSuccess: (response) => {
      setAuth(response);
      navigate("/");
    }
  });

  return (
    <div className="auth-page">
      <div className="auth-hero">
        <p className="eyebrow">Beautiful. Fast. Test-ready.</p>
        <h1>Own every rupee with a dashboard your testers will struggle to break.</h1>
        <p>
          Track income, budgets, goals, recurring payments, and analytics from one polished workspace built for Spring Boot and React.
        </p>
      </div>
      <div className="auth-card">
        <div className="mode-switch">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")} type="button">
            Login
          </button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")} type="button">
            Sign up
          </button>
        </div>
        {mode === "login" ? (
          <form className="auth-form" onSubmit={loginForm.handleSubmit((values) => mutation.mutate(values))}>
            <label>
              Email
              <input {...loginForm.register("email")} placeholder="name@company.com" />
            </label>
            <label>
              Password
              <input {...loginForm.register("password")} type="password" placeholder="Enter password" />
            </label>
            <button className="primary-button" disabled={mutation.isPending} type="submit">
              {mutation.isPending ? "Signing in..." : "Log in"}
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={registerForm.handleSubmit((values) => mutation.mutate(values))}>
            <label>
              Display name
              <input {...registerForm.register("displayName")} placeholder="Finance Captain" />
            </label>
            <label>
              Email
              <input {...registerForm.register("email")} placeholder="name@company.com" />
            </label>
            <label>
              Password
              <input {...registerForm.register("password")} type="password" placeholder="At least 8 characters" />
            </label>
            <button className="primary-button" disabled={mutation.isPending} type="submit">
              {mutation.isPending ? "Creating..." : "Create account"}
            </button>
          </form>
        )}
        {mutation.isError ? <p className="form-error">Authentication failed. Please verify the backend is running.</p> : null}
      </div>
    </div>
  );
}
