import { RotateCcw } from "lucide-react";
import { Table, THead, Th, Td } from "@/components/ui/base";
import { formatTimestamp } from "@/lib/format";
import type { MemoryFailureDto } from "@/api/admin";

interface MemoryFailuresTableProps {
  failures: MemoryFailureDto[];
  onRetry: (failure: MemoryFailureDto) => void;
  retryingId?: number | null;
  emptyText?: string;
}

function channelLabel(failure: MemoryFailureDto): string {
  return failure.channelName ?? `channel #${String(failure.channelId ?? "")}`;
}

function messageRange(failure: MemoryFailureDto): string {
  return failure.fromMessageId && failure.toMessageId
    ? `${String(failure.fromMessageId)}–${String(failure.toMessageId)}`
    : "—";
}

/**
 * Table of memory-synthesis partitions that were skipped after repeated AI
 * failures, with a per-row "Retry" action that rewinds the channel cursor so
 * the next scheduled run re-processes the batch.
 */
export function MemoryFailuresTable({
  failures,
  onRetry,
  retryingId,
  emptyText = "No skipped memory batches. 🎉",
}: MemoryFailuresTableProps) {
  if (failures.length === 0) {
    return (
      <div className="rounded-lg border border-gray-800 p-6 text-center text-sm text-gray-500">
        {emptyText}
      </div>
    );
  }

  return (
    <Table>
      <THead>
        <Th>Channel</Th>
        <Th>Messages</Th>
        <Th>Error</Th>
        <Th>When</Th>
        <Th className="w-24">Actions</Th>
      </THead>
      <tbody className="divide-y divide-gray-800">
        {failures.map((failure) => (
          <tr key={failure.id}>
            <Td className="font-mono text-xs font-medium text-gray-100">
              {channelLabel(failure)}
            </Td>
            <Td className="font-mono text-xs text-gray-400">
              {messageRange(failure)}
            </Td>
            <Td className="max-w-md">
              <div
                className="line-clamp-2 text-sm break-words whitespace-pre-wrap text-gray-300"
                title={failure.error ?? undefined}
              >
                {failure.error ?? "—"}
              </div>
            </Td>
            <Td className="text-sm whitespace-nowrap text-gray-400">
              {formatTimestamp(failure.createdAt)}
            </Td>
            <Td>
              <button
                className="rounded p-1 text-gray-500 hover:bg-primary-500/10 hover:text-primary-300 disabled:opacity-50"
                onClick={() => {
                  onRetry(failure);
                }}
                disabled={retryingId === failure.id}
                aria-label={`Retry skipped memory batch for ${channelLabel(failure)}`}
              >
                <RotateCcw className="h-3.5 w-3.5" />
              </button>
            </Td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
