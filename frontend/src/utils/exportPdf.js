import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

/**
 * 将指定 DOM 元素导出为 PDF
 * @param {string|HTMLElement} selector — CSS 选择器或 DOM 元素
 * @param {string} filename — 文件名（不含扩展名）
 * @param {object} options
 * @param {number} options.scale — 渲染倍率，默认 2（高清）
 * @param {string} options.format — 纸张格式，默认 'a4'
 * @param {string} options.orientation — 方向，默认 'portrait'
 */
export async function exportPdf(selector, filename, options = {}) {
  const el = typeof selector === 'string' ? document.querySelector(selector) : selector
  if (!el) {
    console.warn('[exportPdf] element not found:', selector)
    return
  }

  const scale = options.scale || 2
  const format = options.format || 'a4'
  const orientation = options.orientation || 'portrait'

  const canvas = await html2canvas(el, {
    scale,
    useCORS: true,
    backgroundColor: '#ffffff',
    logging: false
  })

  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF({ orientation, unit: 'mm', format })

  const pdfWidth = pdf.internal.pageSize.getWidth()
  const pdfHeight = pdf.internal.pageSize.getHeight()
  const imgWidth = pdfWidth - 20  // 10mm margins on each side
  const imgHeight = (canvas.height * imgWidth) / canvas.width

  // If content exceeds one page, split across pages
  if (imgHeight <= pdfHeight - 20) {
    // Single page
    pdf.addImage(imgData, 'PNG', 10, 10, imgWidth, imgHeight)
  } else {
    // Multi-page: draw in strips
    let remainingHeight = canvas.height
    let pageOffset = 0
    let isFirstPage = true

    while (remainingHeight > 0) {
      if (!isFirstPage) pdf.addPage()

      // How much of the canvas fits on this page
      const pageCanvasHeight = Math.min(remainingHeight, (pdfHeight - 20) * canvas.width / imgWidth)
      const pageCanvas = document.createElement('canvas')
      pageCanvas.width = canvas.width
      pageCanvas.height = pageCanvasHeight
      const ctx = pageCanvas.getContext('2d')
      // Draw the slice of the original canvas
      ctx.drawImage(canvas, 0, pageOffset, canvas.width, pageCanvasHeight, 0, 0, canvas.width, pageCanvasHeight)

      const pageImgData = pageCanvas.toDataURL('image/png')
      const pageImgHeight = pdfHeight - 20
      pdf.addImage(pageImgData, 'PNG', 10, 10, imgWidth, pageImgHeight)

      pageOffset += pageCanvasHeight
      remainingHeight -= pageCanvasHeight
      isFirstPage = false
    }
  }

  pdf.save(`${filename}.pdf`)
}
