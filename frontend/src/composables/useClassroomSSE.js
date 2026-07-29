import { ref, onBeforeUnmount } from 'vue';
import { createEventSourceWithReconnect } from '@/utils/sseTicket';

/**
 * 智慧大屏 SSE 连接生命周期管理
 *
 * R112v3改进：
 * - connected事件附带studentId，前端直接取用无需额外API
 * - 心跳超时检测：40秒内未收到heartbeat → 主动断开并重连
 * - 连接状态细化：disconnected | connecting | connected | reconnecting | error
 * - 重连后自动恢复所有业务监听器绑定
 */
export function useClassroomSSE(classId) {
  const conn = ref(null);
  const status = ref('disconnected');
  const myStudentId = ref(null); // R112v3: 从connected事件中提取，100%可靠
  const onlineCount = ref(0); // 从心跳事件获取在线人数
  const listeners = new Map();
  const errorListeners = new Set();
  let reconnectWrapper = null;
  let heartbeatTimer = null;
  const HEARTBEAT_TIMEOUT = 40_000;

  const resetHeartbeatTimer = () => {
    clearTimeout(heartbeatTimer);
    heartbeatTimer = setTimeout(() => {
      if (status.value === 'connected' && reconnectWrapper) {
        status.value = 'reconnecting';
        if (reconnectWrapper.es) {
          reconnectWrapper.es.close();
        }
      }
    }, HEARTBEAT_TIMEOUT);
  };

  const connect = async () => {
    if (conn.value) return conn.value;
    try {
      status.value = 'connecting';

      // R112v3: connected事件携带studentId
      const allListeners = {
        heartbeat: (e) => {
          resetHeartbeatTimer();
          try {
            const d = JSON.parse(e.data);
            if (d.onlineCount != null) onlineCount.value = d.onlineCount;
          } catch {
            /* ignore */
          }
        },
        connected: (e) => {
          status.value = 'connected';
          resetHeartbeatTimer();
          // 从connected事件提取studentId
          try {
            const d = JSON.parse(e.data);
            if (d.studentId) myStudentId.value = d.studentId;
          } catch {
            /* ignore */
          }
        },
      };

      reconnectWrapper = await createEventSourceWithReconnect(
        `/api/classroom/class/${classId}/subscribe`,
        allListeners,
        {
          maxRetries: 8,
          onStatusChange: (newStatus) => {
            if (newStatus === 'connected') {
              status.value = 'connected';
              resetHeartbeatTimer();
            } else if (newStatus === 'reconnecting') {
              status.value = 'reconnecting';
            } else if (newStatus === 'error') {
              status.value = 'error';
              for (const handler of errorListeners) handler();
            }
          },
        }
      );
      conn.value = reconnectWrapper.es;
      status.value = 'connected';
      resetHeartbeatTimer();

      for (const [event, handlers] of listeners) {
        for (const handler of handlers) {
          conn.value.addEventListener(event, handler);
        }
      }
      return conn.value;
    } catch {
      status.value = 'error';
      for (const handler of errorListeners) handler();
      return null;
    }
  };

  const on = (event, handler) => {
    if (!listeners.has(event)) listeners.set(event, new Set());
    listeners.get(event).add(handler);
    if (conn.value) conn.value.addEventListener(event, handler);
  };

  const off = (event, handler) => {
    if (listeners.has(event)) {
      listeners.get(event).delete(handler);
    }
    if (conn.value) conn.value.removeEventListener(event, handler);
  };

  const onError = (handler) => {
    errorListeners.add(handler);
  };

  const offError = (handler) => {
    errorListeners.delete(handler);
  };

  const close = () => {
    clearTimeout(heartbeatTimer);
    if (reconnectWrapper) {
      reconnectWrapper.close();
      reconnectWrapper = null;
    }
    conn.value = null;
    listeners.clear();
    errorListeners.clear();
    status.value = 'disconnected';
    myStudentId.value = null;
  };

  onBeforeUnmount(() => {
    close();
  });

  return { connect, on, off, onError, offError, close, conn, status, myStudentId, onlineCount };
}
