import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Trash2, ChevronRight, Users, Hash } from "lucide-react";
import { useChannels, useDeleteChannel } from "@/api/queries";
import { useBooleanParam, usePageParam, useSearchQuery } from "@/lib/url-state";
import { PageState } from "@/components/page-state";
import { Pagination } from "@/components/pagination";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { SearchInput } from "@/components/search-input";
import { Badge, Table, THead, Th, Td } from "@/components/ui/base";
import { formatTimestamp, formatRelativeTime } from "@/lib/format";

const PAGE_SIZE = 25;

export function ChannelsPage() {
  const navigate = useNavigate();
  const [page, setPage] = usePageParam("page", 0);
  const [dmFilter, setDmFilter] = useBooleanParam("dm");
  const [query, setQuery] = useSearchQuery();

  const { data, isLoading, error, refetch } = useChannels({
    page,
    size: PAGE_SIZE,
    ...(dmFilter !== undefined ? { dm: dmFilter } : {}),
    ...(query ? { query } : {}),
  });

  const deleteMutation = useDeleteChannel();
  const [deleteTarget, setDeleteTarget] = useState<{
    id: number;
    name: string;
  } | null>(null);

  const channels = data?.content ?? [];
  const isEmpty = !isLoading && !error && channels.length === 0;

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-100">Channels</h1>

        <div className="flex items-center gap-2">
          <SearchInput
            value={query}
            onChange={setQuery}
            placeholder="Search name or key…"
            ariaLabel="Search channels"
          />
          <span className="text-sm text-gray-500">Filter:</span>
          <select
            value={dmFilter === undefined ? "" : String(dmFilter)}
            onChange={(e) => {
              const val = e.target.value;
              setDmFilter(val === "" ? undefined : val === "true");
            }}
            className="rounded-md border border-gray-700 bg-gray-900 px-2.5 py-1.5 text-sm text-gray-100"
            aria-label="Filter by DM status"
          >
            <option value="">All channels</option>
            <option value="false">Regular channels</option>
            <option value="true">Direct messages</option>
          </select>
        </div>
      </div>

      <div className="mt-4">
        <PageState
          isLoading={isLoading}
          error={error}
          isEmpty={isEmpty}
          emptyText="No channels found."
          onRetry={() => refetch()}
        >
          <Table>
            <THead>
              <Th>Name</Th>
              <Th>Key</Th>
              <Th>Type</Th>
              <Th>Last Message</Th>
              <Th>Last Activity</Th>
              <Th className="w-20">Actions</Th>
            </THead>
            <tbody className="divide-y divide-gray-800">
              {channels.map((ch) => (
                <tr
                  key={ch.id}
                  className="cursor-pointer hover:bg-gray-800/60"
                  onClick={() => navigate(`/channels/${ch.id}`)}
                >
                  <Td>
                    <div className="flex items-center gap-2">
                      {ch.isDm ? (
                        <Users className="h-4 w-4 text-gray-500" />
                      ) : (
                        <Hash className="h-4 w-4 text-gray-500" />
                      )}
                      <span className="font-medium text-gray-100">
                        {ch.channelName ?? "(unnamed)"}
                      </span>
                    </div>
                  </Td>
                  <Td className="font-mono text-xs text-gray-500">
                    {ch.channelKey}
                  </Td>
                  <Td>
                    <Badge variant={ch.isDm ? "default" : "success"}>
                      {ch.isDm ? "DM" : "Channel"}
                    </Badge>
                  </Td>
                  <Td className="text-xs whitespace-nowrap text-gray-300">
                    {formatTimestamp(ch.lastMessageAt)}
                  </Td>
                  <Td className="text-xs whitespace-nowrap text-gray-500">
                    {formatRelativeTime(ch.lastMessageAt)}
                  </Td>
                  <Td>
                    <div className="flex items-center gap-1">
                      <button
                        className="rounded p-1 text-gray-500 hover:bg-red-500/10 hover:text-red-400"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteTarget({
                            id: ch.id,
                            name: ch.channelName ?? ch.channelKey,
                          });
                        }}
                        aria-label={`Delete ${ch.channelName ?? ch.channelKey}`}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                      <ChevronRight className="h-4 w-4 text-gray-600" />
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        </PageState>
      </div>

      {data && (
        <Pagination
          currentPage={data.currentPage}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete Channel"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This will permanently remove the channel and all associated messages and memories.`}
        confirmLabel="Delete Channel"
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
    </div>
  );
}
