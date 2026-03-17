import type { PropsWithChildren, ReactNode } from "react";
import clsx from "clsx";

type CardProps = PropsWithChildren<{
  title?: string;
  subtitle?: string;
  action?: ReactNode;
  className?: string;
}>;

export function Card({ title, subtitle, action, className, children }: CardProps) {
  return (
    <section className={clsx("card", className)}>
      {(title || action) && (
        <header className="card-header">
          <div>
            {title ? <h3>{title}</h3> : null}
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
          {action}
        </header>
      )}
      {children}
    </section>
  );
}
