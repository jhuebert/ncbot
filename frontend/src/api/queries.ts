import {
  useQuery,
  useMutation,
  useQueryClient,
  useInfiniteQuery,
} from "@tanstack/react-query";
import {
  fetchChannels,
  deleteChannel,
  fetchChannel,
  fetchChannelMessages,
  fetchChannelMemory,
  createChannelMemory,
  updateChannelMemory,
  deleteChannelMemory,
  promoteMemory,
  fetchChannelParticipants,
  fetchGlobalMemory,
  createGlobalMemory,
  updateGlobalMemory,
  deleteGlobalMemory,
  fetchMemoryFailures,
  fetchChannelMemoryFailures,
  retryMemoryFailure,
  fetchAllParticipants,
  updateParticipantBlocked,
  fetchConfigItems,
  updateConfigItem,
  resetConfigItem,
} from "./admin";
import { toast } from "sonner";

// ── Query Keys ──

// List / page size used for infinite-scroll lists.
const LIST_PAGE_SIZE = 25;
// Fewer messages per page so the chat fits on screen; scrolling up loads more.
const MESSAGE_PAGE_SIZE = 12;

/**
 * React Query resolves the next page number from the last server response.
 * The API reports the current page and total pages; when `currentPage + 1`
 * is still within range we request that page next.
 */
function nextPageParam<T extends { currentPage: number; totalPages: number }>(
  lastPage: T,
): number | undefined {
  return lastPage.currentPage + 1 < lastPage.totalPages
    ? lastPage.currentPage + 1
    : undefined;
}

export const queryKeys = {
  channels: {
    all: ["channels"] as const,
    list: (params?: { dm?: boolean; query?: string }) =>
      ["channels", "list", params] as const,
    byId: (channelId: number) => ["channels", channelId] as const,
  },
  messages: {
    byChannel: (
      channelId: number,
      params?: { before?: string; after?: string },
    ) => ["messages", channelId, params] as const,
  },
  channelMemory: {
    byChannel: (channelId: number, params?: { query?: string }) =>
      ["channelMemory", channelId, params] as const,
  },
  channelParticipants: {
    all: ["channelParticipants"] as const,
    byChannel: (channelId: number, params?: { query?: string }) =>
      ["channelParticipants", channelId, params] as const,
  },
  globalMemory: {
    all: ["globalMemory"] as const,
    list: (params?: { query?: string }) =>
      ["globalMemory", "list", params] as const,
  },
  memoryFailures: {
    all: ["memoryFailures"] as const,
    list: (params?: { page?: number; size?: number }) =>
      ["memoryFailures", "list", params] as const,
    byChannel: (channelId: number) => ["memoryFailures", channelId] as const,
  },
  participants: {
    all: ["participants"] as const,
    list: (params?: { query?: string }) =>
      ["participants", "list", params] as const,
  },
  config: {
    all: ["config"] as const,
  },
};

// ── Channels ──

