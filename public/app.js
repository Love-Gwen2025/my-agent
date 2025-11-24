const loginForm = document.getElementById('login-form');
const authErrorEl = document.getElementById('auth-error');
const logoutBtn = document.getElementById('logout-btn');
const authView = document.getElementById('auth-view');
const workspaceView = document.getElementById('workspace');
const currentUserEl = document.getElementById('current-user');
const assistantNameEl = document.getElementById('assistant-name');
const conversationListEl = document.getElementById('conversation-list');
const conversationEmptyEl = document.getElementById('conversation-empty');
const newConvoBtn = document.getElementById('new-convo-btn');
const chatHistory = document.getElementById('chat-history');
const chatErrorEl = document.getElementById('chat-error');
const chatForm = document.getElementById('chat-form');
const messageInput = document.getElementById('message-input');
const fileInput = document.getElementById('file-input');
const filePreview = document.getElementById('file-preview');
const template = document.getElementById('message-template');
const themeToggleBtn = document.getElementById('theme-toggle');

const THEME_KEY = 'ai-assistant-theme';
const THEMES = ['blue', 'pink'];
const TEXT_CHUNK_SIZE = 6;

const tzRegex = /([zZ]|[+-]\d{2}:?\d{2})$/;

const normalizeTimestamp = (value) => {
  if (!value) return null;
  if (value instanceof Date) return value;
  const raw = typeof value === 'string' ? value.trim() : value;
  if (!raw) return null;
  const needsTimezone = typeof raw === 'string' && !tzRegex.test(raw);
  const normalized =
    typeof raw === 'string'
      ? `${raw.replace(' ', 'T')}${needsTimezone ? 'Z' : ''}`
      : raw;
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
};

const state = {
  accounts: [],
  user: null,
  assistantName: '小叶',
  conversations: [],
  activeConversationId: null,
  sending: false,
  typingBubble: null,
  theme: 'blue',
  stickToBottom: true,
};

const updateThemeToggleLabel = () => {
  if (!themeToggleBtn) return;
  themeToggleBtn.textContent = '点我试试';
};

const applyTheme = (theme) => {
  const next = THEMES.includes(theme) ? theme : 'blue';
  state.theme = next;
  document.documentElement.dataset.theme = next;
  try {
    localStorage.setItem(THEME_KEY, next);
  } catch (error) {
    console.warn('无法持久化主题设置', error);
  }
  updateThemeToggleLabel();
};

const initTheme = () => {
  let stored = null;
  try {
    stored = localStorage.getItem(THEME_KEY);
  } catch (error) {
    stored = null;
  }
  applyTheme(stored && THEMES.includes(stored) ? stored : 'blue');
};

initTheme();

themeToggleBtn?.addEventListener('click', () => {
  const nextTheme = state.theme === 'pink' ? 'blue' : 'pink';
  applyTheme(nextTheme);
});

const escapeHtml = (text = '') =>
  text.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));

