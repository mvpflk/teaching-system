import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ref } from 'vue';

// Mock API
const mockReportCheatWarning = vi
  .fn()
  .mockResolvedValue({ code: 200, data: { cheatWarnings: 0, terminated: false } });
vi.mock('@/api/task', () => ({
  reportCheatWarning: (...args) => mockReportCheatWarning(...args),
}));

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: { warning: vi.fn(), error: vi.fn() },
  ElMessageBox: { alert: vi.fn().mockResolvedValue(undefined) },
}));

describe('useCheatMonitor', () => {
  let useCheatMonitor;

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mockReportCheatWarning.mockResolvedValue({
      code: 200,
      data: { cheatWarnings: 0, terminated: false },
    });

    const mod = await import('@/composables/useCheatMonitor.js');
    useCheatMonitor = mod.useCheatMonitor;

    // Mock localStorage
    const store = {};
    Storage.prototype.getItem = vi.fn((key) => store[key] || null);
    Storage.prototype.setItem = vi.fn((key, value) => {
      store[key] = value;
    });
    Storage.prototype.removeItem = vi.fn((key) => {
      delete store[key];
    });

    // Mock document.hidden
    Object.defineProperty(document, 'hidden', { value: false, writable: true, configurable: true });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('初始化时 cheatCount 为 0', () => {
    const taskId = ref('1');
    const taskConfig = ref(null);
    const { cheatCount, terminated } = useCheatMonitor(taskId, taskConfig);

    expect(cheatCount.value).toBe(0);
    expect(terminated.value).toBe(false);
  });

  it('clearCache 调用 localStorage.removeItem', () => {
    const taskId = ref('1');
    const taskConfig = ref(null);
    const { clearCache } = useCheatMonitor(taskId, taskConfig);

    clearCache();
    expect(localStorage.removeItem).toHaveBeenCalled();
  });

  it('返回正确的 API', () => {
    const taskId = ref('1');
    const taskConfig = ref(null);
    const result = useCheatMonitor(taskId, taskConfig);

    expect(result).toHaveProperty('cheatCount');
    expect(result).toHaveProperty('maxWarnings');
    expect(result).toHaveProperty('terminated');
    expect(result).toHaveProperty('clearCache');
    expect(result).toHaveProperty('flushPending');
    expect(result).toHaveProperty('activate');
    expect(result).toHaveProperty('deactivate');
  });

  it('flushPending 调用 reportCheatWarning', async () => {
    const taskId = ref('1');
    const taskConfig = ref(null);
    const { flushPending } = useCheatMonitor(taskId, taskConfig);

    await flushPending();
    expect(mockReportCheatWarning).toHaveBeenCalled();
  });

  it('activate/deactivate 切换考试状态', () => {
    const taskId = ref('1');
    const taskConfig = ref(null);
    const { activate, deactivate } = useCheatMonitor(taskId, taskConfig);

    // activate 和 deactivate 不应抛错
    activate();
    deactivate();
  });
});
