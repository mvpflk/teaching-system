<template>
  <div class="empty-state" :class="`empty--${effectiveVariant}`">
    <!-- 图标：C4 插画就位前先用增强版 el-icon 占位（预留 <Illustration> 插槽） -->
    <slot name="illustration">
      <el-icon class="empty-icon" :size="finalIconSize">
        <component :is="finalIcon" />
      </el-icon>
    </slot>
    <h4 class="empty-title">{{ finalTitle }}</h4>
    <p v-if="finalDescription" class="empty-desc">{{ finalDescription }}</p>
    <!-- 双 CTA 按钮：主 + 次 -->
    <div v-if="actionText || secondaryActionText" class="empty-actions">
      <el-button
        v-if="secondaryActionText"
        size="small"
        class="press-feedback"
        @click="$emit('secondary-action')"
      >
        {{ secondaryActionText }}
      </el-button>
      <el-button
        v-if="actionText"
        type="primary"
        size="small"
        class="press-feedback"
        @click="$emit('action')"
      >
        {{ actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { FolderOpened, Search, Lock, WarningFilled } from '@element-plus/icons-vue';

/**
 * B5 新增：4 种场景化空状态
 * 优先级：用户显式传入的 prop > variant 预设
 */
const VARIANT_PRESET = {
  'no-data': {
    icon: FolderOpened,
    title: '暂无数据',
    description: '还没有内容，点击右上角按钮创建一个吧',
  },
  'empty-search': {
    icon: Search,
    title: '未找到匹配项',
    description: '换个关键词或筛选条件试试',
  },
  'no-permission': {
    icon: Lock,
    title: '暂无访问权限',
    description: '如需访问此功能，请联系管理员开通权限',
  },
  'loading-error': {
    icon: WarningFilled,
    title: '加载失败',
    description: '网络异常或服务暂不可用，请稍后重试',
  },
};

const props = defineProps({
  /** B5 新增：4 种场景变体 */
  variant: {
    type: String,
    default: '',
    validator: (v) =>
      v === '' || ['no-data', 'empty-search', 'no-permission', 'loading-error'].includes(v),
  },
  /**
   * BUGFIX-1：兼容旧调用的 type prop 别名（HomeBottomGrid、MobileCardList 等老页面传入 type="tasks"/"trophy"）
   * 已知旧值枚举：tasks / trophy → 映射到对应 variant，其他未知值退化 no-data
   */
  type: {
    type: String,
    default: '',
  },
  /** 自定义图标（可选），覆盖 variant 预设 */
  icon: { type: Object, default: null },
  iconSize: { type: Number, default: 52 },
  /** BUGFIX-1：兼容旧调用的 :image-size="80" 写法（= iconSize，同义别名） */
  imageSize: { type: Number, default: 0 },
  /** 自定义标题（可选），覆盖 variant 预设 */
  title: { type: String, default: '' },
  /** 自定义描述（可选），覆盖 variant 预设 */
  description: { type: String, default: '' },
  /** 主按钮文字（可选） */
  actionText: { type: String, default: '' },
  /** B5 新增：次按钮文字（可选，如"返回首页"） */
  secondaryActionText: { type: String, default: '' },
});

defineEmits(['action', 'secondary-action']);

/* ── 兼容层：type → variant 映射表（老值 → 新值） ── */
const TYPE_TO_VARIANT = {
  tasks: 'no-data',
  trophy: 'no-data',
};
/* 最终生效 variant：显式 variant > type 映射 > 默认 no-data */
const effectiveVariant = computed(() => {
  if (props.variant) return props.variant;
  if (props.type) {
    if (TYPE_TO_VARIANT[props.type]) return TYPE_TO_VARIANT[props.type];
    console.warn('[EmptyState] 未知的 type prop 值:', props.type);
  }
  return 'no-data';
});
/* 最终生效 iconSize：imageSize（旧别名） > iconSize（新） */
const finalIconSize = computed(() =>
  props.imageSize && props.imageSize > 0 ? props.imageSize : props.iconSize
);

/* ── 最终渲染值：显式 prop 优先，缺失则走 variant 预设 ── */
const finalIcon = computed(
  () => props.icon ?? VARIANT_PRESET[effectiveVariant.value]?.icon ?? FolderOpened
);
const finalTitle = computed(
  () => props.title || (VARIANT_PRESET[effectiveVariant.value]?.title ?? '暂无数据')
);
const finalDescription = computed(
  () => props.description || (VARIANT_PRESET[effectiveVariant.value]?.description ?? '')
);
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: var(--spacing-2xl) var(--spacing-lg);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* B5 新增：4 场景色条定位基准 */
  position: relative;
  border-radius: var(--radius-lg);
}
/* 4 种变体的左侧氛围色条（低饱和，不抢注意力） */
.empty-state::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 3px;
  border-radius: 2px;
  opacity: 0.6;
}
.empty--no-data::before {
  background: var(--primary-color);
}
.empty--empty-search::before {
  background: var(--info-color);
}
.empty--no-permission::before {
  background: var(--warning-color);
}
.empty--loading-error::before {
  background: var(--danger-color);
}

/* 图标：主色浅底 + 圆形徽章，不再是裸灰色 */
.empty-icon {
  color: var(--primary-color);
  background: var(--primary-light);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-md);
  padding: 16px;
  box-sizing: border-box;
}
.empty--no-permission .empty-icon {
  background: var(--bg-warning-light);
  color: var(--warning-color);
}
.empty--loading-error .empty-icon {
  background: var(--bg-danger-light);
  color: var(--danger-color);
}
.empty--empty-search .empty-icon {
  background: var(--bg-secondary);
  color: var(--text-regular);
}

.empty-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs);
  line-height: var(--lh-tight);
}
.empty-desc {
  font-size: var(--fs-sm);
  margin: 0 0 var(--spacing-md);
  line-height: var(--lh-relaxed);
  max-width: 360px;
  color: var(--text-secondary);
}
/* 双 CTA 容器：次按钮 + 主按钮 左右排列 */
.empty-actions {
  display: inline-flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  justify-content: center;
}
</style>
