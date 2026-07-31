import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { queryClient } from "@/lib/query-client";
import { ChannelsPage } from "@/pages/channels-page";
import { GlobalMemoryPage } from "@/pages/global-memory-page";
import { ParticipantsPage } from "@/pages/participants-page";
import { NotFoundPage } from "@/pages/not-found-page";

const server = setupServer(
  http.get("*/v1/channels", () =>
    HttpResponse.json({
      content: [
        { id: 1, channelKey: "#ncbot", channelName: "#ncbot", isDm: false },
        { id: 2, channelKey: "#general", channelName: "#general", isDm: false },
        { id: 3, channelKey: "d41d8cd98f00b204e9800998ecf8427e", channelName: null, isDm: true },
      ],
      totalPages: 1,
      currentPage: 0,
      totalElements: 3,
    }),
  ),
  http.get("*/v1/memory", () =>
    HttpResponse.json({
      content: [
        { id: 5, channelId: null, key: "bot.version", value: "Running ncbot v2.3.1" },
      ],
      totalPages: 1,
      currentPage: 0,
      totalElements: 1,
    }),
  ),
  http.get("*/v1/participants", () =>
    HttpResponse.json({
      content: [
        { name: "alice", lastSeen: "2026-06-03T14:30:00Z" },
        { name: "bob", lastSeen: "2026-06-03T14:29:00Z" },
      ],
      totalPages: 1,
      currentPage: 0,
      totalElements: 2,
    }),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: "bypass" }));
afterEach(() => {
  server.resetHandlers();
  queryClient.clear();
});
afterAll(() => server.close());

function renderPage(ui: React.ReactElement) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ChannelsPage", () => {
  it("renders heading", () => {
    renderPage(<ChannelsPage />);
    expect(screen.getByText("Channels")).toBeDefined();
  });

  it("displays channels after loading", async () => {
    renderPage(<ChannelsPage />);
    const ncbotElements = await screen.findAllByText("#ncbot");
    expect(ncbotElements.length).toBeGreaterThanOrEqual(1);
    const generalElements = screen.getAllByText("#general");
    expect(generalElements.length).toBeGreaterThanOrEqual(1);
  });

  it("shows DM badge", async () => {
    renderPage(<ChannelsPage />);
    expect(await screen.findByText("DM")).toBeDefined();
  });
});

describe("GlobalMemoryPage", () => {
  it("renders heading", () => {
    renderPage(<GlobalMemoryPage />);
    expect(screen.getByText("Global Memory")).toBeDefined();
  });

  it("displays memories after loading", async () => {
    renderPage(<GlobalMemoryPage />);
    expect(await screen.findByText("bot.version")).toBeDefined();
  });
});

describe("ParticipantsPage", () => {
  it("renders heading", () => {
    renderPage(<ParticipantsPage />);
    expect(screen.getByText("Participants")).toBeDefined();
  });

  it("displays participants after loading", async () => {
    renderPage(<ParticipantsPage />);
    expect(await screen.findByText("alice")).toBeDefined();
    expect(screen.getByText("bob")).toBeDefined();
  });
});

describe("NotFoundPage", () => {
  it("renders not found message", () => {
    renderPage(<NotFoundPage />);
    expect(screen.getByText("Page Not Found")).toBeDefined();
  });
});