const formatContent = (text = '') => {
  if (!text) return '';

  const codeBlocks = [];
  let working = text.replace(/```([\w+-]*)?\n?([\s\S]*?)```/g, (_match, lang = '', code = '') => {
    const langLabel = lang ? lang.trim() : '';
    const safeCode = escapeHtml(code.trim());
    const title = langLabel ? `<div class="msg-code-title">${langLabel}</div>` : '';
    codeBlocks.push(`${title}<pre class="msg-code-block"><code>${safeCode}</code></pre>`);
    return `~~~CODE_BLOCK_${codeBlocks.length - 1}~~~`;
  });

  working = escapeHtml(working);

  working = working
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
      (_match, label, url) =>
        `<a class="msg-tag msg-tag-link" href="${url}" target="_blank" rel="noopener noreferrer">${label}</a>`
    )
    .replace(/\[([^\]\n]+?)\](?!\()/g, (_match, label) => `<span class="msg-tag">${label}</span>`);

  const headings = [
    { regex: /^###\s+(.+)$/gm, className: 'msg-heading h3' },
    { regex: /^##\s+(.+)$/gm, className: 'msg-heading h2' },
    { regex: /^#\s+(.+)$/gm, className: 'msg-heading h1' },
  ];

  headings.forEach(({ regex, className }) => {
    working = working.replace(regex, (_, title) => `<div class="${className}">${title}</div>`);
  });

  working = working.replace(/\n/g, '<br />').replace(/(<\/div>)<br \/>/g, '$1');

  codeBlocks.forEach((block, index) => {
    const placeholder = new RegExp(`~~~CODE_BLOCK_${index}~~~`, 'g');
    working = working.replace(placeholder, block);
  });

  return working;
};

const formatTime = (timestamp) => {
  const date = normalizeTimestamp(timestamp);
  return date ? date.toLocaleString() : new Date().toLocaleTimeString();
};

const formatRelative = (timestamp) => {
  const date = normalizeTimestamp(timestamp);
  if (!date) return '--';
  const diff = Date.now() - date.getTime();
  if (diff < 0) return '刚刚';
  if (diff < 60 * 1000) return '刚刚';
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} 分钟前`;
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))} 小时前`;
  return date.toLocaleDateString();
};

const bytesToSize = (bytes) => {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** i).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

const api = async (url, options = {}) => {
  const config = {
    method: options.method || 'GET',
    credentials: 'same-origin',
    headers: { ...(options.headers || {}) },
  };

  if (options.body instanceof FormData) {
    config.body = options.body;
  } else if (options.body) {
    config.body = JSON.stringify(options.body);
    config.headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, config);
  const contentType = response.headers.get('content-type');
  const payload =
    contentType && contentType.includes('application/json') ? await response.json() : {};

  if (!response.ok) {
    throw new Error(payload.error || '请求失败');
  }

  return payload;
};

const toggleWorkspace = (authenticated) => {
  if (authenticated) {
    authView.classList.add('hidden');
    workspaceView.classList.remove('hidden');
    logoutBtn.classList.remove('hidden');
  } else {
    authView.classList.remove('hidden');
    workspaceView.classList.add('hidden');
    logoutBtn.classList.add('hidden');
  }
};

const showError = (element, message) => {
  if (!message) {
    element.classList.add('hidden');
    element.textContent = '';
    return;
  }
  element.textContent = message;
  element.classList.remove('hidden');
};

const updateAccountHints = (accounts) => {
  const hint = document.querySelector('.auth-card .hint');
  if (!hint || !accounts.length) return;
  const names = accounts.map((acc) => `${acc.displayName}（${acc.username}）`).join(' / ');
  hint.innerHTML = `目前支持：${names}，账号密码写在服务器 <code>.env</code> 中。`;
};

const renderConversationList = () => {
  conversationListEl.innerHTML = '';
  conversationEmptyEl.classList.toggle('hidden', state.conversations.length > 0);

  state.conversations.forEach((conversation) => {
    const li = document.createElement('li');

    const item = document.createElement('div');
    item.className = 'conversation-item';
    item.dataset.conversationId = conversation.id;
    if (conversation.id === state.activeConversationId) {
      item.classList.add('active');
    }

    const info = document.createElement('div');
    info.className = 'conversation-info';

    const titleEl = document.createElement('span');
    titleEl.className = 'title';
    titleEl.textContent = conversation.title;

    const timeEl = document.createElement('span');
    timeEl.className = 'time';
    timeEl.textContent = formatRelative(conversation.updated_at || conversation.created_at);

    info.append(titleEl, timeEl);

    const actions = document.createElement('div');
    actions.className = 'conversation-actions-inline';

    const renameBtn = document.createElement('button');
    renameBtn.type = 'button';
    renameBtn.className = 'conversation-action-btn';
    renameBtn.dataset.action = 'rename';
    renameBtn.dataset.conversationId = conversation.id;
    renameBtn.title = '重命名';
    renameBtn.textContent = '✏️';

    const deleteBtn = document.createElement('button');
    deleteBtn.type = 'button';
    deleteBtn.className = 'conversation-action-btn danger';
    deleteBtn.dataset.action = 'delete';
    deleteBtn.dataset.conversationId = conversation.id;
    deleteBtn.title = '删除';
    deleteBtn.textContent = '🗑️';

    actions.append(renameBtn, deleteBtn);
    item.append(info, actions);
    li.appendChild(item);
    conversationListEl.appendChild(li);
  });
};

const promptRenameConversation = async (conversationId) => {
  const current = state.conversations.find((item) => item.id === conversationId);
  const nextTitle = window.prompt('新的会话名称', current?.title || '');
  if (!nextTitle) return;
  const trimmed = nextTitle.trim();
  if (!trimmed) return;
  try {
    const { conversation } = await api(`/api/conversations/${conversationId}`, {
      method: 'PATCH',
      body: { title: trimmed },
    });
    state.conversations = state.conversations.map((item) =>
      item.id === conversation.id ? conversation : item
    );
    renderConversationList();
  } catch (error) {
    showError(chatErrorEl, error.message);
  }
};

const confirmDeleteConversation = async (conversationId) => {
  const confirmed = window.confirm('确定要删除该会话及其聊天记录吗？');
  if (!confirmed) return;
  const wasActive = state.activeConversationId === conversationId;
  try {
    await api(`/api/conversations/${conversationId}`, { method: 'DELETE' });
    state.conversations = state.conversations.filter((item) => item.id !== conversationId);
    if (wasActive) {
      state.activeConversationId = null;
    }
    await loadConversations({ keepCurrent: !wasActive });
  } catch (error) {
    showError(chatErrorEl, error.message);
  }
};

const clearHistoryView = ({ preserveStick = false } = {}) => {
  chatHistory.innerHTML = '';
  state.typingBubble = null;
  if (!preserveStick) {
    state.stickToBottom = true;
  }
};

const renderAttachment = (container, attachment) => {
  const wrapper = document.createElement('div');
  wrapper.className = 'attachment';

  if (attachment.url && attachment.mimeType?.startsWith('image/')) {
    const img = document.createElement('img');
    img.src = attachment.url;
    img.alt = attachment.fileName;
    wrapper.appendChild(img);
  } else if (attachment.url) {
    wrapper.innerHTML = `<a href="${attachment.url}" target="_blank" rel="noopener noreferrer">${attachment.fileName} · ${bytesToSize(attachment.size)}</a>`;
  } else {
    wrapper.textContent = `${attachment.fileName || '附件'} · ${bytesToSize(attachment.size)}`;
  }

  container.appendChild(wrapper);
};

const chunkTextNode = (text = '') => {
  if (!text) return [];
  if (text.length <= TEXT_CHUNK_SIZE) {
    return [document.createTextNode(text)];
  }
  const pieces = [];
  for (let i = 0; i < text.length; i += TEXT_CHUNK_SIZE) {
    pieces.push(document.createTextNode(text.slice(i, i + TEXT_CHUNK_SIZE)));
  }
  return pieces;
};

const expandAnimationNodes = (nodes) => {
  const expanded = [];
  nodes.forEach((node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      const text = node.textContent || '';
      expanded.push(...chunkTextNode(text));
    } else {
      expanded.push(node);
    }
  });
  return expanded;
};