export function useChannels(params?: { dm?: boolean; query?: string }) {
  return useInfiniteQuery({
    queryKey: queryKeys.channels.list(params),
    queryFn: ({ pageParam = 0 }) =>
      fetchChannels({ ...params, page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useDeleteChannel() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (channelId: number) => deleteChannel(channelId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.channels.all });
      toast.success("Channel deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useChannel(channelId: number) {
  return useQuery({
    queryKey: queryKeys.channels.byId(channelId),
    queryFn: () => fetchChannel(channelId),
  });
}

// ── Messages ──

/**
 * Channel messages are loaded newest-first (page 0 is the most recent batch)
 * and older pages are fetched as the user scrolls up. The UI reverses the
 * flattened result so the newest message sits at the bottom, chat-style.
 */
export function useChannelMessages(
  channelId: number,
  params?: { before?: string; after?: string },
) {
  return useInfiniteQuery({
    queryKey: queryKeys.messages.byChannel(channelId, params),
    queryFn: ({ pageParam = 0 }) =>
      fetchChannelMessages(channelId, {
        ...params,
        page: pageParam,
        size: MESSAGE_PAGE_SIZE,
        sortDirection: "DESC",
      }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

// ── Channel Memory ──

export function useChannelMemory(
  channelId: number,
  params?: { query?: string },
) {
  return useInfiniteQuery({
    queryKey: queryKeys.channelMemory.byChannel(channelId, params),
    queryFn: ({ pageParam = 0 }) =>
      fetchChannelMemory(channelId, {
        ...params,
        page: pageParam,
        size: LIST_PAGE_SIZE,
      }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useCreateChannelMemory(channelId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { key: string; value: string }) =>
      createChannelMemory(channelId, body),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["channelMemory", channelId],
      });
      toast.success("Memory created");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useUpdateChannelMemory(channelId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: number;
      body: { key: string; value: string };
    }) => updateChannelMemory(channelId, id, body),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["channelMemory", channelId],
      });
      toast.success("Memory updated");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useDeleteChannelMemory(channelId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteChannelMemory(channelId, id),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["channelMemory", channelId],
      });
      toast.success("Memory deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function usePromoteMemory(channelId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => promoteMemory(channelId, id),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["channelMemory", channelId],
      });
      qc.invalidateQueries({ queryKey: queryKeys.globalMemory.all });
      toast.success("Memory promoted to global");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

// ── Channel Participants ──

export function useChannelParticipants(
  channelId: number,
  params?: { query?: string },
) {
  return useInfiniteQuery({
    queryKey: queryKeys.channelParticipants.byChannel(channelId, params),
    queryFn: ({ pageParam = 0 }) =>
      fetchChannelParticipants(channelId, {
        ...params,
        page: pageParam,
        size: LIST_PAGE_SIZE,
      }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

// ── Global Memory ──

export function useGlobalMemory(params?: { query?: string }) {
  return useInfiniteQuery({
    queryKey: queryKeys.globalMemory.list(params),
    queryFn: ({ pageParam = 0 }) =>
      fetchGlobalMemory({ ...params, page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useCreateGlobalMemory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { key: string; value: string }) =>
      createGlobalMemory(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.globalMemory.all });
      toast.success("Global memory created");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useUpdateGlobalMemory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: number;
      body: { key: string; value: string };
    }) => updateGlobalMemory(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.globalMemory.all });
      toast.success("Global memory updated");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useDeleteGlobalMemory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteGlobalMemory(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.globalMemory.all });
      toast.success("Global memory deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

// ── Memory Synthesis Failures ──

export function useMemoryFailures() {
  return useInfiniteQuery({
    queryKey: queryKeys.memoryFailures.list(),
    queryFn: ({ pageParam = 0 }) =>
      fetchMemoryFailures({ page: pageParam, size: LIST_PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useChannelMemoryFailures(channelId: number) {
  return useQuery({
    queryKey: queryKeys.memoryFailures.byChannel(channelId),
    queryFn: () => fetchChannelMemoryFailures(channelId, { size: 25 }),
  });
}

export function useRetryMemoryFailure() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => retryMemoryFailure(id),
    onSuccess: () => {
      // Prefix-invalidates both the aggregate list and per-channel failure queries.
      qc.invalidateQueries({ queryKey: queryKeys.memoryFailures.all });
      toast.success("Memory batch queued for retry");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

// ── Participants ──

export function useAllParticipants(params?: { query?: string }) {
  return useInfiniteQuery({
    queryKey: queryKeys.participants.list(params),
    queryFn: ({ pageParam = 0 }) =>
      fetchAllParticipants({
        ...params,
        page: pageParam,
        size: LIST_PAGE_SIZE,
      }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useUpdateParticipantBlocked() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      participantId,
      blocked,
    }: {
      participantId: number;
      blocked: boolean;
    }) => updateParticipantBlocked(participantId, blocked),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.participants.all });
      qc.invalidateQueries({ queryKey: queryKeys.channelParticipants.all });
      toast.success("Participant updated");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

// ── Configuration items ──

export function useConfigItems() {
  return useQuery({
    queryKey: queryKeys.config.all,
    queryFn: fetchConfigItems,
  });
}

export function useUpdateConfigItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      updateConfigItem(key, value),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.config.all });
      toast.success("Configuration updated");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}

export function useResetConfigItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => resetConfigItem(key),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.config.all });
      toast.success("Configuration reset to default");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });
}
