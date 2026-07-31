import { useAllParticipants } from "@/api/queries";
import { usePageParam } from "@/lib/url-state";
import { PageState } from "@/components/page-state";
import { Pagination } from "@/components/pagination";
import { Table, THead, Th, Td } from "@/components/ui/base";
import { formatTimestamp, formatRelativeTime } from "@/lib/format";

const PAGE_SIZE = 25;

export function ParticipantsPage() {
  const [page, setPage] = usePageParam("page", 0);

  const { data, isLoading, error, refetch } = useAllParticipants({ page, size: PAGE_SIZE });

  const participants = data?.content ?? [];
  const isEmpty = !isLoading && !error && participants.length === 0;

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-100">Participants</h1>

      <div className="mt-4">
        <PageState
          isLoading={isLoading}
          error={error}
          isEmpty={isEmpty}
          emptyText="No participants found."
          onRetry={() => refetch()}
        >
          <Table>
            <THead>
              <Th>Name</Th>
              <Th>Last Seen</Th>
              <Th>Last Active</Th>
            </THead>
            <tbody className="divide-y divide-gray-800">
              {participants.map((p) => (
                <tr key={p.name}>
                  <Td className="font-medium text-gray-100">{p.name}</Td>
                  <Td className="whitespace-nowrap text-xs text-gray-300">
                    {formatTimestamp(p.lastSeen)}
                  </Td>
                  <Td className="whitespace-nowrap text-xs text-gray-500">
                    {formatRelativeTime(p.lastSeen)}
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
    </div>
  );
}