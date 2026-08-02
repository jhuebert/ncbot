import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { ArrowLeft, Plus, Trash2, Edit3, ArrowUp } from "lucide-react";
import {
  useChannelMessages,
  useChannelMemory,
  useChannelParticipants,
  useDeleteChannelMemory,
  usePromoteMemory,
  useCreateChannelMemory,
  useUpdateChannelMemory,
  useUpdateParticipantBlocked,
} from "@/api/queries";
import { usePageParam, useSearchQuery, useStringParam } from "@/lib/url-state";
import {
  ChannelDetailTabs,
  type ChannelTab,
} from "@/components/channel-detail-tabs";
import { PageState } from "@/components/page-state";
import { Pagination } from "@/components/pagination";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { MemoryFormDialog } from "@/components/memory-form-dialog";
import type { MemoryFormValues } from "@/components/memory-form-dialog";
import { SearchInput } from "@/components/search-input";
import { Button, Table, THead, Th, Td, Badge } from "@/components/ui/base";
import { formatTimestamp, truncate } from "@/lib/format";
import { clsx } from "clsx";
import type { MessageDto, MemoryDto } from "@/api/admin";

const PAGE_SIZE = 25;
// Fewer messages per page so the chat fits on screen without an internal
// scrollbar; the page itself scrolls only in extreme cases.
const MESSAGE_PAGE_SIZE = 12;

// ── Chat helpers ──

interface ChatItem {
  key: string;
  kind: "in" | "out";
  sender: string | null;
  text: string;
  time: string;
}

/**
 * Flatten message rows into a chat stream. Each DB row is a received
 * message (`content`) that may carry an ncbot reply (`response`), which we
 * render as a "sent" bubble on the right.
 */
function toChatItems(messages: MessageDto[]): ChatItem[] {
  const items: ChatItem[] = [];
  for (const msg of messages) {
    items.push({
      key: `${msg.id}-in`,
      kind: "in",
      sender: msg.senderName,
      text: msg.content,
      time: msg.createdAt,
    });
    if (msg.response) {
      items.push({
        key: `${msg.id}-out`,
        kind: "out",
        sender: null,
        text: msg.response,
        time: msg.createdAt,
      });
    }
  }
  return items;
}

function formatChatTime(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  const sameDay = d.toDateString() === new Date().toDateString();
  return sameDay
    ? d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })
    : d.toLocaleString([], {
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
      });
}

export function ChannelDetailPage() {
  const { channelId } = useParams<{ channelId: string }>();
  const id = Number(channelId);
  const [tab, setTab] = useStringParam("tab", "messages");
  const isValidTab = (t: string): t is ChannelTab =>
    t === "messages" || t === "memory" || t === "participants";
  const currentTab: ChannelTab = isValidTab(tab) ? tab : "messages";

  if (isNaN(id)) {
    return (
      <div className="flex flex-col items-center gap-4 py-20">
        <p className="text-red-400">Invalid channel ID.</p>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => window.history.back()}
        >
          <ArrowLeft className="h-4 w-4" />
          Go back
        </Button>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => window.history.back()}
          aria-label="Back to channels"
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-bold text-gray-100">Channel #{id}</h1>
      </div>

      <ChannelDetailTabs activeTab={currentTab} onChange={(t) => setTab(t)} />

      <div className="mt-4">
        {currentTab === "messages" && <MessagesTab channelId={id} />}
        {currentTab === "memory" && <MemoryTab channelId={id} />}
        {currentTab === "participants" && <ParticipantsTab channelId={id} />}
      </div>
    </div>
  );
}

// ── Messages Tab (chat view) ──

