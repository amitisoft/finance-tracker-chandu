import { render, screen } from "@testing-library/react";
import { SummaryCard } from "./SummaryCard";

describe("SummaryCard", () => {
  it("renders the label and formatted currency", () => {
    render(<SummaryCard label="Net Balance" value={12500} />);
    expect(screen.getByText("Net Balance")).toBeInTheDocument();
    expect(screen.getByText(/12,500/)).toBeInTheDocument();
  });
});
