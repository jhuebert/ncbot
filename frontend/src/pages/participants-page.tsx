import { useAllParticipants, useUpdateParticipantBlocked } from "@/api/queries";
import { usePageParam, useSearchQuery } from "@/lib/url-state";
import { PageState } from "@/components/page-state";
import { Pagination } from "@/components/pagination";
import { SearchInput } from "@/components/search-input";
import { Table, THead, Th, Td, Badge, Button } from "@/components/ui/base";
import { formatTimestamp, formatRelativeTime } from "@/lib/format";

const PAGE_SIZE = 25;

export function ParticipantsPage() {
  const [page, setPage] = usePageParam("page", 0);
  const [query, setQuery] = useSearchQuery();

  const { data, isLoading, error, refetch } = useAllParticipants({
    page,
    size: PAGE_SIZE,
    ...(query ? { query } : {}),
  });

  const updateBlocked = useUpdateParticipantBlocked();

  const participants = data?.content ?? [];
  const isEmpty = !isLoading && !error && participants.length === 0;

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-100">Participants</h1>
        <SearchInput
          value={query}
          onChange={setQuery}
          placeholder="Search name…"
          ariaLabel="Search participants"
        />
      </div>

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
              <Th>Status</Th>
              <Th>First Seen</Th>
              <Th>Last Seen</Th>
              <Th>Last Active</Th>
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
                  <Td className="text-xs whitespace-nowrap text-gray-500">
                    {formatTimestamp(p.firstSeen)}
                  </Td>
                  <Td className="text-xs whitespace-nowrap text-gray-300">
                    {formatTimestamp(p.lastSeen)}
                  </Td>
                  <Td className="text-xs whitespace-nowrap text-gray-500">
                    {formatRelativeTime(p.lastSeen)}
                  </Td>
                  <Td className="text-xs whitespace-nowrap text-gray-500">
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
