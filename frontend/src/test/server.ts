import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

export const mockChannels = [
  { id: 1, channelKey: "#ncbot", channelName: "#ncbot", isDm: false },
  { id: 2, channelKey: "#general", channelName: "#general", isDm: false },
  {
    id: 3,
    channelKey: "d41d8cd98f00b204e9800998ecf8427e",
    channelName: null,
    isDm: true,
  },
];

export const mockMessages = {
  channelId: 1,
  channelName: "#ncbot",
  messages: [
    {
      id: 101,
      senderName: "alice",
      content: "Hello everyone!",
      createdAt: "2026-06-03T14:30:00Z",
      response: "@[alice] Welcome to the channel!",
    },
    {
      id: 100,
      senderName: "bob",
      content: "Hey alice, how's it going?",
      createdAt: "2026-06-03T14:29:00Z",
      response: null,
    },
  ],
  totalPages: 1,
  currentPage: 0,
  totalElements: 2,
};

export const mockMemories = [
  {
    id: 10,
    channelId: 1,
    key: "user.alice",
    value: "Alice is a regular contributor.",
  },
  {
    id: 11,
    channelId: 1,
    key: "channel.topic",
    value: "#ncbot focuses on bot development.",
  },
];

export const mockGlobalMemories = [
  { id: 5, channelId: null, key: "bot.version", value: "Running ncbot v2.3.1" },
  {
    id: 6,
    channelId: null,
    key: "channel.#ncbot",
    value: "#ncbot is the primary development channel.",
  },
];

export const mockParticipants = [
  { name: "alice", lastSeen: "2026-06-03T14:30:00Z" },
  { name: "bob", lastSeen: "2026-06-03T14:29:00Z" },
  { name: "charlie", lastSeen: "2026-06-03T12:00:00Z" },
];

function pageResponse<T>(content: T[]) {
  return {
    content,
    totalPages: 1,
    currentPage: 0,
    totalElements: content.length,
  };
}

export const handlers = [
  // Channels
  http.get("*/v1/channels", () => {
    return HttpResponse.json(pageResponse(mockChannels));
  }),

  http.delete("*/v1/channels/:channelId", () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Messages
  http.get("*/v1/channels/:channelId/messages", () => {
    return HttpResponse.json(mockMessages);
  }),

  // Channel Memory
  http.get("*/v1/channels/:channelId/memory", () => {
    return HttpResponse.json(pageResponse(mockMemories));
  }),

  http.post("*/v1/channels/:channelId/memory", async ({ request }) => {
    const body = (await request.json()) as { key: string; value: string };
    return HttpResponse.json(
      { id: 99, channelId: 1, key: body.key, value: body.value },
      { status: 201 },
    );
  }),

  http.put("*/v1/channels/:channelId/memory/:id", async ({ request }) => {
    const body = (await request.json()) as { key: string; value: string };
    return HttpResponse.json({
      id: 10,
      channelId: 1,
      key: body.key,
      value: body.value,
    });
  }),

  http.delete("*/v1/channels/:channelId/memory/:id", () => {
    return new HttpResponse(null, { status: 204 });
  }),

  http.post("*/v1/channels/:channelId/memory/:id/promote", () => {
    return HttpResponse.json({
      id: 99,
      channelId: null,
      key: "user.alice",
      value: "Alice is a regular contributor.",
    });
  }),

  // Channel Participants
  http.get("*/v1/channels/:channelId/participants", () => {
    return HttpResponse.json(pageResponse(mockParticipants));
  }),

  // Global Memory
  http.get("*/v1/memory", () => {
    return HttpResponse.json(pageResponse(mockGlobalMemories));
  }),

  http.post("*/v1/memory", async ({ request }) => {
    const body = (await request.json()) as { key: string; value: string };
    return HttpResponse.json(
      { id: 99, channelId: null, key: body.key, value: body.value },
      { status: 201 },
    );
  }),

  http.put("*/v1/memory/:id", async ({ request }) => {
    const body = (await request.json()) as { key: string; value: string };
    return HttpResponse.json({
      id: 5,
      channelId: null,
      key: body.key,
      value: body.value,
    });
  }),

  http.delete("*/v1/memory/:id", () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Global Participants
  http.get("*/v1/participants", () => {
    return HttpResponse.json(pageResponse(mockParticipants));
  }),
];

export const server = setupServer(...handlers);