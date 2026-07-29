<template>
  <div class="control-panel">
    <div class="cp-header">控制面板</div>
    <div class="cp-grid">
      <div
        v-for="item in items"
        :key="item.id"
        class="cp-item"
        @click="openSub(item.id)"
      >
        <div class="cp-icon">{{ item.icon }}</div>
        <div class="cp-label">{{ item.label }}</div>
      </div>
    </div>
    <div v-if="subPanel" class="cp-subpanel">
      <div class="cp-subheader"><button @click="subPanel = null">← 返回</button> {{ subPanel.title }}</div>
      <component :is="subPanel.component" />
    </div>
  </div>
</template>
<script setup>
import { ref, h } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
const store = useWin7SimStore()

const subPanel = ref(null)
const items = [
  { id: 'personalize', icon: '🖼️', label: '个性化' },
  { id: 'display', icon: '🖥️', label: '显示' },
  { id: 'datetime', icon: '🕐', label: '日期和时间' },
  { id: 'mouse', icon: '🖱️', label: '鼠标' },
  { id: 'keyboard', icon: '⌨', label: '键盘' },
  { id: 'programs', icon: '📦', label: '程序和功能' }
]

function openSub(id) {
  store.recordAction('click', `cp:${id}`)
  const configs = {
    personalize: { title: '个性化', component: { render: () => h('div', { class: 'cp-form' }, [
      h('label', '桌面背景:'), h('select', { onChange: (e) => { store.wallpaper = e.target.value; store.recordAction('click', 'bg:solid') } }, [
        h('option', { value: 'default' }, 'Windows 7 默认'), h('option', { value: 'solid' }, '纯色')
      ])
    ])}},
    display: { title: '屏幕分辨率', component: { render: () => h('div', { class: 'cp-form' }, [
      h('label', '分辨率:'), h('select', { onChange: (e) => { store.recordAction('click', 'resolution:1366x768') } },
        [h('option', '1366 × 768（推荐）'), h('option', '1920 × 1080')])
    ])}},
    datetime: { title: '日期和时间', component: { render: () => h('div', { class: 'cp-form' }, [
      h('p', `当前: ${new Date().toLocaleString('zh-CN')}`)
    ])}},
    mouse: { title: '鼠标属性', component: { render: () => h('div', { class: 'cp-form' }, [
      h('label', '双击速度:'), h('input', { type: 'range', min: 0, max: 10 })
    ])}},
    keyboard: { title: '键盘属性', component: { render: () => h('div', { class: 'cp-form' }, [
      h('label', '重复延迟:'), h('input', { type: 'range', min: 0, max: 3 })
    ])}},
    programs: { title: '程序和功能', component: { render: () => h('div', { class: 'cp-form' }, [
      h('p', '已安装的程序列表（仿真）')
    ])}}
  }
  subPanel.value = configs[id]
}
</script>
<style scoped>
.control-panel { padding: 16px; height: 100%; }
.cp-header { font-size: var(--fs-md); font-weight: 600; margin-bottom: 16px; color: #333; }
.cp-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.cp-item { text-align: center; padding: 12px; border: 1px solid #e0e0e0; border-radius: 6px; cursor: pointer; transition: background 0.15s; }
.cp-item:hover { background: #e8f0fe; }
.cp-icon { font-size: 32px; margin-bottom: 6px; }
.cp-label { font-size: var(--fs-xs); color: #555; }
.cp-subheader { font-size: var(--fs-sm); margin-bottom: 12px; }
.cp-subheader button { background: none; border: none; color: var(--primary-color); cursor: pointer; font-size: var(--fs-xs); }
.cp-form { padding: 12px; }
.cp-form label { display: block; font-size: var(--fs-xs); color: #555; margin-bottom: 4px; }
.cp-form select, .cp-form input { padding: 4px 8px; border: 1px solid #ccc; border-radius: 3px; font-size: var(--fs-xs); }
</style>
