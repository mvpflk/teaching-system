<template>
  <div class="taskmgr">
    <div class="tm-tabs"><span class="active">进程</span><span>性能</span><span>用户</span></div>
    <div class="tm-list">
      <div v-for="proc in processes" :key="proc.name" class="tm-row">
        <span>{{ proc.name }}</span><span>{{ proc.pid }}</span><span>{{ proc.cpu }}%</span><span>{{ proc.mem }} MB</span>
      </div>
    </div>
    <div class="tm-footer">进程数: {{ processes.length }} | CPU 使用率: 5% | 物理内存: 45%</div>
  </div>
</template>
<script setup>
const processes = [
  { name: 'explorer.exe', pid: 1234, cpu: 2, mem: 48 },
  { name: 'svchost.exe', pid: 856, cpu: 1, mem: 32 },
  { name: 'winlogon.exe', pid: 672, cpu: 0, mem: 8 },
  { name: 'cmd.exe', pid: 2104, cpu: 2, mem: 12 },
  { name: 'System Idle Process', pid: 0, cpu: 95, mem: 0 }
]
</script>
<style scoped>
.taskmgr { height: 100%; display: flex; flex-direction: column; font-size: var(--fs-xs); }
.tm-tabs { display: flex; border-bottom: 1px solid #ddd; background: #f0f0f0; }
.tm-tabs span { padding: 6px 16px; cursor: pointer; border-right: 1px solid #ddd; }
.tm-tabs span.active { background: #fff; border-bottom: 2px solid var(--primary-color); font-weight: 600; }
.tm-list { flex: 1; overflow-y: auto; }
.tm-row { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; padding: 3px 10px; border-bottom: 1px solid #f0f0f0; }
.tm-footer { padding: 4px 10px; background: #f0f0f0; border-top: 1px solid #ddd; font-size: 10px; color: #666; }
</style>
