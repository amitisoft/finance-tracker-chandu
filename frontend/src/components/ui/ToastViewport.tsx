import clsx from "clsx";
import { useToastStore } from "../../store/toastStore";

export function ToastViewport() {
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);

  return (
    <div className="toast-viewport">
      {toasts.map((toast) => (
        <div key={toast.id} className={clsx("toast-item", `toast-item-${toast.tone}`)}>
          <div className="toast-copy">
            <strong>{toast.tone === "error" ? "Action failed" : toast.tone === "success" ? "Saved" : "Notice"}</strong>
            <span>{toast.message}</span>
          </div>
          <button aria-label="Dismiss notification" className="toast-close" onClick={() => removeToast(toast.id)} type="button">
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
