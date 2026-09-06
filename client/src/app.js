class NodePanel {
    /**
     * @param {string} id         - 'a' or 'b'
     * @param {string} label      - 'Node A' or 'Node B'
     * @param {string} prefix     - '/node-a' or '/node-b'
     */
    constructor(id, label, prefix) {
        this.id = id;
        this.label = label;
        this.prefix = prefix;
        this.clientId = crypto.randomUUID();
        this.eventSource = null;

        this._build();
    }

    _build() {
        const container = document.getElementById(`panel-${this.id}`);

        container.innerHTML = `
            <div class="panel-header">
                <span class="status-dot" id="dot-${this.id}"></span>
                <span class="panel-title">${this.label}</span>
                <span class="status-label" id="status-${this.id}">connecting…</span>
            </div>
            <div class="messages" id="messages-${this.id}">
                <div class="empty-hint" id="hint-${this.id}">No messages yet</div>
            </div>
            <div class="input-row">
                <input id="input-${this.id}" type="text" placeholder="Type a message…" />
                <button id="send-${this.id}" disabled>Send</button>
            </div>
        `;

        this._dot = document.getElementById(`dot-${this.id}`);
        this._statusLabel = document.getElementById(`status-${this.id}`);
        this._feed = document.getElementById(`messages-${this.id}`);
        this._hint = document.getElementById(`hint-${this.id}`);
        this._input = document.getElementById(`input-${this.id}`);
        this._sendBtn = document.getElementById(`send-${this.id}`);

        this._input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') this._send();
        });
        this._sendBtn.addEventListener('click', () => this._send());
    }

    connect() {
        this.eventSource?.close();
        const url = `${this.prefix}/register/${this.clientId}`;
        this.eventSource = new EventSource(url);

        this.eventSource.addEventListener('open', () => this._setConnected());
        this.eventSource.addEventListener('error', () => this._setDisconnected());

        this.eventSource.addEventListener('chat', (e) => {
            try {
                const data = JSON.parse(e.data);
                if (typeof data.text !== 'string' || typeof data.node !== 'string') {
                    throw new TypeError('Invalid chat event');
                }
                this._setConnected();
                this._appendMessage(data.text, data.node);
            }
            catch (error) {
                console.error('Ignoring invalid chat event', error);
            }
        });

        this._syncConnectionState();
    }

    disconnect() {
        this.eventSource?.close();
        this.eventSource = null;
        this._setDisconnected();
    }

    _syncConnectionState(attempt = 0) {
        if (!this.eventSource) {
            return;
        }

        if (this.eventSource.readyState === EventSource.OPEN) {
            this._setConnected();
            return;
        }

        if (this.eventSource.readyState === EventSource.CLOSED) {
            this._setDisconnected();
            return;
        }

        if (attempt < 10) {
            window.setTimeout(() => this._syncConnectionState(attempt + 1), 100);
        }
    }

    _setConnected() {
        this._dot.className = 'status-dot connected';
        this._statusLabel.textContent = 'connected';
        this._sendBtn.disabled = false;
    }

    _setDisconnected() {
        this._dot.className = 'status-dot error';
        this._statusLabel.textContent = 'disconnected';
        this._sendBtn.disabled = true;
    }

    _appendMessage(text, fromNode) {
        this._hint?.remove();
        this._hint = null;

        const isLocalOrigin = fromNode === this.label;
        const fromClass = fromNode.endsWith('A') ? 'from-a' : 'from-b';
        const remoteTag = !isLocalOrigin
            ? `<span class="remote-indicator">via Valkey</span>`
            : '';

        const msg = document.createElement('div');
        msg.className = `message ${fromClass}`;
        msg.innerHTML = `
            <div class="message-meta">${escapeHtml(fromNode)}${remoteTag}</div>
            <div class="message-text">${escapeHtml(text)}</div>
        `;
        this._feed.appendChild(msg);
        this._feed.scrollTop = this._feed.scrollHeight;
    }

    async _send() {
        const text = this._input.value.trim();
        if (!text) return;
        this._input.value = '';
        this._sendBtn.disabled = true;

        try {
            const response = await fetch(`${this.prefix}/send`, {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain; charset=UTF-8' },
                body: text
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
        }
        catch (error) {
            this._input.value = text;
            this._statusLabel.textContent = 'send failed';
            console.error('Unable to send message', error);
        }
        finally {
            this._sendBtn.disabled = this.eventSource?.readyState !== EventSource.OPEN;
        }
    }
}

function escapeHtml(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

export default class App {
    start() {
        this.nodeA = new NodePanel('a', 'Node A', '/node-a');
        this.nodeB = new NodePanel('b', 'Node B', '/node-b');
        this.nodeA.connect();
        this.nodeB.connect();
        window.addEventListener('pagehide', () => this.stop(), { once: true });
    }

    stop() {
        this.nodeA?.disconnect();
        this.nodeB?.disconnect();
    }
}
