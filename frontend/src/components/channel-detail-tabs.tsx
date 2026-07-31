import { clsx } from "clsx";

const tabs = [
  { key: "messages", label: "Messages" },
  { key: "memory", label: "Memory" },
  { key: "participants", label: "Participants" },
] as const;

export type ChannelTab = (typeof tabs)[number]["key"];

interface ChannelDetailTabsProps {
  activeTab: ChannelTab;
  onChange: (tab: ChannelTab) => void;
}

export function ChannelDetailTabs({
  activeTab,
  onChange,
}: ChannelDetailTabsProps) {
  return (
    <div className="border-b border-gray-800" role="tablist">
      <nav className="-mb-px flex gap-0">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            onClick={() => onChange(tab.key)}
            className={clsx(
              "border-b-2 px-4 py-3 text-sm font-medium transition-colors",
              activeTab === tab.key
                ? "border-primary-400 text-primary-300"
                : "border-transparent text-gray-500 hover:border-gray-600 hover:text-gray-300",
            )}
          >
            {tab.label}
          </button>
        ))}
      </nav>
    </div>
  );
}