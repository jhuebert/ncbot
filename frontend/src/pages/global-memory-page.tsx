import { useState } from "react";
import { Plus, Trash2, Edit3 } from "lucide-react";
import {
  useGlobalMemory,
  useCreateGlobalMemory,
  useUpdateGlobalMemory,
  useDeleteGlobalMemory,
} from "@/api/queries";
import { usePageParam } from "@/lib/url-state";
import { PageState } from "@/components/page-state";
import { Pagination } from "@/components/pagination";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { MemoryFormDialog } from "@/components/memory-form-dialog";
import type { MemoryFormValues } from "@/components/memory-form-dialog";
import { Button, Table, THead, Th, Td } from "@/components/ui/base";
import { truncate } from "@/lib/format";
import type { MemoryDto } from "@/api/admin";

const PAGE_SIZE = 25;

export function GlobalMemoryPage() {
  const [page, setPage] = usePageParam("page", 0);
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MemoryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MemoryDto | null>(null);

  const { data, isLoading, error, refetch } = useGlobalMemory({ page, size: PAGE_SIZE });

  const createMutation = useCreateGlobalMemory();
  const updateMutation = useUpdateGlobalMemory();
  const deleteMutation = useDeleteGlobalMemory();

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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-100">Global Memory</h1>
        <Button
          size="sm"
          onClick={() => { setEditTarget(null); setFormOpen(true); }}
        >
          <Plus className="h-4 w-4" />
          Add Memory
        </Button>
      </div>

      <div className="mt-4">
        <PageState
          isLoading={isLoading}
          error={error}
          isEmpty={isEmpty}
          emptyText="No global memories yet."
          onRetry={() => refetch()}
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
                    <div className="whitespace-pre-wrap break-words text-sm text-gray-300">
                      {truncate(mem.value, 300)}
                    </div>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-0.5">
                      <button
                        className="rounded p-1 text-gray-500 hover:bg-primary-500/10 hover:text-primary-300"
                        onClick={() => { setEditTarget(mem); setFormOpen(true); }}
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

      <MemoryFormDialog
        open={formOpen}
        title={editTarget ? "Edit Global Memory" : "Create Global Memory"}
        defaultValues={editTarget ? { key: editTarget.key, value: editTarget.value } : undefined}
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
            deleteMutation.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
          }
        }}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}