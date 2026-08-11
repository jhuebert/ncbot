import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchChannels,
  deleteChannel,
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
  fetchAllParticipants,
  updateParticipantBlocked,
  fetchConfigItems,
  updateConfigItem,
  resetConfigItem,
} from "./admin";
import { toast } from "sonner";

// ── Query Keys ──

export const queryKeys = {
  channels: {
    all: ["channels"] as const,
    list: (params?: {
      dm?: boolean;
      query?: string;
      page?: number;
      size?: number;
    }) => ["channels", "list", params] as const,
  },
  messages: {
    byChannel: (
      channelId: number,
      params?: {
        before?: string;
        after?: string;
        page?: number;
        size?: number;
        sortDirection?: "ASC" | "DESC";
      },
    ) => ["messages", channelId, params] as const,
  },
  channelMemory: {
    byChannel: (
      channelId: number,
      params?: { query?: string; page?: number; size?: number },
    ) => ["channelMemory", channelId, params] as const,
  },
  channelParticipants: {
    all: ["channelParticipants"] as const,
    byChannel: (
      channelId: number,
      params?: { query?: string; page?: number; size?: number },
    ) => ["channelParticipants", channelId, params] as const,
  },
  globalMemory: {
    all: ["globalMemory"] as const,
    list: (params?: { query?: string; page?: number; size?: number }) =>
      ["globalMemory", "list", params] as const,
  },
  participants: {
    all: ["participants"] as const,
    list: (params?: { query?: string; page?: number; size?: number }) =>
      ["participants", "list", params] as const,
  },
  config: {
    all: ["config"] as const,
  },
};

// ── Channels ──

export function useChannels(params?: {
  dm?: boolean;
  query?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: queryKeys.channels.list(params),
    queryFn: () => fetchChannels(params),
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

// ── Messages ──

export function useChannelMessages(
  channelId: number,
  params?: {
    before?: string;
    after?: string;
    page?: number;
    size?: number;
    sortDirection?: "ASC" | "DESC";
  },
) {
  return useQuery({
    queryKey: queryKeys.messages.byChannel(channelId, params),
    queryFn: () => fetchChannelMessages(channelId, params),
  });
}

// ── Channel Memory ──

export function useChannelMemory(
  channelId: number,
  params?: { query?: string; page?: number; size?: number },
) {
  return useQuery({
    queryKey: queryKeys.channelMemory.byChannel(channelId, params),
    queryFn: () => fetchChannelMemory(channelId, params),
  });
}

export function useCreateChannelMemory(channelId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { key: string; value: string }) =>
      createChannelMemory(channelId, body),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: queryKeys.channelMemory.byChannel(channelId),
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
        queryKey: queryKeys.channelMemory.byChannel(channelId),
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
        queryKey: queryKeys.channelMemory.byChannel(channelId),
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
        queryKey: queryKeys.channelMemory.byChannel(channelId),
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
  params?: { query?: string; page?: number; size?: number },
) {
  return useQuery({
    queryKey: queryKeys.channelParticipants.byChannel(channelId, params),
    queryFn: () => fetchChannelParticipants(channelId, params),
  });
}

// ── Global Memory ──

export function useGlobalMemory(params?: {
  query?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: queryKeys.globalMemory.list(params),
    queryFn: () => fetchGlobalMemory(params),
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

// ── Participants ──

export function useAllParticipants(params?: {
  query?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: queryKeys.participants.list(params),
    queryFn: () => fetchAllParticipants(params),
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
