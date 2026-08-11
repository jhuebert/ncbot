import { client, parseApiError } from "./client";
import type { components } from "./schema";

// ── Properly typed DTOs (generated schema has all-optional fields from allOf) ──

export interface ChannelDto {
  id: number;
  channelKey: string;
  channelName: string | null;
  isDm: boolean;
  /** ISO-8601 timestamp of the most recent message, or null if the channel has none. */
  lastMessageAt: string | null;
}

export interface MessageDto {
  id: number;
  senderName: string;
  content: string;
  createdAt: string;
  /** The ncbot response text, or null if the bot did not respond. */
  response: string | null;
}

export interface MemoryDto {
  id: number;
  channelId: number | null;
  key: string;
  value: string;
}

export interface ParticipantDto {
  id: number;
  name: string;
  firstSeen: string | null;
  lastSeen: string | null;
  pathUpgradeNotifiedAt: string | null;
  blocked: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

export interface MessagesResponse {
  channelId: number;
  channelName: string | null;
  messages: MessageDto[];
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

export type MemoryCreateRequest = components["schemas"]["MemoryCreateRequest"];
export type MemoryUpdateRequest = components["schemas"]["MemoryUpdateRequest"];

// ── Configuration items ──

export interface ConfigItemDto {
  key: string;
  type: "STRING" | "TEXT" | "BOOLEAN" | "INT" | "LONG" | "LIST";
  value: string;
  defaultValue: string;
  description: string;
  restartRequired: boolean;
  isDefault: boolean;
}

export async function fetchConfigItems(): Promise<ConfigItemDto[]> {
  const { data, error, response } = await client.GET("/v1/config");
  if (error) throw await parseApiError(response);
  return (data ?? []) as ConfigItemDto[];
}

export async function updateConfigItem(
  key: string,
  value: string,
): Promise<ConfigItemDto> {
  const { data, error, response } = await client.PUT("/v1/config/{key}", {
    params: { path: { key } },
    body: { value },
  });
  if (error) throw await parseApiError(response);
  return data as unknown as ConfigItemDto;
}

export async function resetConfigItem(key: string): Promise<ConfigItemDto> {
  const { data, error, response } = await client.DELETE("/v1/config/{key}", {
    params: { path: { key } },
  });
  if (error) throw await parseApiError(response);
  return data as unknown as ConfigItemDto;
}

// ── Channels ──

export async function fetchChannels(params?: {
  dm?: boolean;
  query?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<ChannelDto>> {
  const { data, error, response } = await client.GET("/v1/channels", {
    params: { query: params },
  });
  if (error) throw await parseApiError(response);
  return data as unknown as PageResponse<ChannelDto>;
}

export async function deleteChannel(channelId: number): Promise<void> {
  const { error, response } = await client.DELETE("/v1/channels/{channelId}", {
    params: { path: { channelId } },
  });
  if (error) throw await parseApiError(response);
}

// ── Messages ──

export async function fetchChannelMessages(
  channelId: number,
  params?: {
    before?: string;
    after?: string;
    page?: number;
    size?: number;
    sortDirection?: "ASC" | "DESC";
  },
): Promise<MessagesResponse> {
  const { data, error, response } = await client.GET(
    "/v1/channels/{channelId}/messages",
    { params: { path: { channelId }, query: params } },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as MessagesResponse;
}

// ── Channel Memory ──

export async function fetchChannelMemory(
  channelId: number,
  params?: { query?: string; page?: number; size?: number },
): Promise<PageResponse<MemoryDto>> {
  const { data, error, response } = await client.GET(
    "/v1/channels/{channelId}/memory",
    { params: { path: { channelId }, query: params } },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as PageResponse<MemoryDto>;
}

export async function createChannelMemory(
  channelId: number,
  body: MemoryCreateRequest,
): Promise<MemoryDto> {
  const { data, error, response } = await client.POST(
    "/v1/channels/{channelId}/memory",
    { params: { path: { channelId } }, body },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as MemoryDto;
}

export async function updateChannelMemory(
  channelId: number,
  id: number,
  body: MemoryUpdateRequest,
): Promise<MemoryDto> {
  const { data, error, response } = await client.PUT(
    "/v1/channels/{channelId}/memory/{id}",
    { params: { path: { channelId, id } }, body },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as MemoryDto;
}

export async function deleteChannelMemory(
  channelId: number,
  id: number,
): Promise<void> {
  const { error, response } = await client.DELETE(
    "/v1/channels/{channelId}/memory/{id}",
    { params: { path: { channelId, id } } },
  );
  if (error) throw await parseApiError(response);
}

export async function promoteMemory(
  channelId: number,
  id: number,
): Promise<MemoryDto> {
  const { data, error, response } = await client.POST(
    "/v1/channels/{channelId}/memory/{id}/promote",
    { params: { path: { channelId, id } } },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as MemoryDto;
}

// ── Channel Participants ──

export async function fetchChannelParticipants(
  channelId: number,
  params?: { query?: string; page?: number; size?: number },
): Promise<PageResponse<ParticipantDto>> {
  const { data, error, response } = await client.GET(
    "/v1/channels/{channelId}/participants",
    { params: { path: { channelId }, query: params } },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as PageResponse<ParticipantDto>;
}

// ── Global Memory ──

export async function fetchGlobalMemory(params?: {
  query?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<MemoryDto>> {
  const { data, error, response } = await client.GET("/v1/memory", {
    params: { query: params },
  });
  if (error) throw await parseApiError(response);
  return data as unknown as PageResponse<MemoryDto>;
}

export async function createGlobalMemory(
  body: MemoryCreateRequest,
): Promise<MemoryDto> {
  const { data, error, response } = await client.POST("/v1/memory", {
    body,
  });
  if (error) throw await parseApiError(response);
  return data as unknown as MemoryDto;
}

export async function updateGlobalMemory(
  id: number,
  body: MemoryUpdateRequest,
): Promise<MemoryDto> {
  const { data, error, response } = await client.PUT("/v1/memory/{id}", {
    params: { path: { id } },
    body,
  });
  if (error) throw await parseApiError(response);
  return data as unknown as MemoryDto;
}

export async function deleteGlobalMemory(id: number): Promise<void> {
  const { error, response } = await client.DELETE("/v1/memory/{id}", {
    params: { path: { id } },
  });
  if (error) throw await parseApiError(response);
}

// ── Global Participants ──

export async function fetchAllParticipants(params?: {
  query?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<ParticipantDto>> {
  const { data, error, response } = await client.GET("/v1/participants", {
    params: { query: params },
  });
  if (error) throw await parseApiError(response);
  return data as unknown as PageResponse<ParticipantDto>;
}

/**
 * Toggles the explicit block flag on a participant. Blocked participants are
 * ignored by the bot (unless they match an `allow-user` regex).
 */
export async function updateParticipantBlocked(
  participantId: number,
  blocked: boolean,
): Promise<ParticipantDto> {
  const { data, error, response } = await client.PUT(
    "/v1/participants/{participantId}",
    { params: { path: { participantId } }, body: { blocked } },
  );
  if (error) throw await parseApiError(response);
  return data as unknown as ParticipantDto;
}