function MessagesTab({ channelId }: { channelId: number }) {
  const [page, setPage] = usePageParam("page", 0);
  const [sortDirection, setSortDirection] = useStringParam(
    "sortDirection",
    "DESC",
  );
  const [before, setBefore] = useStringParam("before", "");
  const [after, setAfter] = useStringParam("after", "");

  const queryParams = {
    page,
    size: MESSAGE_PAGE_SIZE,
    sortDirection: sortDirection as "ASC" | "DESC",
    ...(before ? { before: before + ":00Z" } : {}),
    ...(after ? { after: after + ":00Z" } : {}),
  };

  const { data, isLoading, error, refetch } = useChannelMessages(
    channelId,
    queryParams,
  );

  const messages = data?.messages ?? [];
  const isEmpty = !isLoading && !error && messages.length === 0;

  // Keep the newest message at the bottom of the page, like a messaging app.
  const displayMessages = useMemo(
    () => (sortDirection === "DESC" ? [...messages].reverse() : messages),
    [messages, sortDirection],
  );
  const items = useMemo(() => toChatItems(displayMessages), [displayMessages]);

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-1.5 text-sm text-gray-500">
          Sort:
          <select
            value={sortDirection}
            onChange={(e) => {
              setSortDirection(e.target.value);
              setPage(0);
            }}
            className="rounded-md border border-gray-700 bg-gray-900 px-2 py-1 text-sm text-gray-100"
            aria-label="Sort direction"
          >
            <option value="DESC">Newest first</option>
            <option value="ASC">Oldest first</option>
          </select>
        </label>
        <label className="flex items-center gap-1.5 text-sm text-gray-500">
          Before:
          <input
            type="datetime-local"
            value={before}
            onChange={(e) => {
              setBefore(e.target.value);
              setPage(0);
            }}
            className="rounded-md border border-gray-700 bg-gray-900 px-2 py-1 text-sm text-gray-100"
          />
        </label>
        <label className="flex items-center gap-1.5 text-sm text-gray-500">
          After:
          <input
            type="datetime-local"
            value={after}
            onChange={(e) => {
              setAfter(e.target.value);
              setPage(0);
            }}
            className="rounded-md border border-gray-700 bg-gray-900 px-2 py-1 text-sm text-gray-100"
          />
        </label>
      </div>

      <PageState
        isLoading={isLoading}
        error={error}
        isEmpty={isEmpty}
        emptyText="No messages found."
        onRetry={() => refetch()}
      >
        {items.length > 0 && (
          <div className="mb-3 text-center text-xs text-gray-600">
            Left: received · Right: ncbot response
          </div>
        )}
        <div className="mx-auto max-w-3xl py-2">
          {items.map((item, i) => {
            const isSent = item.kind === "out";
            const isNewGroup =
              i === 0 ||
              items[i - 1].kind !== item.kind ||
              items[i - 1].sender !== item.sender;
            return (
              <div key={item.key}>
                {isNewGroup && (
                  <div
                    className={clsx(
                      "mt-4 flex px-1 pb-1 first:mt-0",
                      isSent ? "justify-end" : "justify-start",
                    )}
                  >
                    <span className="text-[11px] font-medium text-gray-500">
                      {isSent
                        ? formatChatTime(item.time)
                        : `${item.sender} · ${formatChatTime(item.time)}`}
                    </span>
                  </div>
                )}
                <div
                  className={clsx(
                    "flex",
                    isSent ? "justify-end" : "justify-start",
                  )}
                >
                  <div
                    className={clsx(
                      "max-w-[75%] px-3.5 py-2 text-sm leading-relaxed break-words whitespace-pre-wrap shadow-sm",
                      isSent
                        ? "rounded-2xl rounded-br-md bg-primary-600 text-white"
                        : "rounded-2xl rounded-bl-md bg-gray-800 text-gray-100",
                      isNewGroup ? "mt-1" : "mt-1.5",
                    )}
                  >
                    {item.text}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </PageState>

      {data && (
        <div className="mx-auto max-w-3xl">
          <Pagination
            currentPage={data.currentPage}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={MESSAGE_PAGE_SIZE}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
}

// ── Memory Tab ──

function MemoryTab({ channelId }: { channelId: number }) {
  const [page, setPage] = usePageParam("page", 0);
  const [query, setQuery] = useSearchQuery();
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MemoryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MemoryDto | null>(null);
  const [promoteTarget, setPromoteTarget] = useState<MemoryDto | null>(null);

  const { data, isLoading, error, refetch } = useChannelMemory(channelId, {
    page,
    size: PAGE_SIZE,
    ...(query ? { query } : {}),
  });

  const createMutation = useCreateChannelMemory(channelId);
  const updateMutation = useUpdateChannelMemory(channelId);
  const deleteMutation = useDeleteChannelMemory(channelId);
  const promoteMutation = usePromoteMemory(channelId);

  const memories = data?.content ?? [];
  const isEmpty = !isLoading && !error && memories.length === 0;

  const handleSubmit = (values: MemoryFormValues) => {
    if (editTarget) {
      updateMutation.mutate(
        { id: editTarget.id, body: values },
        { onSuccess: () => setFormOpen(false) },
      );
    } else {
      createMutation.mutate(values, { onSuccess: () => setFormOpen(false) });
    }
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-100">
          Channel Memories
        </h2>
        <div className="flex items-center gap-2">
          <SearchInput
            value={query}
            onChange={setQuery}
            placeholder="Search key or value…"
            ariaLabel="Search channel memories"
          />
          <Button
            size="sm"
            onClick={() => {
              setEditTarget(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" />
            Add Memory
          </Button>
        </div>
      </div>

      <PageState
        isLoading={isLoading}
        error={error}
        isEmpty={isEmpty}
        emptyText="No memories for this channel."
        onRetry={() => refetch()}
      >
        <Table>
          <THead>
            <Th>Key</Th>
            <Th>Value</Th>
            <Th className="w-28">Actions</Th>
          </THead>
          <tbody className="divide-y divide-gray-800">
            {memories.map((mem) => (
              <tr key={mem.id}>
                <Td className="font-mono text-xs font-medium text-gray-100">
                  {mem.key}
                </Td>
                <Td className="max-w-md">
                  <div className="text-sm break-words whitespace-pre-wrap text-gray-300">
                    {truncate(mem.value, 200)}
                  </div>
                </Td>
                <Td>
                  <div className="flex items-center gap-0.5">
                    <button
                      className="rounded p-1 text-gray-500 hover:bg-primary-500/10 hover:text-primary-300"
                      onClick={() => {
                        setEditTarget(mem);
                        setFormOpen(true);
                      }}
                      aria-label={`Edit memory ${mem.key}`}
                    >
                      <Edit3 className="h-3.5 w-3.5" />
                    </button>
                    <button
                      className="rounded p-1 text-gray-500 hover:bg-emerald-500/10 hover:text-emerald-400"
                      onClick={() => setPromoteTarget(mem)}
                      aria-label={`Promote ${mem.key} to global`}
                    >
                      <ArrowUp className="h-3.5 w-3.5" />
                    </button>
                    <button
                      className="rounded p-1 text-gray-500 hover:bg-red-500/10 hover:text-red-400"
                      onClick={() => setDeleteTarget(mem)}
                      aria-label={`Delete memory ${mem.key}`}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      </PageState>

      {data && (
        <Pagination
          currentPage={data.currentPage}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}

      <MemoryFormDialog
        open={formOpen}
        title={editTarget ? "Edit Memory" : "Create Memory"}
        defaultValues={
          editTarget
            ? { key: editTarget.key, value: editTarget.value }
            : undefined
        }
        loading={createMutation.isPending || updateMutation.isPending}
        onSubmit={handleSubmit}
        onCancel={() => setFormOpen(false)}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete Memory"
        message={`Are you sure you want to delete the memory "${deleteTarget?.key}"? This cannot be undone.`}
        confirmLabel="Delete Memory"
        variant="danger"
        loading={deleteMutation.isPending}
        onConfirm={() => {
          if (deleteTarget) {
            deleteMutation.mutate(deleteTarget.id, {
              onSuccess: () => setDeleteTarget(null),
            });
          }
        }}
        onCancel={() => setDeleteTarget(null)}
      />

      <ConfirmDialog
        open={promoteTarget !== null}
        title="Promote to Global"
        message={`Promote "${promoteTarget?.key}" to global scope? This creates a global copy and removes the channel-scoped original.`}
        confirmLabel="Promote"
        variant="primary"
        loading={promoteMutation.isPending}
        onConfirm={() => {
          if (promoteTarget) {
            promoteMutation.mutate(promoteTarget.id, {
              onSuccess: () => setPromoteTarget(null),
            });
          }
        }}
        onCancel={() => setPromoteTarget(null)}
      />
    </div>
  );
}

// ── Participants Tab ──

function ParticipantsTab({ channelId }: { channelId: number }) {
  const [page, setPage] = usePageParam("page", 0);
  const [query, setQuery] = useSearchQuery();

  const { data, isLoading, error, refetch } = useChannelParticipants(
    channelId,
    {
      page,
      size: PAGE_SIZE,
      ...(query ? { query } : {}),
    },
  );

  const updateBlocked = useUpdateParticipantBlocked();

  const participants = data?.content ?? [];
  const isEmpty = !isLoading && !error && participants.length === 0;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-100">
          Channel Participants
        </h2>
        <SearchInput
          value={query}
          onChange={setQuery}
          placeholder="Search name…"
          ariaLabel="Search channel participants"
        />
      </div>

      <PageState
        isLoading={isLoading}
        error={error}
        isEmpty={isEmpty}
        emptyText="No participants found for this channel."
        onRetry={() => refetch()}
      >
        <Table>
          <THead>
            <Th>Name</Th>
            <Th>Status</Th>
            <Th>First Seen</Th>
            <Th>Last Seen</Th>
            <Th>Last Notified</Th>
            <Th className="w-28">Actions</Th>
          </THead>
          <tbody className="divide-y divide-gray-800">
            {participants.map((p) => (
              <tr key={p.id}>
                <Td className="font-medium text-gray-100">{p.name}</Td>
                <Td>
                  {p.blocked ? (
                    <Badge variant="danger">Blocked</Badge>
                  ) : (
                    <Badge variant="success">Active</Badge>
                  )}
                </Td>
                <Td className="text-xs text-gray-500">
                  {formatTimestamp(p.firstSeen)}
                </Td>
                <Td className="text-xs text-gray-300">
                  {formatTimestamp(p.lastSeen)}
                </Td>
                <Td className="text-xs text-gray-500">
                  {formatTimestamp(p.pathUpgradeNotifiedAt)}
                </Td>
                <Td>
                  {p.blocked ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      disabled={updateBlocked.isPending}
                      onClick={() => {
                        updateBlocked.mutate({
                          participantId: p.id,
                          blocked: false,
                        });
                      }}
                    >
                      Unblock
                    </Button>
                  ) : (
                    <Button
                      variant="danger"
                      size="sm"
                      disabled={updateBlocked.isPending}
                      onClick={() => {
                        updateBlocked.mutate({
                          participantId: p.id,
                          blocked: true,
                        });
                      }}
                    >
                      Block
                    </Button>
                  )}
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      </PageState>

      {data && (
        <Pagination
          currentPage={data.currentPage}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