const animateAssistantContent = (container, html) => {
  if (!container) return;
  const temp = document.createElement('div');
  temp.innerHTML = html;
  const nodes = expandAnimationNodes(Array.from(temp.childNodes));
  container.innerHTML = '';
  let index = 0;
  const revealNext = () => {
    if (index >= nodes.length) return;
    container.appendChild(nodes[index]);
    index += 1;
    if (state.stickToBottom) {
      chatHistory.scrollTop = chatHistory.scrollHeight;
    }
    if (index < nodes.length) {
      window.setTimeout(revealNext, 90);
    }
  };
  revealNext();
};

const renderBubble = ({ role, content, created_at: createdAt, attachments = [] }, options = {}) => {
  const { animate = false } = options;
  const node = template.content.firstElementChild.cloneNode(true);
  node.classList.add(role);
  node.querySelector('.role').textContent =
    role === 'assistant' ? state.assistantName : state.user?.displayName || '我';
  node.querySelector('.time').textContent = formatTime(createdAt);
  const htmlContent = formatContent(content || '(无文字)');
  const contentEl = node.querySelector('.content');
  if (animate && role === 'assistant') {
    animateAssistantContent(contentEl, htmlContent);
  } else {
    contentEl.innerHTML = htmlContent;
  }

  const attachmentBox = node.querySelector('.attachments');
  if (attachments.length) {
    attachmentBox.innerHTML = '';
    attachments.forEach((attachment) => renderAttachment(attachmentBox, attachment));
  } else {
    attachmentBox.remove();
  }

  chatHistory.appendChild(node);
  if (state.stickToBottom) {
    chatHistory.scrollTop = chatHistory.scrollHeight;
  }
  return node;
};

