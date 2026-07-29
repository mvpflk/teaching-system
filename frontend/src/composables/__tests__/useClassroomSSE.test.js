import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ref } from 'vue';

// Mock sseTicket
const mockClose = vi.fn();
const mockAddEventListener = vi.fn();
const mockRemoveEventListener = vi.fn();
const mockEs = {
  close: mockClose,
  addEventListener: mockAddEventListener,
  removeEventListener: mockRemoveEventListener,
  readyState: 1,
};

vi.mock('@/utils/sseTicket', () => ({
  createEventSourceWithReconnect: vi.fn().mockResolvedValue({
    es: mockEs,
    close: mockClose,
  }),
}));

describe('useClassroomSSE', () => {
  let useClassroomSSE;

  beforeEach(async () => {
    vi.clearAllMocks();
    const mod = await import('@/composables/useClassroomSSE.js');
    useClassroomSSE = mod.useClassroomSSE;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('初始状态为 disconnected', () => {
    const classId = ref(1);
    const { status } = useClassroomSSE(classId);

    expect(status.value).toBe('disconnected');
  });

  it('connect 后状态变为 connected', async () => {
    const classId = ref(1);
    const { connect, status } = useClassroomSSE(classId);

    await connect();

    expect(status.value).toBe('connected');
  });

  it('on 注册事件监听器', async () => {
    const classId = ref(1);
    const { connect, on } = useClassroomSSE(classId);
    const handler = vi.fn();

    await connect();
    on('buzz', handler);

    expect(mockAddEventListener).toHaveBeenCalledWith('buzz', handler);
  });

  it('off 移除事件监听器', async () => {
    const classId = ref(1);
    const { connect, on, off } = useClassroomSSE(classId);
    const handler = vi.fn();

    await connect();
    on('buzz', handler);
    off('buzz', handler);

    expect(mockRemoveEventListener).toHaveBeenCalledWith('buzz', handler);
  });

  it('close 断开连接', async () => {
    const classId = ref(1);
    const { connect, close, status } = useClassroomSSE(classId);

    await connect();
    close();

    expect(status.value).toBe('disconnected');
    expect(mockClose).toHaveBeenCalled();
  });

  it('onError 注册错误监听器', async () => {
    const classId = ref(1);
    const { onError, offError } = useClassroomSSE(classId);
    const handler = vi.fn();

    onError(handler);
    offError(handler);

    // 不应报错
  });
});
