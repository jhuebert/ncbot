import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { queryClient } from "@/lib/query-client";
import { ChannelsPage } from "@/pages/channels-page";
import { GlobalMemoryPage } from "@/pages/global-memory-page";
import { ParticipantsPage } from "@/pages/participants-page";
import { SettingsPage } from "@/pages/settings-page";
import { NotFoundPage } from "@/pages/not-found-page";

// Mutable mock state so block/unblock mutations reflect on refetch.
const participantList: {
  id: number;
  name: string;
  firstSeen: string;
  lastSeen: string;
  pathUpgradeNotifiedAt: string | null;
  blocked: boolean;
}[] = [
  {
    id: 1,
    name: "alice",
    firstSeen: "2026-06-01T09:00:00Z",
    lastSeen: "2026-06-03T14:30:00Z",
    pathUpgradeNotifiedAt: "2026-06-02T10:00:00Z",
    blocked: false,
  },
  {
    id: 2,
    name: "bob",
    firstSeen: "2026-06-01T10:00:00Z",
    lastSeen: "2026-06-03T14:29:00Z",
    pathUpgradeNotifiedAt: null,
    blocked: false,
  },
];

// Mutable config state so update/reset mutations reflect on refetch.
const configState: {
  key: string;
  type: string;
  value: string;
  defaultValue: string;
  description: string;
  restartRequired: boolean;
  isDefault: boolean;
}[] = [
  {
    key: "bot.name",
    type: "STRING",
    value: "ncbot",
    defaultValue: "ncbot",
    description: "Bot display name.",
    restartRequired: false,
    isDefault: true,
  },
  {
    key: "chat.max-reply-bytes",
    type: "INT",
    value: "128",
    defaultValue: "128",
    description: "Maximum UTF-8 byte length of a reply.",
    restartRequired: false,
    isDefault: true,
  },
  {
    key: "chat.ai-enabled",
    type: "BOOLEAN",
    value: "true",
    defaultValue: "true",
    description: "Master switch for the AI.",
    restartRequired: false,
    isDefault: true,
  },
];

const server = setupServer(
  http.get("*/v1/channels", ({ request }) => {
    const query = new URL(request.url).searchParams.get("query")?.toLowerCase();
    const channels = [
      {
        id: 1,
        channelKey: "#ncbot",
        channelName: "#ncbot",
        isDm: false,
        lastMessageAt: "2026-06-03T14:30:00Z",
      },
      {
        id: 2,
        channelKey: "#general",
        channelName: "#general",
        isDm: false,
        lastMessageAt: "2026-06-02T10:00:00Z",
      },
      {
        id: 3,
        channelKey: "d41d8cd98f00b204e9800998ecf8427e",
        channelName: null,
        isDm: true,
        lastMessageAt: null,
      },
    ];
    const content = query
      ? channels.filter(
          (c) =>
            c.channelName?.toLowerCase().includes(query) ||
            c.channelKey.toLowerCase().includes(query),
        )
      : channels;
    return HttpResponse.json({
      content,
      totalPages: 1,
      currentPage: 0,
      totalElements: content.length,
    });
  }),
  http.get("*/v1/memory", ({ request }) => {
    const query = new URL(request.url).searchParams.get("query")?.toLowerCase();
    const memories = [
      {
        id: 5,
        channelId: null,
        key: "bot.version",
        value: "Running ncbot v2.3.1",
      },
    ];
    const content = query
      ? memories.filter(
          (m) =>
            m.key.toLowerCase().includes(query) ||
            m.value.toLowerCase().includes(query),
        )
      : memories;
    return HttpResponse.json({
      content,
      totalPages: 1,
      currentPage: 0,
      totalElements: content.length,
    });
  }),
  http.get("*/v1/participants", ({ request }) => {
    const query = new URL(request.url).searchParams.get("query")?.toLowerCase();
    const content = query
      ? participantList.filter((p) => p.name.toLowerCase().includes(query))
      : participantList;
    return HttpResponse.json({
      content,
      totalPages: 1,
      currentPage: 0,
      totalElements: content.length,
    });
  }),
  http.put("*/v1/participants/:participantId", async ({ request, params }) => {
    const body = (await request.json()) as { blocked: boolean };
    const participant = participantList.find(
      (p) => p.id === Number(params.participantId),
    );
    if (participant) {
      participant.blocked = body.blocked;
    }
    return HttpResponse.json(participant);
  }),
  http.get("*/v1/config", () => {
    return HttpResponse.json(configState);
  }),
  http.put("*/v1/config/:key", async ({ request, params }) => {
    const body = (await request.json()) as { value: string };
    const item = configState.find((c) => c.key === String(params.key));
    if (item) {
      item.value = body.value;
      item.isDefault = body.value === item.defaultValue;
    }
    return HttpResponse.json(item);
  }),
  http.delete("*/v1/config/:key", ({ params }) => {
    const item = configState.find((c) => c.key === String(params.key));
    if (item) {
      item.value = item.defaultValue;
      item.isDefault = true;
    }
    return HttpResponse.json(item);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "bypass" }));