const showTypingBubble = () => {
  if (state.typingBubble) return;
  state.typingBubble = renderBubble({
    role: 'assistant',
    content: `${state.assistantName} 正在思考...`,
    created_at: new Date().toISOString(),
  });
  state.typingBubble.classList.add('typing');
  const timeEl = state.typingBubble.querySelector('.time');
  if (timeEl) timeEl.textContent = '正在思考';
};

const hideTypingBubble = () => {
  if (state.typingBubble) {
    state.typingBubble.remove();
    state.typingBubble = null;
  }
};

const updateFilePreview = () => {
  const files = Array.from(fileInput.files || []);
  if (!files.length) {
    filePreview.innerHTML = '';
    filePreview.classList.add('hidden');
    return;
  }
  filePreview.classList.remove('hidden');
  filePreview.innerHTML = '';
  files.forEach((file) => {
    const chip = document.createElement('div');
    chip.className = 'attachment';
    chip.textContent = `${file.name} · ${bytesToSize(file.size)}`;
    filePreview.appendChild(chip);
  });
};

const autoResizeTextarea = (element) => {
  if (!element) return;
  const minHeight = 44;
  const maxHeight = Math.max(window.innerHeight * 0.25, 120);
  element.style.height = 'auto';
  const nextHeight = Math.min(Math.max(element.scrollHeight, minHeight), maxHeight);
  element.style.height = `${nextHeight}px`;
};

const loadHistory = async (conversationId, { animateLatest = false } = {}) => {
  if (!conversationId) {
    clearHistoryView();
    return;
  }
  try {
    const { conversation, history } = await api(`/api/history?conversationId=${conversationId}`);
    clearHistoryView({ preserveStick: animateLatest });
    history.forEach((entry, index) => {
      const shouldAnimate =
        animateLatest && index === history.length - 1 && entry.role === 'assistant';
      renderBubble(entry, { animate: shouldAnimate });
    });
  } catch (error) {
    clearHistoryView();
    showError(chatErrorEl, error.message);
  }
};

const selectConversation = async (conversationId, { skipHistory = false } = {}) => {
  if (!conversationId) return;
  state.activeConversationId = conversationId;
  renderConversationList();
  if (!skipHistory) {
    await loadHistory(conversationId);
  }
};

const loadConversations = async ({ keepCurrent = true } = {}) => {
  try {
    const { conversations } = await api('/api/conversations');
    state.conversations = conversations;
    renderConversationList();

    if (!conversations.length) {
      state.activeConversationId = null;
      clearHistoryView();
      return;
    }

    const shouldKeep =
      keepCurrent &&
      state.activeConversationId &&
      conversations.some((item) => item.id === state.activeConversationId);

    if (shouldKeep) {
      await selectConversation(state.activeConversationId, { skipHistory: false });
    } else {
      await selectConversation(conversations[0].id);
    }
  } catch (error) {
    showError(chatErrorEl, error.message);
  }
};

const bootstrap = async () => {
  try {
    const session = await api('/api/session');
    state.assistantName = session.assistantName || state.assistantName;
    assistantNameEl.textContent = state.assistantName;
    messageInput.placeholder = `和${state.assistantName}聊点什么？`;

    state.accounts = session.accounts || [];
    updateAccountHints(state.accounts);

    if (session.authenticated) {
      state.user = session.user;
      currentUserEl.textContent = session.user.displayName;
      toggleWorkspace(true);
      await loadConversations();
    } else {
      toggleWorkspace(false);
    }
  } catch (error) {
    console.error(error);
    toggleWorkspace(false);
  }
};

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  showError(authErrorEl, '');

  const formData = new FormData(loginForm);
  const payload = Object.fromEntries(formData.entries());

  try {
    await api('/api/login', {
      method: 'POST',
      body: payload,
    });
    loginForm.reset();
    await bootstrap();
  } catch (error) {
    showError(authErrorEl, error.message);
  }
});

