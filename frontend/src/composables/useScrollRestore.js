/**
 * 移动端滚动位置恢复 — 解决 iOS 左滑返回时列表滚动位置丢失
 *
 * 用法：
 *   import { useScrollRestore } from '@/composables/useScrollRestore'
 *   useScrollRestore('student_tasks')  // key 用于区分不同列表页
 */
import { onMounted, onUnmounted } from 'vue';

export function useScrollRestore(key) {
  onMounted(() => {
    const saved = sessionStorage.getItem(`scroll_${key}`);
    if (saved) {
      requestAnimationFrame(() => {
        window.scrollTo(0, parseInt(saved, 10));
      });
    }
  });

  const save = () => {
    sessionStorage.setItem(`scroll_${key}`, window.scrollY.toString());
  };

  onMounted(() => {
    window.addEventListener('scrollend', save);
    window.addEventListener('beforeunload', save);
  });

  onUnmounted(() => {
    save();
    window.removeEventListener('scrollend', save);
    window.removeEventListener('beforeunload', save);
  });
}
