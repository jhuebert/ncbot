import { useState } from "react";
import { NavLink } from "react-router-dom";
import {
  MessageSquare,
  Brain,
  Users,
  Settings,
  Menu,
  X,
  Hash,
} from "lucide-react";
import { clsx } from "clsx";

const navItems = [
  { to: "/channels", label: "Channels", icon: Hash },
  { to: "/memory", label: "Global Memory", icon: Brain },
  { to: "/participants", label: "Participants", icon: Users },
  { to: "/settings", label: "Settings", icon: Settings },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="flex min-h-screen flex-col">
      {/* Skip link */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-primary-600 focus:px-4 focus:py-2 focus:text-white"
      >
        Skip to main content
      </a>

      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-gray-800 bg-gray-950/95 backdrop-blur">
        <div className="flex h-14 items-center justify-between px-4">
          <div className="flex items-center gap-3">
            <MessageSquare className="h-5 w-5 text-primary-400" />
            <span className="text-lg font-semibold text-gray-100">
              ncbot Admin
            </span>
          </div>

          {/* Desktop nav */}
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  clsx(
                    "flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary-500/10 text-primary-300"
                      : "text-gray-400 hover:bg-gray-800 hover:text-gray-100",
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* Mobile menu button */}
          <button
            className="rounded-md p-2 text-gray-400 hover:bg-gray-800 md:hidden"
            onClick={() => setMobileOpen(!mobileOpen)}
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
          >
            {mobileOpen ? (
              <X className="h-5 w-5" />
            ) : (
              <Menu className="h-5 w-5" />
            )}
          </button>
        </div>

        {/* Mobile nav */}
        {mobileOpen && (
          <nav className="border-t border-gray-800 bg-gray-950 px-4 pb-3 md:hidden" aria-label="Mobile">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  clsx(
                    "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary-500/10 text-primary-300"
                      : "text-gray-400 hover:bg-gray-800",
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      {/* Main content */}
      <main id="main-content" className="flex-1 p-4 md:p-6">
        {children}
      </main>
    </div>
  );
}