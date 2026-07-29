<template>
  <div class="ph-card">
    <div class="ph-card-title">第 {{ weekNo }} 周学习包</div>
    <p class="ph-card-desc">可打印的离线纸笔练习，或在线直接答题</p>
    <van-button
      type="success"
      block
      round
      :loading="packLoading"
      style="background: var(--el-color-success); border-color: var(--el-color-success)"
      @click="openWeeklyPack"
    >
      生成学习包
    </van-button>
    <div v-if="packReady" class="ph-pack-toolbar">
      <van-button
        type="primary"
        size="small"
        icon="edit"
        @click="startOnlinePractice"
      >
        在线答题
      </van-button>
      <van-button
        plain
        hairline
        size="small"
        icon="share-o"
        @click="copyPackLink"
      >
        复制链接
      </van-button>
      <van-button
        plain
        hairline
        size="small"
        icon="printer"
        @click="printPack"
      >
        打印
      </van-button>
    </div>
    <iframe
      v-if="packReady && !onlineMode"
      :srcdoc="packHtml"
      class="ph-pack-frame"
      :style="{ height: 'calc(100vh - 340px)' }"
    />
    <div v-if="onlineMode" class="ph-online-practice">
      <OnlinePracticeMode :subject="subject" :week-no="weekNo" @complete="onOnlineComplete" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { getWeeklyPack } from '@/api/precision';
import { sanitizeHtml } from '@/utils/markdown';
import OnlinePracticeMode from './OnlinePracticeMode.vue';

const props = defineProps({
  subject: { type: Object, required: true },
  weekNo: { type: Number, required: true },
});

const emit = defineEmits(['refresh']);

const packLoading = ref(false);
const packReady = ref(false);
const packHtml = ref('');
const onlineMode = ref(false);

async function openWeeklyPack() {
  packLoading.value = true;
  packReady.value = false;
  onlineMode.value = false;
  try {
    const res = await getWeeklyPack(props.subject.key, props.weekNo);
    packHtml.value = res;
    packReady.value = true;
  } catch (e) {
    let msg = '生成失败';
    try {
      const d = JSON.parse(e?.response?.data);
      msg = d.message || msg;
    } catch (_) {
      /* 忽略 */
    }
    if (
      e?.response?.data &&
      typeof e.response.data === 'string' &&
      e.response.data.startsWith('<html')
    ) {
      packHtml.value = e.response.data;
      packReady.value = true;
      return;
    }
    showToast(msg);
  }
  packLoading.value = false;
}

function startOnlinePractice() {
  onlineMode.value = true;
}

function onOnlineComplete() {
  onlineMode.value = false;
  showToast('练习完成！');
  emit('refresh');
}

async function copyPackLink() {
  const url = `${location.origin}/api/precision/weekly-pack?subject=${encodeURIComponent(props.subject.key)}&week=${props.weekNo}`;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url);
    } else {
      const ta = document.createElement('textarea');
      ta.value = url;
      ta.style.position = 'fixed';
      ta.style.left = '-9999px';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
    showToast('已复制到剪贴板');
  } catch {
    showToast('复制失败，请手动复制链接');
  }
}

function printPack() {
  const w = window.open('', '_blank');
  if (w) {
    const cleanHtml = sanitizeHtml(packHtml.value);
    w.document.write(cleanHtml);
    w.document.close();
    w.print();
  }
}
</script>

<style scoped>
.ph-card {
  margin: 0 16px;
  padding: 20px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
}
.ph-card-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
  margin-bottom: 6px;
}
.ph-card-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--text-secondary));
  margin: 0 0 16px;
  line-height: 1.5;
}
.ph-pack-toolbar {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.ph-pack-frame {
  width: 100%;
  height: 360px;
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-sm, 4px);
  margin-top: 12px;
}
</style>
