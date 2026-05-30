// ── Admin SPA — vanilla JS, Bootstrap 5 ──

(() => {
    'use strict';

    // ── State ──
    const state = {
        selectedChannelId: null,
        selectedChannelKey: null,
        selectedChannelName: null,
        selectedIsDm: false,
        activeTab: 'messages',
        olderMessagesCursor: null, // ISO timestamp for infinite scroll
        autoRefreshInterval: null,
        editingMemoryId: null,
    };

    // ── API Client ──
    const api = {
        async get(url) {
            const res = await fetch(url);
            if (!res.ok) throw new Error(`API error ${res.status}: ${res.statusText}`);
            return res.json();
        },
        async post(url, body) {
            const res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });
            if (res.status === 204) return null;
            return res.json();
        },
        async put(url, body) {
            const res = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });
            return res.json();
        },
        async del(url) {
            const res = await fetch(url, { method: 'DELETE' });
            if (res.status === 204) return null;
            return res.json();
        },
    };

    // ── Utils ──
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function relativeTime(isoString) {
        const now = Date.now();
        const then = new Date(isoString).getTime();
        const diff = Math.floor((now - then) / 1000);

        if (diff < 0) return 'just now';
        if (diff < 60) return diff + 's ago';
        const minutes = Math.floor(diff / 60);
        if (minutes < 60) return minutes + 'm ago';
        const hours = Math.floor(minutes / 60);
        if (hours < 24) return hours + 'h ago';
        const days = Math.floor(hours / 24);
        if (days < 30) return days + 'd ago';
        const months = Math.floor(days / 30);
        return months + 'mo ago';
    }

    function formatFullTime(isoString) {
        const d = new Date(isoString);
        return d.toLocaleString();
    }

    function getBootstrapColor(varName) {
        return getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
    }

    // ── Sidebar ──
    function renderSidebar(channels) {
        const publicList = document.getElementById('channels-public');
        const dmList = document.getElementById('channels-dm');

        publicList.innerHTML = channels.publicChannels.map(ch =>
            `<div class="list-group-item" data-channel-id="${ch.id}" data-channel-key="${escapeHtml(ch.channelKey)}" data-channel-name="${escapeHtml(ch.channelName)}" data-is-dm="${ch.isDm}">
                <i class="bi bi-hash channel-type"></i>${escapeHtml(ch.channelName)}
            </div>`
        ).join('');

        dmList.innerHTML = channels.dmChannels.map(ch =>
            `<div class="list-group-item" data-channel-id="${ch.id}" data-channel-key="${escapeHtml(ch.channelKey)}" data-channel-name="${escapeHtml(ch.channelName)}" data-is-dm="${ch.isDm}">
                <i class="bi bi-person channel-type"></i>${escapeHtml(ch.channelName)}
            </div>`
        ).join('');

        // Attach click handlers
        publicList.querySelectorAll('.list-group-item').forEach(el => {
            el.addEventListener('click', () => selectChannel(el));
        });
        dmList.querySelectorAll('.list-group-item').forEach(el => {
            el.addEventListener('click', () => selectChannel(el));
        });
    }

    function selectChannel(el) {
        const id = parseInt(el.dataset.channelId);
        const key = el.dataset.channelKey;
        const name = el.dataset.channelName;
        const isDm = el.dataset.isDm === 'true';

        // Update active state
        document.querySelectorAll('.sidebar-list .list-group-item').forEach(item => {
            item.classList.remove('active');
        });
        el.classList.add('active');

        state.selectedChannelId = id;
        state.selectedChannelKey = key;
        state.selectedChannelName = name;
        state.selectedIsDm = isDm;
        state.olderMessagesCursor = null;

        // Update header
        document.getElementById('messages-channel-name').textContent = name;
        document.getElementById('messages-channel-key').textContent = `#${key}`;

        // Load messages
        loadMessages();

        // Switch to messages tab
        switchTab('messages');

        // Close sidebar on mobile
        if (window.innerWidth <= 768) {
            const sidebar = document.getElementById('sidebar');
            sidebar.classList.remove('show');
            // Also hide the Bootstrap toggle
            const toggleBtn = document.querySelector('[data-bs-target="#sidebar"]');
            if (toggleBtn) {
                toggleBtn.setAttribute('aria-expanded', 'false');
            }
        }
    }

    async function refreshChannels() {
        try {
            const channels = await api.get('/api/admin/channels');
            renderSidebar(channels);
        } catch (e) {
            console.error('Failed to load channels:', e);
        }
    }

    // ── Tab Switching ──
    function switchTab(tabName) {
        state.activeTab = tabName;

        // Update tab buttons
        document.querySelectorAll('#mainTabs .nav-link').forEach(btn => {
            btn.classList.remove('active');
        });
        document.getElementById(tabName + '-tab').classList.add('active');

        // Update tab panes
        document.querySelectorAll('.tab-content .tab-pane').forEach(pane => {
            pane.classList.remove('show', 'active');
        });
        document.getElementById(tabName).classList.add('show', 'active');

        // Load tab content
        if (tabName === 'messages') {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }

        if (tabName === 'memory') {
            loadMemory();
        }

        if (tabName === 'participants') {
            loadParticipants();
        }
    }

    // Attach tab click handlers
    document.querySelectorAll('#mainTabs .nav-link').forEach(btn => {
        btn.addEventListener('shown.bs.tab', (e) => {
            switchTab(e.target.id.replace('-tab', ''));
        });
    });

    // ── Messages Module ──
    function startAutoRefresh() {
        stopAutoRefresh();
        state.autoRefreshInterval = setInterval(() => {
            if (state.activeTab === 'messages' && state.selectedChannelId) {
                loadMessages();
            }
        }, 10000);
    }

    function stopAutoRefresh() {
        if (state.autoRefreshInterval) {
            clearInterval(state.autoRefreshInterval);
            state.autoRefreshInterval = null;
        }
    }

    async function loadMessages() {
        if (!state.selectedChannelId) return;

        const list = document.getElementById('messages-list');
        try {
            const data = await api.get(`/api/admin/messages/${state.selectedChannelId}`);
            renderMessages(data.messages, list);
            state.olderMessagesCursor = null;
        } catch (e) {
            console.error('Failed to load messages:', e);
            list.innerHTML = `<div class="empty-state">Failed to load messages.</div>`;
        }
    }

    async function loadOlderMessages() {
        if (!state.selectedChannelId) return;
        if (state.olderMessagesCursor === null) return;

        const spinner = document.getElementById('load-more-spinner');
        const text = document.getElementById('load-more-text');
        spinner.style.display = 'inline-block';
        text.style.display = 'none';

        try {
            const data = await api.get(`/api/admin/messages/${state.selectedChannelId}/older?before=${state.olderMessagesCursor}`);
            const list = document.getElementById('messages-list');

            if (data.messages.length === 0) {
                state.olderMessagesCursor = null;
                text.textContent = 'No more messages';
                text.style.display = 'inline';
                spinner.style.display = 'none';
                return;
            }

            // Prepend older messages (they go at the top visually due to column-reverse)
            const frag = document.createDocumentFragment();
            data.messages.forEach(msg => {
                frag.appendChild(createMessageElement(msg));
            });
            list.insertBefore(frag, list.firstChild);

            // Update cursor
            const oldestMsg = data.messages[data.messages.length - 1];
            state.olderMessagesCursor = oldestMsg.createdAt;
        } catch (e) {
            console.error('Failed to load older messages:', e);
        } finally {
            spinner.style.display = 'none';
            text.style.display = 'inline';
        }
    }

    function renderMessages(messages, container) {
        container.innerHTML = '';
        if (messages.length === 0) {
            container.innerHTML = `<div class="empty-state"><i class="bi bi-inbox" style="font-size: 2rem;"></i><p class="mt-2">No messages yet.</p></div>`;
            return;
        }

        const frag = document.createDocumentFragment();
        messages.forEach(msg => {
            frag.appendChild(createMessageElement(msg));
        });
        container.appendChild(frag);

        // Set oldest message cursor
        const oldestMsg = messages[messages.length - 1];
        state.olderMessagesCursor = oldestMsg.createdAt;
    }

    function createMessageElement(msg) {
        const isBot = msg.senderName.toLowerCase() === 'ncbot';
        const row = document.createElement('div');
        row.className = `message-row ${isBot ? 'bot' : 'user'}`;

        row.innerHTML = `
            <div class="message-meta">
                <span class="message-sender">${escapeHtml(msg.senderName)}</span>
                <span class="message-time" title="${formatFullTime(msg.createdAt)}">${relativeTime(msg.createdAt)}</span>
            </div>
            <p class="message-text">${escapeHtml(msg.content)}</p>
        `;

        return row;
    }

    // Infinite scroll — scroll to top loads older
    document.getElementById('messages-list').addEventListener('scroll', (e) => {
        const el = e.target;
        // When scrolled to within 50px of top, load older
        if (el.scrollTop <= 50 && state.olderMessagesCursor) {
            loadOlderMessages();
        }
    });

    // ── Memory Module ──
    async function loadMemory() {
        if (!state.selectedChannelId) {
            // Show global only when no channel selected
            loadGlobalMemory();
            return;
        }

        try {
            const data = await api.get(`/api/admin/memory?channelId=${state.selectedChannelId}`);
            renderMemoryList(data.memories, 'memory-channel-list', state.selectedChannelId);
        } catch (e) {
            console.error('Failed to load memory:', e);
        }

        loadGlobalMemory();
    }

    async function loadGlobalMemory() {
        try {
            const data = await api.get('/api/admin/memory/global');
            renderMemoryList(data.memories, 'memory-global-list', null);
        } catch (e) {
            console.error('Failed to load global memory:', e);
        }
    }

    function renderMemoryList(memories, listId, channelId) {
        const list = document.getElementById(listId);

        if (memories.length === 0) {
            list.innerHTML = `<div class="empty-state" style="padding: 1.5rem;"><small>No memories</small></div>`;
            return;
        }

        list.innerHTML = memories.map(m => {
            const isGlobal = m.channelId === null;
            return `<div class="memory-card" data-memory-id="${m.id}">
                <div class="memory-card-row">
                    <span class="memory-key" title="Click to edit" onclick="window._editMemory(${m.id})">${escapeHtml(m.key)}</span>
                    <span class="memory-value" title="${escapeHtml(m.value)}">${escapeHtml(m.value)}</span>
                    <span class="memory-actions">
                        ${!isGlobal ? `<button class="btn btn-sm btn-outline-secondary" title="Promote to global" onclick="window._promoteMemory(${m.id})"><i class="bi bi-arrow-up-circle"></i></button>` : ''}
                        <button class="btn btn-sm btn-outline-secondary" title="Edit" onclick="window._editMemory(${m.id})"><i class="bi bi-pencil"></i></button>
                        <button class="btn btn-sm btn-outline-danger" title="Delete" onclick="window._deleteMemory(${m.id})"><i class="bi bi-trash"></i></button>
                    </span>
                </div>
            </div>`;
        }).join('');
    }

    // ── Memory Modal ──
    const memoryModal = new bootstrap.Modal(document.getElementById('memoryModal'));

    function showAddMemoryForm(channelId) {
        state.editingMemoryId = null;
        document.getElementById('memoryModalLabel').textContent = 'Add Memory';
        document.getElementById('memoryFormId').value = '';
        document.getElementById('memoryFormChannelId').value = channelId !== null ? channelId : '';
        document.getElementById('memoryFormKey').value = '';
        document.getElementById('memoryFormValue').value = '';
        memoryModal.show();
    }

    window._editMemory = function(id) {
        // Find the memory in the DOM
        const card = document.querySelector(`.memory-card[data-memory-id="${id}"]`);
        if (!card) return;

        const keyEl = card.querySelector('.memory-key');
        const valueEl = card.querySelector('.memory-value');
        if (!keyEl || !valueEl) return;

        state.editingMemoryId = id;
        document.getElementById('memoryModalLabel').textContent = 'Edit Memory';
        document.getElementById('memoryFormId').value = id;
        document.getElementById('memoryFormKey').value = keyEl.textContent;
        document.getElementById('memoryFormValue').value = valueEl.textContent;

        // Determine channel ID from the parent section
        const section = card.closest('.tab-pane') || card.closest('.memory-view');
        if (section) {
            const channelSection = section.querySelector('#memory-channel-section');
            if (channelSection && channelSection.contains(card)) {
                document.getElementById('memoryFormChannelId').value = state.selectedChannelId || '';
            } else {
                document.getElementById('memoryFormChannelId').value = '';
            }
        }

        memoryModal.show();
    };

    window.saveMemoryFromModal = async function() {
        const id = document.getElementById('memoryFormId').value;
        const channelId = document.getElementById('memoryFormChannelId').value;
        const key = document.getElementById('memoryFormKey').value.trim();
        const value = document.getElementById('memoryFormValue').value.trim();

        if (!key || !value) return;

        try {
            if (id) {
                // Update existing
                await api.put(`/api/admin/memory/${id}`, { key, value });
            } else {
                // Create new
                await api.post('/api/admin/memory', {
                    channelId: channelId ? parseInt(channelId) : null,
                    key,
                    value,
                });
            }
            memoryModal.hide();
            loadMemory();
        } catch (e) {
            console.error('Failed to save memory:', e);
            alert('Failed to save memory: ' + e.message);
        }
    }

    window._deleteMemory = async function(id) {
        if (!confirm('Delete this memory?')) return;

        try {
            await api.del(`/api/admin/memory/${id}`);
            loadMemory();
        } catch (e) {
            console.error('Failed to delete memory:', e);
            alert('Failed to delete memory: ' + e.message);
        }
    };

    window._promoteMemory = async function(id) {
        try {
            await api.post('/api/admin/memory/promote', { memoryId: id });
            loadMemory();
        } catch (e) {
            console.error('Failed to promote memory:', e);
            alert('Failed to promote memory: ' + e.message);
        }
    };

    // ── Participants Module ──
    async function loadParticipants() {
        if (state.selectedChannelId) {
            try {
                const data = await api.get(`/api/admin/participants?channelId=${state.selectedChannelId}`);
                renderParticipants(data.senders, 'participants-channel-list');
            } catch (e) {
                console.error('Failed to load participants:', e);
            }
        }

        try {
            const data = await api.get('/api/admin/participants/all');
            renderParticipants(data.senders, 'participants-all-list');
        } catch (e) {
            console.error('Failed to load all participants:', e);
        }
    }

    function renderParticipants(senders, listId) {
        const list = document.getElementById(listId);

        if (senders.length === 0) {
            list.innerHTML = `<div class="empty-state" style="padding: 1.5rem;"><small>No participants</small></div>`;
            return;
        }

        list.innerHTML = senders.map(p => {
            const lastSeenHtml = p.lastSeen
                ? `<span class="participant-last-seen" title="${formatFullTime(p.lastSeen)}">${relativeTime(p.lastSeen)}</span>`
                : '';

            return `<div class="list-group-item">
                <div class="d-flex align-items-center justify-content-between">
                    <span class="participant-name"><i class="bi bi-person"></i> ${escapeHtml(p.name)}</span>
                    ${lastSeenHtml}
                </div>
            </div>`;
        }).join('');
    }

    // ── Info Modal ──
    window.showInfo = async function() {
        try {
            const data = await api.get('/api/admin/info');
            const details = document.getElementById('infoDetails');
            details.innerHTML = `
                <dt class="col-5">Uptime</dt>
                <dd class="col-7">${Math.floor(data.uptimeSeconds / 3600)}h ${Math.floor((data.uptimeSeconds % 3600) / 60)}m</dd>
                <dt class="col-5">Total Messages</dt>
                <dd class="col-7">${data.totalMessages.toLocaleString()}</dd>
                <dt class="col-5">Total Channels</dt>
                <dd class="col-7">${data.totalChannels}</dd>
                <dt class="col-5">Total Memories</dt>
                <dd class="col-7">${data.totalMemories}</dd>
            `;
            new bootstrap.Modal(document.getElementById('infoModal')).show();
        } catch (e) {
            console.error('Failed to load info:', e);
        }
    };

    // ── Mobile sidebar toggle ──
    document.querySelector('[data-bs-target="#sidebar"]').addEventListener('click', () => {
        const sidebar = document.getElementById('sidebar');
        const isShowing = sidebar.classList.contains('show');
        sidebar.classList.toggle('show');
        const toggleBtn = document.querySelector('[data-bs-target="#sidebar"]');
        toggleBtn.setAttribute('aria-expanded', String(!isShowing));
    });

    // ── Initialize ──
    async function init() {
        await refreshChannels();
    }

    init();
})();
