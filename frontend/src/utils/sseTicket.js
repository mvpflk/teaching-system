/**
 * SSE Ticket — 短期一次性令牌，避免主 JWT 暴露在 URL 参数中。
 *
 * 用法：
 *   const es = await createEventSource('/api/classroom/class/123/subscribe')
 *   es.addEventListener('quiz:start', (e) => { ... })
 *   // 用完后 es.close()
 *
 * R112: 重连增加随机jitter防惊群 + ticket提前刷新 + error短暂等待期
 */

/** 按命名空间隔离的 ticket 缓存，避免不同功能模块的 ticket 互相覆盖 */
const ticketCaches = new Map();

function getCache(namespace) {
  if (!ticketCaches.has(namespace)) {
    ticketCaches.set(namespace, { promise: null, ticket: null, expireTime: 0 });
  }
  return ticketCaches.get(namespace);
}

/**
 * 获取 SSE ticket（120秒过期，提前15秒刷新，同 namespace 内复用直至过期）
 * @param {string} namespace - 缓存命名空间，不同功能（AI/课堂）用不同 key 隔离
 */
async function getTicket(namespace = 'default') {
  const cache = getCache(namespace);
  if (cache.ticket && Date.now() < cache.expireTime) return cache.ticket;

  // 防止并发请求
  if (cache.promise) return cache.promise;

  cache.promise = (async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/sse/ticket', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error('Failed to get SSE ticket');
      const json = await res.json();
      cache.ticket = json.data.ticket;
      cache.expireTime = Date.now() + (json.data.expiresIn - 15) * 1000; // 提前15秒刷新
      return cache.ticket;
    } finally {
      cache.promise = null;
    }
  })();

  return cache.promise;
}

/**
 * 创建安全的 EventSource（ticket 在 URL，主 JWT 不出现在 URL 中）
 * @param {string} url - SSE 端点 URL
 * @param {string} [namespace='default'] - 缓存命名空间，用于隔离不同功能的 ticket
 */
export async function createEventSource(url, namespace = 'default') {
  const ticket = await getTicket(namespace);
  const sep = url.includes('?') ? '&' : '?';
  return new EventSource(`${url}${sep}token=${encodeURIComponent(ticket)}`);
}

/**
 * 创建带断线重连的 SSE 连接（指数退避 + 随机抖动，最大 8 次）
 *
 * R112改进：
 * - 重连延迟 = 基础退避 + 随机jitter(0~1s)，防止微机室40人同时重连冲垮服务器
 * - 收到error后先等500ms观察浏览器是否自动恢复，避免双重重连冲突
 * - 最大重试次数增加到8次，覆盖更长的网络波动
 *
 * @param {string} url
 * @param {object} listenerMap { eventName: handler } — 在每次重连后自动重新绑定
 * @param {object} opts { maxRetries?: number, onStatusChange?: (status) => void, namespace?: string }
 * @returns {{ close: () => void, es: EventSource }}
 */
export async function createEventSourceWithReconnect(url, listenerMap, opts = {}) {
  const maxRetries = opts.maxRetries ?? 8;
  const onStatusChange = opts.onStatusChange || (() => {});
  const namespace = opts.namespace || 'default';
  let es = null;
  let retryCount = 0;
  let closed = false;
  let reconnectTimer = null;
  let errorWaitTimer = null;

  async function connect() {
    if (closed) return;
    // 先关闭旧连接，防止 error 事件触发多余重连
    if (es) {
      es.close();
      es = null;
    }
    try {
      es = await createEventSource(url, namespace);
    } catch (e) {
      // ticket获取失败 → 延迟重试
      if (!closed && retryCount < maxRetries) {
        retryCount++;
        onStatusChange('reconnecting');
        const delay =
          Math.min(1000 * Math.pow(2, retryCount), 30000) + Math.floor(Math.random() * 1000);
        reconnectTimer = setTimeout(connect, delay);
      } else {
        onStatusChange('error');
      }
      return;
    }

    // 重新绑定所有事件监听器
    for (const [event, handler] of Object.entries(listenerMap)) {
      es.addEventListener(event, handler);
    }

    retryCount = 0;
    onStatusChange('connected');

    // R112: error事件处理 — 短暂等待期防浏览器自动恢复
    es.addEventListener('error', () => {
      if (closed || retryCount >= maxRetries) return;

      // 先等500ms，看浏览器是否自动恢复连接
      clearTimeout(errorWaitTimer);
      errorWaitTimer = setTimeout(() => {
        if (closed) return;
        // 检查readyState: 0=CONNECTING, 1=OPEN, 2=CLOSED
        if (es && es.readyState === EventSource.OPEN) {
          // 浏览器已自动恢复，不需要自定义重连
          onStatusChange('connected');
          return;
        }
        // 确实断开了，开始自定义重连
        retryCount++;
        onStatusChange('reconnecting');
        const baseDelay = Math.min(1000 * Math.pow(2, retryCount), 30000);
        const jitter = Math.floor(Math.random() * 1000); // 0-1000ms随机抖动
        const delay = baseDelay + jitter;
        clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(connect, delay);
      }, 500);
    });
  }

  await connect();

  return {
    close: () => {
      closed = true;
      clearTimeout(reconnectTimer);
      clearTimeout(errorWaitTimer);
      if (es) {
        es.close();
        es = null;
      }
    },
    get es() {
      return es;
    },
  };
}
