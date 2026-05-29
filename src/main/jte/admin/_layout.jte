@import org.huebert.ncbot.config.NcbotProperties
@import java.util.List
@import java.util.Objects
@import org.huebert.ncbot.entity.ChatChannel

@param String title
@param List<ChatChannel> publicChannels
@param List<ChatChannel> dmChannels
@param Long channelId = null
@param String channelName = ""

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ncbot - ${title}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="/css/admin.css">
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.4/dist/htmx.min.js"></script>
</head>
<body class="bg-dark text-light">
    <!-- Navbar -->
    <nav class="navbar navbar-dark bg-dark border-bottom border-secondary">
        <div class="container-fluid">
            <button class="btn btn-sm btn-outline-light me-2 d-md-none" id="sidebar-toggle" aria-label="Toggle sidebar">
                <i class="bi bi-list"></i>
            </button>
            <span class="navbar-brand mb-0 h1">ncbot - ${title}</span>
        </div>
    </nav>

    <div class="container-fluid g-0">
        <div class="row g-0" style="min-height: calc(100vh - 56px);">
            <!-- Sidebar -->
            <aside class="col-md-3 col-lg-2 d-md-block sidebar collapse" id="sidebar">
                <div class="position-sticky pt-3">
                    <div class="list-group list-group-flush">
                        <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">Public</h6>
                        @for(ChatChannel ch : publicChannels)
                            @template.admin._channel_item(channel = ch, tab = "messages", active = Objects.equals(ch.getId(), channelId))
                        @endfor

                        <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">DMs</h6>
                        @for(ChatChannel ch : dmChannels)
                            @template.admin._channel_item(channel = ch, tab = "messages", active = Objects.equals(ch.getId(), channelId))
                        @endfor

                        <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">Tools</h6>
                        <div class="list-group-item list-group-item-action channel-link"
                             data-channel-id="global"
                             data-tab="memory"
                             hx-get="/admin/memory/global"
                             hx-target="#tab-memory"
                             hx-swap="innerHTML">
                            <i class="bi bi-journal-text"></i> Global Memory
                        </div>
                        <div class="list-group-item list-group-item-action channel-link"
                             data-channel-id="participants"
                             data-tab="participants"
                             hx-get="/admin/participants/null"
                             hx-target="#tab-participants"
                             hx-swap="innerHTML">
                            <i class="bi bi-people"></i> Participants
                        </div>
                    </div>
                </div>
            </aside>

            <!-- Main content -->
            <main class="col-md-9 col-lg-10 ms-sm-auto px-0">
                <!-- Tab bar -->
                <ul class="nav nav-tabs nav-justified border-bottom border-secondary" id="mainTabs" role="tablist">
                    <li class="nav-item">
                        <button class="nav-link nav-link-tab active" data-tab="messages" type="button">
                            <i class="bi bi-chat-dots"></i> Messages
                        </button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link nav-link-tab" data-tab="memory" type="button">
                            <i class="bi bi-journal-text"></i> Memory
                        </button>
                    </li>
                    <li class="nav-item">
                        <button class="nav-link nav-link-tab" data-tab="participants" type="button">
                            <i class="bi bi-people"></i> Participants
                        </button>
                    </li>
                </ul>

                <!-- Tab content -->
                <div class="tab-content" id="mainTabContent">
                    <div class="tab-pane fade show active" id="tab-messages" role="tabpanel">
                        <!-- Messages loaded here -->
                    </div>
                    <div class="tab-pane fade" id="tab-memory" role="tabpanel">
                        <!-- Memory loaded here -->
                    </div>
                    <div class="tab-pane fade" id="tab-participants" role="tabpanel">
                        <!-- Participants loaded here -->
                    </div>
                </div>
            </main>
        </div>
    </div>

    <script>
        // Sidebar toggle (mobile)
        document.getElementById('sidebar-toggle')?.addEventListener('click', function() {
            const sidebar = document.getElementById('sidebar');
            sidebar.classList.toggle('show');
        });

        // Highlight active channel in sidebar
        function setActiveChannel(channelId) {
            document.querySelectorAll('.channel-link').forEach(item => {
                item.classList.toggle('active', item.getAttribute('data-channel-id') == channelId);
            });
        }

        // Scroll to bottom after HTMX swap
        document.body.addEventListener('htmx:afterSwap', (evt) => {
            const container = evt.target.querySelector('.message-container') || evt.target.closest('.message-container');
            if (container) {
                requestAnimationFrame(() => { container.scrollTop = container.scrollHeight; });
            }
            // Highlight the channel that was clicked
            const clicked = evt.detail?.elt;
            if (clicked) {
                const item = clicked.closest('.channel-link');
                if (item) {
                    setActiveChannel(item.getAttribute('data-channel-id'));
                }
            }
        });

        // Tab switching
        document.querySelectorAll('.nav-link-tab').forEach(btn => {
            btn.addEventListener('click', function() {
                document.querySelectorAll('.nav-link-tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                const tabId = 'tab-' + this.dataset.tab;
                document.querySelectorAll('.tab-pane').forEach(p => {
                    p.classList.remove('show', 'active');
                });
                document.getElementById(tabId)?.classList.add('show', 'active');
            });
        });

        // Memory edit functions
        window.startEdit = function(id) {
            const row = document.querySelector('#memory-' + id);
            if (!row) return;
            row.querySelector('.memory-view-row').style.display = 'none';
            row.querySelector('.memory-edit').style.display = 'block';
            row.querySelector('.value-input')?.focus();
        };

        window.cancelEdit = function(id) {
            const row = document.querySelector('#memory-' + id);
            if (!row) return;
            row.querySelector('.memory-view-row').style.display = 'flex';
            row.querySelector('.memory-edit').style.display = 'none';
        };

        // Infinite scroll for messages
        let isLoading = false;
        let hasMore = true;

        function checkScroll() {
            if (isLoading || !hasMore) return;
            const container = document.querySelector('.message-container');
            if (!container) return;
            if (container.scrollTop < 100) {
                isLoading = true;
                const beforeEl = container.querySelector('.message-row:last-child .timestamp');
                if (beforeEl) {
                    const before = beforeEl.getAttribute('data-timestamp');
                    const channelId = container.getAttribute('data-channel-id');
                    fetch('/admin/messages/' + channelId + '/older?before=' + encodeURIComponent(before))
                        .then(r => { if (!r.ok) { hasMore = false; return null; } return r.text(); })
                        .then(html => {
                            if (html) container.insertAdjacentHTML('beforeend', html);
                            else hasMore = false;
                        })
                        .finally(() => { isLoading = false; });
                }
            }
        }

        function attachScrollListener() {
            // Remove old listener to avoid duplicates
            document.removeEventListener('scroll', checkScroll);
            const container = document.querySelector('.message-container');
            if (container) {
                container.addEventListener('scroll', checkScroll, { passive: true });
            }
        }

        // Attach on load and after HTMX swaps
        attachScrollListener();
        document.body.addEventListener('htmx:afterSwap', attachScrollListener);
    </script>
</body>
</html>
