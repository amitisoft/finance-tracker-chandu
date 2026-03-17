import { formatCurrency, getBudgetStatus } from "./format";

describe("format utilities", () => {
  it("formats currency in INR style", () => {
    expect(formatCurrency(1234)).toContain("1,234");
  });

  it("returns expected budget status", () => {
    expect(getBudgetStatus(65)).toBe("healthy");
    expect(getBudgetStatus(85)).toBe("warning");
    expect(getBudgetStatus(105)).toBe("danger");
  });
});
