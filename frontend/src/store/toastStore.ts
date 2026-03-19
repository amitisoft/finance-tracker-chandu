import { create } from "zustand";

export type ToastTone = "success" | "error" | "info";

type ToastItem = {
  id: number;
  message: string;
  tone: ToastTone;
};

type ToastState = {
  toasts: ToastItem[];
  pushToast: (message: string, tone?: ToastTone) => void;
  removeToast: (id: number) => void;
};

let nextToastId = 1;

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  pushToast: (message, tone = "info") => {
    const id = nextToastId++;
    set((state) => ({
      toasts: [...state.toasts, { id, message, tone }]
    }));
    window.setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((toast) => toast.id !== id)
      }));
    }, 3600);
  },
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id)
    }))
}));
