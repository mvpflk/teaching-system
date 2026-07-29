<template>
  <div class="honor-poster-wrap">
    <el-button type="primary" plain @click="generate">
      <el-icon><PictureFilled /></el-icon> 生成荣誉海报
    </el-button>

    <canvas
      ref="canvasRef"
      style="display:none"
      width="800"
      height="1100"
    />

    <el-dialog
      v-model="showPreview"
      title="荣誉海报预览"
      width="420px"
      append-to-body
    >
      <img v-if="posterUrl" :src="posterUrl" style="width:100%;border-radius:8px" />
      <div class="poster-actions" style="text-align:center;margin-top:12px">
        <el-button type="primary" @click="download">
          <el-icon><Download /></el-icon> 下载海报
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PictureFilled, Download } from '@element-plus/icons-vue'
import { primaryColor, elInfo } from '@/utils/theme'

const props = defineProps({
  work: { type: Object, required: true }
})

const canvasRef = ref(null)
const posterUrl = ref('')
const showPreview = ref(false)

const generate = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 1100

  // 背景渐变
  const gradient = ctx.createLinearGradient(0, 0, w, h)
  gradient.addColorStop(0, '#f0f3ff')
  gradient.addColorStop(0.5, '#ffffff')
  gradient.addColorStop(1, '#f5f0ff')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, w, h)

  // 装饰边框
  ctx.strokeStyle = primaryColor
  ctx.lineWidth = 3
  ctx.strokeRect(20, 20, w - 40, h - 40)
  ctx.strokeStyle = '#e0e4f0'
  ctx.lineWidth = 1
  ctx.strokeRect(30, 30, w - 60, h - 60)

  // 顶部装饰线
  ctx.fillStyle = primaryColor
  ctx.fillRect(60, 80, 40, 4)
  ctx.fillRect(110, 80, 40, 4)
  ctx.fillRect(160, 80, 200, 4)

  // 标题
  ctx.fillStyle = '#1a1d29'
  ctx.font = 'bold 42px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('荣 誉 证 书', w / 2, 160)

  // 副标题
  ctx.fillStyle = '#606266'
  ctx.font = '20px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('优秀作品展示墙', w / 2, 200)

  // 分割线
  ctx.strokeStyle = primaryColor
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(200, 230)
  ctx.lineTo(600, 230)
  ctx.stroke()

  // 学生姓名
  ctx.fillStyle = '#1a1d29'
  ctx.font = 'bold 32px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(props.work.studentName || '同学', w / 2, 300)

  // 作品信息
  ctx.fillStyle = '#606266'
  ctx.font = '22px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('作品《' + (props.work.title || '未命名') + '》', w / 2, 360)
  ctx.fillText('被推荐为优秀作品', w / 2, 400)

  // 积分
  ctx.fillStyle = primaryColor
  ctx.font = 'bold 28px "PingFang SC", "Microsoft YaHei", sans-serif'
  ctx.fillText('获得 +' + (props.work.creditAwarded || 0) + ' 积分奖励', w / 2, 460)

  // 底部信息
  ctx.fillStyle = elInfo
  ctx.font = '16px "PingFang SC", "Microsoft YaHei", sans-serif'
  const teacherName = props.work.teacherName || '教师'
  ctx.fillText('推荐教师：' + teacherName, w / 2, 600)
  ctx.fillText('班级：' + (props.work.grade || '') + (props.work.className || ''), w / 2, 640)
  ctx.fillText('学科：' + (props.work.subject || '通用'), w / 2, 680)

  // 底部装饰
  ctx.fillStyle = '#e0e4f0'
  ctx.fillRect(w / 2 - 150, 750, 300, 2)
  ctx.fillStyle = elInfo
  ctx.font = '14px "PingFang SC", "Microsoft YaHei", sans-serif'
  const now = new Date()
  ctx.fillText('生成日期：' + now.getFullYear() + '年' + (now.getMonth() + 1) + '月' + now.getDate() + '日', w / 2, 790)

  // 生成预览
  posterUrl.value = canvas.toDataURL('image/png')
  showPreview.value = true
}

const download = () => {
  const a = document.createElement('a')
  a.href = posterUrl.value
  a.download = `荣誉证书_${props.work.studentName || '同学'}_${props.work.title || '作品'}.png`
  a.click()
}
</script>
