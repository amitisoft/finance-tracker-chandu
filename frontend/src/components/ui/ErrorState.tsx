type ErrorStateProps = {
  message?: string;
};

export function ErrorState({ message = "We couldn't load this section right now." }: ErrorStateProps) {
  return <div className="empty-state error-state">{message}</div>;
}
