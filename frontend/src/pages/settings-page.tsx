import { useMemo, useState } from "react";
import { RotateCcw, Save, RefreshCw } from "lucide-react";
import {
  useConfigItems,
  useUpdateConfigItem,
  useResetConfigItem,
} from "@/api/queries";
import type { ConfigItemDto } from "@/api/admin";
import { PageState } from "@/components/page-state";
import {
  Button,
  Input,
  Select,
  Textarea,
  Badge,
  Spinner,
} from "@/components/ui/base";

const GROUP_TITLES: Record<string, string> = {
  bot: "Bot Identity & Prompts",
  chat: "Chat Behaviour",
  memory: "Memory System",
  channels: "Channel Routing",
  blocking: "Blocking & Filtering",
};

function groupOf(key: string): string {
  return key.split(".")[0];
}

function groupTitle(group: string): string {
  return GROUP_TITLES[group] ?? group;
}

export function ConfigItemRow({
  item,
  saving,
  onSave,
  onReset,
}: {
  item: ConfigItemDto;
  saving: boolean;
  onSave: (value: string) => void;
  onReset: () => void;
}) {
  const [draft, setDraft] = useState(item.value);
  const dirty = draft !== item.value;

  const renderControl = () => {
    switch (item.type) {
      case "BOOLEAN":
        return (
          <Select
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            aria-label={`Value for ${item.key}`}
          >
            <option value="true">true</option>
            <option value="false">false</option>
          </Select>
        );
      case "TEXT":
        return (
          <Textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={5}
            className="font-mono text-xs"
            aria-label={`Value for ${item.key}`}
          />
        );
      default:
        return (
          <Input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            className="font-mono text-xs"
            aria-label={`Value for ${item.key}`}
          />
        );
    }
  };

  return (
    <li className="py-4">
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-mono text-xs font-medium text-gray-100">
          {item.key}
        </span>
        {item.restartRequired && (
          <Badge variant="warning">Restart required</Badge>
        )}
        {!item.isDefault && <Badge>Custom</Badge>}
      </div>
      <p className="mt-1 text-sm text-gray-400">{item.description}</p>

      <div className="mt-2 space-y-2">
        {renderControl()}
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            disabled={!dirty || saving}
            onClick={() => onSave(draft)}
          >
            {saving ? (
              <Spinner className="h-3.5 w-3.5" />
            ) : (
              <Save className="h-3.5 w-3.5" />
            )}
            Save
          </Button>
          {!item.isDefault && (
            <Button
              size="sm"
              variant="secondary"
              disabled={saving}
              onClick={onReset}
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reset to default
            </Button>
          )}
        </div>
      </div>
    </li>
  );
}

export function SettingsPage() {
  const { data, isLoading, error, refetch } = useConfigItems();
  const updateMutation = useUpdateConfigItem();
  const resetMutation = useResetConfigItem();

  const groups = useMemo(() => {
    const map = new Map<string, ConfigItemDto[]>();
    for (const item of data ?? []) {
      const g = groupOf(item.key);
      if (!map.has(g)) map.set(g, []);
      map.get(g)!.push(item);
    }
    return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [data]);

  const handleSave = (item: ConfigItemDto, value: string) =>
    updateMutation.mutate({ key: item.key, value });

  const handleReset = (item: ConfigItemDto) => resetMutation.mutate(item.key);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-100">Settings</h1>
          <p className="mt-1 text-sm text-gray-400">
            Runtime configuration stored in the database. Changes apply
            immediately (unless marked as requiring a restart). Items not listed
            here — AI credentials, model, timeouts and datasource — remain in{" "}
            <code className="text-gray-300">application.yml</code>.
          </p>
        </div>
        <Button size="sm" variant="secondary" onClick={() => refetch()}>
          <RefreshCw className="h-4 w-4" />
          Refresh
        </Button>
      </div>

      <PageState
        isLoading={isLoading}
        error={error}
        isEmpty={false}
        emptyText="No configuration items."
        onRetry={() => refetch()}
      >
        {data &&
          groups.map(([group, items]) => (
            <section
              key={group}
              className="rounded-lg border border-gray-800 bg-gray-900"
            >
              <header className="border-b border-gray-800 px-4 py-3">
                <h2 className="text-sm font-semibold tracking-wide text-gray-300 uppercase">
                  {groupTitle(group)}
                </h2>
              </header>
              <ul className="divide-y divide-gray-800 px-4">
                {items.map((item) => (
                  <ConfigItemRow
                    key={`${item.key}:${item.value}`}
                    item={item}
                    saving={updateMutation.isPending}
                    onSave={(value) => handleSave(item, value)}
                    onReset={() => handleReset(item)}
                  />
                ))}
              </ul>
            </section>
          ))}
        <div className="rounded-lg border border-gray-800 bg-gray-900 px-4 py-3">
          <p className="text-xs text-gray-500">
            Adding a new configuration item is a one-line change to{" "}
            <code className="text-gray-400">ConfigItemDefinition</code> — the
            row is seeded automatically on startup and appears here without
            further wiring.
          </p>
        </div>
      </PageState>
    </div>
  );
}