logoutBtn.addEventListener('click', async () => {
  try {
    await api('/api/logout', { method: 'POST' });
  } catch (error) {
    console.error(error);
  } finally {
    state.user = null;
    state.conversations = [];
    state.activeConversationId = null;
    clearHistoryView();
    toggleWorkspace(false);
  }
});

conversationListEl.addEventListener('click', async (event) => {
  const actionBtn = event.target.closest('.conversation-action-btn');
  if (actionBtn) {
    event.stopPropagation();
    const conversationId = Number(actionBtn.dataset.conversationId);
    if (!conversationId) return;
    if (actionBtn.dataset.action === 'rename') {
      await promptRenameConversation(conversationId);
    } else if (actionBtn.dataset.action === 'delete') {
      await confirmDeleteConversation(conversationId);
    }
    return;
  }

  const item = event.target.closest('.conversation-item');
  if (!item) return;
  const conversationId = Number(item.dataset.conversationId);
  if (!conversationId || conversationId === state.activeConversationId) return;
  await selectConversation(conversationId);
});

newConvoBtn.addEventListener('click', async () => {
  newConvoBtn.disabled = true;
  try {
    const title = `新的会话 ${new Date().toLocaleTimeString()}`;
    const { conversation } = await api('/api/conversations', {
      method: 'POST',
      body: { title },
    });
    state.conversations = [conversation, ...state.conversations];
    await selectConversation(conversation.id);
    renderConversationList();
  } catch (error) {
    showError(chatErrorEl, error.message);
  } finally {
    newConvoBtn.disabled = false;
  }
});

fileInput.addEventListener('change', updateFilePreview);

messageInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    chatForm.requestSubmit();
  }
});

chatForm.addEventListener('input', () => autoResizeTextarea(messageInput));
window.addEventListener('resize', () => autoResizeTextarea(messageInput));
autoResizeTextarea(messageInput);

const handleHistoryScroll = () => {
  if (!chatHistory) return;
  const distance =
    chatHistory.scrollHeight - chatHistory.scrollTop - chatHistory.clientHeight;
  state.stickToBottom = distance < 60;
};

chatHistory?.addEventListener('scroll', handleHistoryScroll);

chatForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  showError(chatErrorEl, '');

  if (state.sending) return;
  if (!state.activeConversationId) {
    showError(chatErrorEl, '请先新建或选择一个会话');
    return;
  }

  const rawValue = messageInput.value;
  const message = rawValue.trim();
  const files = Array.from(fileInput.files || []);

  if (!message && !files.length) {
    showError(chatErrorEl, '至少输入文字或选择一个附件');
    return;
  }

  state.sending = true;
  chatForm.querySelector('button[type="submit"]').disabled = true;

  const attachmentsPreview = files.map((file) => ({
    fileName: file.name,
    size: file.size,
    mimeType: file.type,
  }));

  const optimisticNode = renderBubble({
    role: 'user',
    content: message || (attachmentsPreview.length ? '（附件已上传）' : ''),
    created_at: new Date().toISOString(),
    attachments: attachmentsPreview,
  });

  messageInput.value = '';
  autoResizeTextarea(messageInput);
  fileInput.value = '';
  filePreview.innerHTML = '';
  filePreview.classList.add('hidden');
  showTypingBubble();

  const formData = new FormData();
  formData.append('message', message);
  formData.append('conversationId', state.activeConversationId);
  files.forEach((file) => formData.append('files', file));

  try {
    const chatResult = await api('/api/chat', {
      method: 'POST',
      body: formData,
    });
    await loadHistory(state.activeConversationId, { animateLatest: Boolean(chatResult?.reply) });
    await loadConversations({ keepCurrent: true });
  } catch (error) {
    if (optimisticNode?.parentElement) {
      optimisticNode.remove();
    }
    messageInput.value = rawValue;
    autoResizeTextarea(messageInput);
    showError(chatErrorEl, error.message);
  } finally {
    hideTypingBubble();
    state.sending = false;
    chatForm.querySelector('button[type="submit"]').disabled = false;
  }
});

bootstrap();
