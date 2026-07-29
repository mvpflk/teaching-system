export default [
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    redirect: '/knowledge-base/discover',
    component: () => import('@/views/knowledge/KnowledgeBaseHub.vue'),
    meta: { title: '知识库', icon: 'Reading', featureKey: 'feature.knowledge_base' },
    children: [
      {
        path: 'discover',
        name: 'KnowledgeDiscover',
        component: () => import('@/views/knowledge/KnowledgeDiscover.vue'),
        meta: { title: '发现' }
      },
      {
        path: 'article/:id',
        name: 'KnowledgeArticle',
        component: () => import('@/views/knowledge/KnowledgeArticle.vue'),
        meta: { title: '知识详情' }
      },
      {
        path: 'review',
        name: 'KnowledgeReview',
        component: () => import('@/views/knowledge/KnowledgeReview.vue'),
        meta: { title: '今日复习', roles: ['STUDENT'] }
      },
      {
        path: 'mine',
        name: 'KnowledgeMine',
        component: () => import('@/views/knowledge/KnowledgeMine.vue'),
        meta: { title: '我的学习', roles: ['STUDENT'] }
      },
      {
        path: 'admin/articles',
        name: 'KnowledgeAdmin',
        component: () => import('@/views/knowledge/KnowledgeAdmin.vue'),
        meta: { title: '知识库管理', roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER'] }
      },
      {
        path: 'admin/articles/:id',
        name: 'KnowledgeArticleEditor',
        component: () => import('@/views/knowledge/KnowledgeArticleEditor.vue'),
        meta: { title: '编辑文章', roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER'] }
      },
      {
        path: 'admin/card-review',
        name: 'CardReviewQueue',
        component: () => import('@/views/knowledge/CardReviewQueue.vue'),
        meta: { title: '卡片审核', roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER'] }
      },
      {
        path: 'admin/class-stats',
        name: 'ClassStats',
        component: () => import('@/views/knowledge/ClassStatsView.vue'),
        meta: { title: '全班统计', roles: ['SUPER_ADMIN', 'ADMIN', 'TEACHER'], layoutWidth: 'wide' }
      },
      {
        path: 'english/vocab',
        name: 'KnowledgeVocabDrill',
        component: () => import('@/views/knowledge/KnowledgeVocabDrill.vue'),
        meta: { title: '单词记忆', roles: ['STUDENT'] }
      },
      {
        path: 'checklists',
        name: 'KnowledgeChecklists',
        component: () => import('@/views/knowledge/KnowledgeChecklist.vue'),
        meta: { title: '知识清单', roles: ['STUDENT'] }
      },
      {
        path: 'checklist/:id',
        name: 'KnowledgeChecklistDetail',
        component: () => import('@/views/knowledge/KnowledgeChecklistDetail.vue'),
        meta: { title: '清单详情', roles: ['STUDENT'] }
      }
    ]
  }
]
