import { computed } from 'vue';
import { ElMessage } from 'element-plus';

export function useSmartScreenConnection(sseStatus, sseConnect) {
  const connStatusClass = computed(() => {
    switch (sseStatus.value) {
      case 'connected':
        return 'conn-ok';
      case 'connecting':
        return 'conn-busy';
      case 'reconnecting':
        return 'conn-busy';
      case 'error':
        return 'conn-err';
      default:
        return 'conn-off';
    }
  });

  const connStatusLabel = computed(() => {
    switch (sseStatus.value) {
      case 'connected':
        return '已连接';
      case 'connecting':
        return '连接中...';
      case 'reconnecting':
        return '重连中...';
      case 'error':
        return '已断开';
      default:
        return '未连接';
    }
  });

  const connStatusText = computed(() => {
    switch (sseStatus.value) {
      case 'connected':
        return '实时连接正常，学生可接收互动消息';
      case 'connecting':
        return '正在建立连接...';
      case 'reconnecting':
        return '连接断开，正在自动重连...';
      case 'error':
        return '连接失败！请检查网络后手动重连';
      default:
        return '尚未连接';
    }
  });

  const manualReconnect = async () => {
    ElMessage.info('正在重新连接...');
    await sseConnect();
    if (sseStatus.value === 'connected') {
      ElMessage.success('连接已恢复');
    } else {
      ElMessage.warning('重连失败，请刷新页面重试');
    }
  };

  return { connStatusClass, connStatusLabel, connStatusText, manualReconnect };
}
