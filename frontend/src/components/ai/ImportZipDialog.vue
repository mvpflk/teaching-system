<template>
  <el-dialog
    v-model="visible"
    :title="formatLabel[format]"
    width="580px"
    destroy-on-close
  >
    <div class="izd-hint">
      <div class="izd-format-row">
        <span>导入格式：</span>
        <el-radio-group v-model="format" size="small">
          <el-radio-button value="zip">ZIP 压缩包</el-radio-button>
          <el-radio-button value="txt">TXT 文本</el-radio-button>
          <el-radio-button value="docx">Word 文档</el-radio-button>
        </el-radio-group>
      </div>
      <pre class="izd-struct" v-text="structureHint" />
      <div class="izd-extra-hint">{{ extraHint }}</div>
    </div>

    <div class="izd-row">
      <span>目标学科：</span>
      <el-select v-model="subjectId" placeholder="选择学科" style="width: 220px">
        <el-option
          v-for="s in subjects"
          :key="s.id"
          :label="s.subjectName"
          :value="s.id"
        />
      </el-select>
    </div>

    <el-upload
      drag
      :show-file-list="false"
      :http-request="handleUpload"
      :accept="acceptTypes"
      class="izd-upload"
    >
      <el-icon class="izd-upload-icon"><UploadFilled /></el-icon>
      <div>点击或拖拽 {{ acceptLabel }} 到此处</div>
    </el-upload>

    <div v-if="uploading" style="text-align: center; padding: 8px">
      <el-icon class="is-loading"><Loading /></el-icon> 正在导入...
    </div>

    <div class="izd-template-row">
      <span>下载模板：</span>
      <el-button size="small" @click="downloadTemplate('culture')">文化课模板</el-button>
      <el-button size="small" @click="downloadTemplate('professional')">专业课模板</el-button>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { importKnowledgeZip, importKnowledgeTxt, importKnowledgeDocx } from '@/api/knowledgeNode';
import { getMySubjects } from '@/api/settings';
import { ElMessage } from 'element-plus';
import { UploadFilled, Loading } from '@element-plus/icons-vue';

const emit = defineEmits(['imported']);

const visible = ref(false);
const subjectId = ref(null);
const subjects = ref([]);
const uploading = ref(false);
const format = ref('zip');

const formatLabel = { zip: '导入知识库 (ZIP)', txt: '导入知识库 (TXT)', docx: '导入知识库 (Word)' };
const acceptTypes = computed(() => ({ zip: '.zip', txt: '.txt', docx: '.docx' })[format.value]);
const acceptLabel = computed(
  () => ({ zip: 'ZIP 文件', txt: 'TXT 文件', docx: 'Word 文档' })[format.value]
);

const structureHint = computed(() => {
  switch (format.value) {
    case 'zip':
      return '章节文件夹/\n  ├── 任务文件夹/\n  │   ├── 知识点1.md(.txt)\n  │   └── 知识点2.md(.txt)\n  └── ...';
    case 'txt':
      return '# 章节名\n## 任务名\n### 知识点名\n知识点详细内容（可多行）...\n\n# 下一章节\n## ...';
    case 'docx':
      return '标题1 = 章节名\n标题2 = 任务名\n标题3 = 知识点名\n正文 = 知识点详细内容';
    default:
      return '';
  }
});

const extraHint = computed(() => {
  switch (format.value) {
    case 'zip':
      return 'ZIP 内的 .md 或 .txt 文件将被自动解析为知识点。';
    case 'txt':
      return '使用 # / ## / ### 标记层级，正文为该知识点的详细内容。';
    case 'docx':
      return '请在 Word 中使用「标题1」「标题2」「标题3」样式编写，保存为 .docx。';
    default:
      return '';
  }
});

const handleUpload = async (options) => {
  if (!subjectId.value) return ElMessage.warning('请先选择目标学科');
  uploading.value = true;
  try {
    const fd = new FormData();
    fd.append('file', options.file);
    let res;
    if (format.value === 'zip') res = await importKnowledgeZip(subjectId.value, fd);
    else if (format.value === 'txt') res = await importKnowledgeTxt(subjectId.value, fd);
    else res = await importKnowledgeDocx(subjectId.value, fd);
    if (res.code === 200) {
      ElMessage.success(res.message || `成功导入 ${res.data?.count || 0} 个知识点`);
      visible.value = false;
      emit('imported');
    } else {
      ElMessage.error(res.message || '导入失败');
    }
  } catch {
    ElMessage.error('上传失败');
  } finally {
    uploading.value = false;
  }
};

const downloadTemplate = (type) => {
  const suffix = format.value === 'zip' ? 'zip' : 'txt';
  const name = type === 'professional' ? '专业课' : '文化课';
  let url;
  if (format.value === 'zip') url = `/api/knowledge-node/actions/zip-template/download`;
  else if (format.value === 'txt')
    url = `/api/knowledge-node/actions/txt-template/download?type=${type}`;
  else url = `/api/knowledge-node/actions/docx-template/download?type=${type}`;
  const a = document.createElement('a');
  a.href = url;
  a.download = `${name}知识点导入模板.${suffix}`;
  a.click();
};

onMounted(async () => {
  try {
    const res = await getMySubjects();
    if (res.code === 200) subjects.value = res.data || [];
  } catch {
    /* */
  }
});

const open = () => {
  visible.value = true;
  format.value = 'zip';
};
defineExpose({ open, downloadTemplate });
</script>

<style scoped>
.izd-hint {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin-bottom: 16px;
  line-height: 1.6;
}
.izd-format-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.izd-struct {
  background: var(--bg-section);
  padding: 10px 14px;
  border-radius: 6px;
  font-size: var(--fs-xs);
  margin: 6px 0;
  white-space: pre;
}
.izd-extra-hint {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: var(--fs-xs);
}
.izd-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: var(--fs-sm);
}
.izd-upload {
  margin-bottom: 8px;
}
.izd-upload-icon {
  font-size: 36px;
  color: var(--text-disabled);
}
.izd-template-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: var(--fs-sm);
  color: var(--text-regular);
}
</style>
