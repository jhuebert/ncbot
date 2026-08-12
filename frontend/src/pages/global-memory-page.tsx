import { useState } from "react";
import { Plus, Trash2, Edit3 } from "lucide-react";
import {
  useGlobalMemory,
  useCreateGlobalMemory,
  useUpdateGlobalMemory,
  useDeleteGlobalMemory,
  useMemoryFailures,
  useRetryMemoryFailure,
} from "@/api/queries";
import { useSearchQuery } from "@/lib/url-state";
import { PageState } from "@/components/page-state";
import { InfiniteScroll } from "@/components/infinite-scroll";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { MemoryFormDialog } from "@/components/memory-form-dialog";
import { MemoryFailuresTable } from "@/components/memory-failures-table";
import type { MemoryFormValues } from "@/components/memory-form-dialog";
import { SearchInput } from "@/components/search-input";
import { Button, Table, THead, Th, Td } from "@/components/ui/base";
import { clsx } from "clsx";
import { truncate } from "@/lib/format";
import type { MemoryDto, MemoryFailureDto } from "@/api/admin";

type MemoryTab = "memories" | "failures";

export function GlobalMemoryPage() {
  const [query, setQuery] = useSearchQuery();
  const [activeTab, setActiveTab] = useState<MemoryTab>("memories");
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MemoryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MemoryDto | null>(null);

  const {
    data,
    isLoading,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useGlobalMemory(query ? { query } : {});

  const createMutation = useCreateGlobalMemory();
  const updateMutation = useUpdateGlobalMemory();
  const deleteMutation = useDeleteGlobalMemory();

  const failuresQuery = useMemoryFailures();
  const retryFailure = useRetryMemoryFailure();
  const failures = failuresQuery.data?.pages.flatMap((p) => p.content) ?? [];

  const memories = data?.pages.flatMap((p) => p.content) ?? [];
  const isEmpty = !isLoading && !error && memories.length === 0;

  const handleRetry = (failure: MemoryFailureDto) => {
    retryFailure.mutate(failure.id);
  };

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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-100">Global Memory</h1>
        {activeTab === "memories" && (
        <div className="flex items-center gap-2">
          <SearchInput
            value={query}
            onChange={setQuery}
            placeholder="Search key or value…"
            ariaLabel="Search global memory"
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
        )}
      </div>

      <div className="mt-4 flex items-center gap-4 border-b border-gray-800">
        {(["memories", "failures"] as const).map((tab) => (
          <button
            key={tab}
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
            className={clsx(
              "border-b-2 px-3 py-2 text-sm font-medium transition-colors",
              activeTab === tab
                ? "border-primary-400 text-primary-300"
                : "border-transparent text-gray-500 hover:border-gray-600 hover:text-gray-300",
            )}
          >
            {tab === "memories" ? "Memories" : "Failures"}
            {tab === "failures" && failures.length > 0 && (
              <span className="ml-1.5 rounded-full bg-red-500/15 px-1.5 py-0.5 text-xs font-semibold text-red-400">
                {failures.length}
              </span>
            )}
          </button>
        ))}
      </div>

      {activeTab === "memories" ? (
      <div className="mt-4">
        <PageState
          isLoading={isLoading}
          error={error}
          isEmpty={isEmpty}
          emptyText="No global memories yet."
          onRetry={() => refetch()}
        >
          <InfiniteScroll
            onLoadMore={() => fetchNextPage()}
            hasMore={hasNextPage}
            isLoadingMore={isFetchingNextPage}
            className="max-h-[70vh] rounded-lg border border-gray-800"
          >
          <Table>
            <THead>
              <Th>Key</Th>
              <Th>Value</Th>
              <Th className="w-24">Actions</Th>
            </THead>
            <tbody className="divide-y divide-gray-800">
              {memories.map((mem) => (
                <tr key={mem.id}>
                  <Td className="font-mono text-xs font-medium text-gray-100">
                    {mem.key}
                  </Td>
                  <Td className="max-w-lg">
                    <div className="text-sm break-words whitespace-pre-wrap text-gray-300">
                      {truncate(mem.value, 300)}
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
          </InfiniteScroll>
        </PageState>
      </div>
      ) : (
      <div className="mt-4">
        <PageState
          isLoading={failuresQuery.isLoading}
          error={failuresQuery.error}
          isEmpty={!failuresQuery.isLoading && !failuresQuery.error && failures.length === 0}
          emptyText="No skipped memory batches."
          onRetry={() => failuresQuery.refetch()}
        >
          <InfiniteScroll
            onLoadMore={() => failuresQuery.fetchNextPage()}
            hasMore={failuresQuery.hasNextPage}
            isLoadingMore={failuresQuery.isFetchingNextPage}
            className="max-h-[70vh] rounded-lg border border-gray-800"
          >
            <MemoryFailuresTable
              failures={failures}
              onRetry={handleRetry}
              retryingId={retryFailure.isPending ? retryFailure.variables : null}
            />
          </InfiniteScroll>
        </PageState>
      </div>
      )}

      <MemoryFormDialog
        open={formOpen}
        title={editTarget ? "Edit Global Memory" : "Create Global Memory"}
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
        title="Delete Global Memory"
        message={`Are you sure you want to delete the global memory "${deleteTarget?.key}"? This cannot be undone.`}
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
    </div>
  );
}
