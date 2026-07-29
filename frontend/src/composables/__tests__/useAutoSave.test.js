import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ref } from 'vue';

describe('useAutoSave', () => {
  let useAutoSave, autoSave, restoreDraft, clearDraft;

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    const mod = await import('@/composables/useAutoSave.js');
    useAutoSave = mod.useAutoSave;
    autoSave = mod.autoSave;
    restoreDraft = mod.restoreDraft;
    clearDraft = mod.clearDraft;
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  describe('autoSave 函数', () => {
    it('key 为空时不执行', async () => {
      // 不应报错
      autoSave('', { test: 1 });
      await vi.advanceTimersByTimeAsync(5000);
    });

    it('返回函数不抛错', () => {
      expect(typeof autoSave).toBe('function');
    });
  });

  describe('restoreDraft 函数', () => {
    it('key 为空时返回 null', async () => {
      const result = await restoreDraft('');
      expect(result).toBeNull();
    });

    it('返回 Promise', () => {
      const result = restoreDraft('test_key');
      expect(result).toBeInstanceOf(Promise);
    });
  });

  describe('clearDraft 函数', () => {
    it('key 为空时不执行', async () => {
      // 不应报错
      await clearDraft('');
    });

    it('返回 Promise', () => {
      const result = clearDraft('test_key');
      expect(result).toBeInstanceOf(Promise);
    });
  });

  describe('useAutoSave composable', () => {
    it('返回正确的 API', () => {
      const key = ref('test');
      const result = useAutoSave(key, () => ({}));

      expect(result).toHaveProperty('draft');
      expect(result).toHaveProperty('isRestoring');
      expect(result).toHaveProperty('clearDraft');
      expect(result).toHaveProperty('restore');
    });

    it('draft 初始值为 null', () => {
      const key = ref('test');
      const { draft } = useAutoSave(key, () => ({}));

      expect(draft.value).toBeNull();
    });

    it('isRestoring 初始值为 false', () => {
      const key = ref('test');
      const { isRestoring } = useAutoSave(key, () => ({}));

      expect(isRestoring.value).toBe(false);
    });
  });
});