afterEach(() => {
  server.resetHandlers();
  queryClient.clear();
  participantList.length = 0;
  participantList.push(
    {
      id: 1,
      name: "alice",
      firstSeen: "2026-06-01T09:00:00Z",
      lastSeen: "2026-06-03T14:30:00Z",
      pathUpgradeNotifiedAt: "2026-06-02T10:00:00Z",
      blocked: false,
    },
    {
      id: 2,
      name: "bob",
      firstSeen: "2026-06-01T10:00:00Z",
      lastSeen: "2026-06-03T14:29:00Z",
      pathUpgradeNotifiedAt: null,
      blocked: false,
    },
  );
  configState.forEach((c) => {
    c.value = c.defaultValue;
    c.isDefault = true;
  });
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

  it("shows last message column", async () => {
    renderPage(<ChannelsPage />);
    expect((await screen.findAllByText("#ncbot")).length).toBeGreaterThan(0);
    expect(screen.getByText("Last Message")).toBeDefined();
    expect(screen.getByText("Last Activity")).toBeDefined();
    expect(screen.getAllByText("—").length).toBeGreaterThan(0);
  });

  it("filters channels by search query", async () => {
    renderPage(<ChannelsPage />);
    expect((await screen.findAllByText("#ncbot")).length).toBeGreaterThan(0);
    fireEvent.change(screen.getByLabelText("Search channels"), {
      target: { value: "general" },
    });
    await waitFor(() => expect(screen.queryByText("#ncbot")).toBeNull());
    expect((await screen.findAllByText("#general")).length).toBeGreaterThan(0);
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

  it("filters memories by search query", async () => {
    renderPage(<GlobalMemoryPage />);
    expect(await screen.findByText("bot.version")).toBeDefined();
    fireEvent.change(screen.getByLabelText("Search global memory"), {
      target: { value: "nonexistent" },
    });
    await waitFor(() => expect(screen.queryByText("bot.version")).toBeNull());
    expect(await screen.findByText("No global memories yet.")).toBeDefined();
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

  it("shows blocked status and block/unblock buttons", async () => {
    renderPage(<ParticipantsPage />);
    expect(await screen.findByText("alice")).toBeDefined();
    expect(screen.getAllByText("Active").length).toBe(2);
    const blockButtons = screen.getAllByText("Block");
    expect(blockButtons.length).toBe(2);
  });

  it("shows first seen and last notified columns", async () => {
    renderPage(<ParticipantsPage />);
    expect(await screen.findByText("alice")).toBeDefined();
    expect(screen.getByText("First Seen")).toBeDefined();
    expect(screen.getByText("Last Notified")).toBeDefined();
    expect(screen.getAllByText("—").length).toBeGreaterThan(0);
  });

  it("blocks a participant on click", async () => {
    renderPage(<ParticipantsPage />);
    const blockButtons = await screen.findAllByText("Block");
    fireEvent.click(blockButtons[0]);
    expect(await screen.findByText("Unblock")).toBeDefined();
  });

  it("filters participants by search query", async () => {
    renderPage(<ParticipantsPage />);
    expect(await screen.findByText("alice")).toBeDefined();
    fireEvent.change(screen.getByLabelText("Search participants"), {
      target: { value: "bob" },
    });
    await waitFor(() => expect(screen.queryByText("alice")).toBeNull());
    expect(await screen.findByText("bob")).toBeDefined();
  });
});

describe("NotFoundPage", () => {
  it("renders not found message", () => {
    renderPage(<NotFoundPage />);
    expect(screen.getByText("Page Not Found")).toBeDefined();
  });
});

describe("SettingsPage", () => {
  it("renders heading and groups", async () => {
    renderPage(<SettingsPage />);
    expect(screen.getByText("Settings")).toBeDefined();
    expect(await screen.findByText("Bot Identity & Prompts")).toBeDefined();
    expect(screen.getByText("Chat Behaviour")).toBeDefined();
  });

  it("displays configuration items", async () => {
    renderPage(<SettingsPage />);
    expect(await screen.findByText("bot.name")).toBeDefined();
    expect(screen.getByText("chat.max-reply-bytes")).toBeDefined();
    expect(screen.getByText("chat.ai-enabled")).toBeDefined();
  });

  it("updates an integer configuration item", async () => {
    renderPage(<SettingsPage />);
    const input = (await screen.findByLabelText(
      "Value for chat.max-reply-bytes",
    )) as HTMLInputElement;
    const row = input.closest("li")!;
    fireEvent.change(input, { target: { value: "200" } });
    fireEvent.click(within(row).getByText("Save"));
    await waitFor(() =>
      expect(
        configState.find((c) => c.key === "chat.max-reply-bytes")?.value,
      ).toBe("200"),
    );
    expect(screen.queryByText("Custom")).toBeDefined();
  });

  it("reverts a custom value to default on reset", async () => {
    renderPage(<SettingsPage />);
    const input = (await screen.findByLabelText(
      "Value for bot.name",
    )) as HTMLInputElement;
    const row = input.closest("li")!;
    fireEvent.change(input, { target: { value: "custom-bot" } });
    fireEvent.click(within(row).getByText("Save"));
    await waitFor(() =>
      expect(configState.find((c) => c.key === "bot.name")?.value).toBe(
        "custom-bot",
      ),
    );
    // The row remounts after the update; re-query it before resetting.
    const updatedInput = (await screen.findByLabelText(
      "Value for bot.name",
    )) as HTMLInputElement;
    fireEvent.click(
      within(updatedInput.closest("li")!).getByText("Reset to default"),
    );
    await waitFor(() =>
      expect(configState.find((c) => c.key === "bot.name")?.value).toBe(
        "ncbot",
      ),
    );
    expect(screen.queryByText("Custom")).toBeNull();
  });
});
