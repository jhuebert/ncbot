import type { ComponentType } from "react";
import { Navigate, createHashRouter } from "react-router-dom";
import { AppShell } from "./components/app-shell";
import { ChannelsPage } from "./pages/channels-page";
import { ChannelDetailPage } from "./pages/channel-detail-page";
import { GlobalMemoryPage } from "./pages/global-memory-page";
import { ParticipantsPage } from "./pages/participants-page";
import { SettingsPage } from "./pages/settings-page";
import { NotFoundPage } from "./pages/not-found-page";

function withShell(Page: ComponentType) {
  return (
    <AppShell>
      <Page />
    </AppShell>
  );
}

export const router = createHashRouter([
  {
    path: "/",
    element: <Navigate to="/channels" replace />,
  },
  {
    path: "/channels",
    element: withShell(ChannelsPage),
  },
  {
    path: "/channels/:channelId",
    element: withShell(ChannelDetailPage),
  },
  {
    path: "/memory",
    element: withShell(GlobalMemoryPage),
  },
  {
    path: "/participants",
    element: withShell(ParticipantsPage),
  },
  {
    path: "/settings",
    element: withShell(SettingsPage),
  },
  {
    path: "*",
    element: withShell(NotFoundPage),
  },
]);