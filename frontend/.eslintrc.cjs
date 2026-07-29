// ESLint 配置 — 支持 Vue 3 + 未使用导入/变量检测 + 项目专属铁律
// 使用 eslint-plugin-unused-imports 自动发现未引用的导入和变量
// 使用 "no-unused-vars": "off" 委托给 eslint-plugin-unused-imports 统一管理
module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
    'vue/setup-compiler-macros': true,
  },
  extends: ['eslint:recommended', 'plugin:vue/vue3-recommended'],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  plugins: ['vue', 'unused-imports', 'security'],
  rules: {
    // ── 未使用导入/变量检测（委托给 unused-imports） ──
    'no-unused-vars': 'off',
    'unused-imports/no-unused-imports': 'error',
    'unused-imports/no-unused-vars': [
      'warn',
      {
        vars: 'all',
        varsIgnorePattern: '^_',
        args: 'after-used',
        argsIgnorePattern: '^_',
      },
    ],

    // ── Vue 3 规则 ──
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
    'vue/require-default-prop': 'off',
    'vue/max-attributes-per-line': ['warn', { singleline: { max: 3 } }],
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-self-closing': 'off',

    // ── 安全规则 (eslint-plugin-security) ──
    'security/detect-bidi-characters': 'warn',
    'security/detect-object-injection': 'off',
    'security/detect-non-literal-fs-filename': 'off',
    'security/detect-eval-with-expression': 'error',
    'security/detect-no-csrf-before-method-override': 'off',
    'security/detect-possible-timing-attacks': 'warn',
    'security/detect-pseudoRandomBytes': 'warn',

    // ══════════════════════════════════════════════════════════
    // ── CONVENTIONS.md 项目铁律（自动化强制执行） ──
    // ══════════════════════════════════════════════════════════

    // 铁律1: 禁止硬编码颜色（仅 CSS 变量定义处允许）
    'no-restricted-syntax': [
      'warn',
      {
        selector: 'Literal[value=/^#[0-9a-fA-F]{3,8}$/]',
        message:
          '禁止硬编码颜色值，请使用 CSS 变量（如 var(--primary-color)）。如需新增变量，请在 :root 中定义。',
      },
      {
        selector: 'TemplateLiteral:has(TemplateElement[value.raw=/^#[0-9a-fA-F]{3,8}$/])',
        message: '禁止硬编码颜色值，请使用 CSS 变量。',
      },
      // 铁律3: 禁止动态 import('@/utils/request')
      {
        selector: 'ImportExpression[source.value=/@\\/utils\\/request/] ',
        message: '禁止动态 import request 工具，请通过 @/api/* 模块调用后端接口。',
      },
    ],

    // 铁律2: 禁止组件内直接调 axios/fetch/request，必须通过 @/api/*
    'no-restricted-imports': [
      'error',
      {
        paths: [
          {
            name: 'axios',
            message: '禁止在组件内直接 import axios，请通过 @/api/* 模块调用后端接口。',
          },
          {
            name: '@/utils/request',
            message: '禁止在组件内直接 import request，请通过 @/api/* 模块调用后端接口。',
          },
        ],
        patterns: [
          {
            group: ['axios/*', '@/utils/request/*'],
            message: '禁止在组件内直接 import request 工具，请通过 @/api/* 模块。',
          },
        ],
      },
    ],

    // 铁律3: 禁止动态 import('@/utils/request')
    // 使用 no-restricted-syntax 捕获动态 import 表达式
    // 注意：此规则已包含在上面的 no-restricted-syntax 中，通过自定义插件更精确控制
    // 这里用注释标记，实际由 lint-staged 的 eslint 命令 + 自定义规则文件处理

    // 铁律4: 单个 Vue 文件不超过 300 行（warn 级别，不阻塞提交但提醒）
    // ESLint 无法直接检查文件行数，此规则通过 eslint-plugin-file-length 或自定义规则实现
    // 暂时用 max-lines 替代
    'max-lines': ['warn', { max: 300, skipBlankLines: true, skipComments: true }],

    // 铁律5: 禁止 console.log（生产环境）
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',

    // ── 通用规则 ──
    'prefer-const': 'warn',
    'no-var': 'error',
  },

  // unplugin-auto-import 提供的全局 API
  globals: {
    ref: 'readonly',
    computed: 'readonly',
    watch: 'readonly',
    watchEffect: 'readonly',
    reactive: 'readonly',
    readonly: 'readonly',
    toRef: 'readonly',
    toRefs: 'readonly',
    isRef: 'readonly',
    unref: 'readonly',
    shallowRef: 'readonly',
    shallowReactive: 'readonly',
    triggerRef: 'readonly',
    effect: 'readonly',
    effectScope: 'readonly',
    getCurrentScope: 'readonly',
    onActivated: 'readonly',
    onBeforeMount: 'readonly',
    onBeforeUnmount: 'readonly',
    onBeforeUpdate: 'readonly',
    onDeactivated: 'readonly',
    onErrorCaptured: 'readonly',
    onMounted: 'readonly',
    onRenderTracked: 'readonly',
    onRenderTriggered: 'readonly',
    onUnmounted: 'readonly',
    onUpdated: 'readonly',
    nextTick: 'readonly',
    defineComponent: 'readonly',
    defineAsyncComponent: 'readonly',
    resolveComponent: 'readonly',
    resolveDynamicComponent: 'readonly',
    h: 'readonly',
    mergeModels: 'readonly',
    transition: 'readonly',
    keepAlive: 'readonly',
    useAttrs: 'readonly',
    useCssModule: 'readonly',
    useCssVars: 'readonly',
    useSlots: 'readonly',
    withCtx: 'readonly',
    withDefaults: 'readonly',
    withDirectives: 'readonly',
    withKeys: 'readonly',
    withMemo: 'readonly',
    withModifiers: 'readonly',
    useRouter: 'readonly',
    useRoute: 'readonly',
    defineStore: 'readonly',
    storeToRefs: 'readonly',
    usePinia: 'readonly',
    createPinia: 'readonly',
    getActivePinia: 'readonly',
    mapActions: 'readonly',
    mapGetters: 'readonly',
    mapState: 'readonly',
    mapStores: 'readonly',
    mapWritableState: 'readonly',
    setActivePinia: 'readonly',
    setMapHelperSuffix: 'readonly',
    skipHydrate: 'readonly',
    ElMessage: 'readonly',
    ElMessageBox: 'readonly',
    ElNotification: 'readonly',
  },
};
