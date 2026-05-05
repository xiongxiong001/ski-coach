/**
 * 图片压缩工具
 * 使用 Canvas API 在前端压缩图片,减少上传流量和服务器存储压力
 */

const MAX_WIDTH = 1920
const MAX_HEIGHT = 1920
const QUALITY = 0.7
const SKIP_SIZE_BYTES = 500 * 1024 // 小于500KB跳过压缩

export async function compressImage(file, maxWidth = MAX_WIDTH, maxHeight = MAX_HEIGHT, quality = QUALITY) {
  // 小文件直接返回
  if (file.size < SKIP_SIZE_BYTES) return file

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img

        // 计算缩放比例
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = Math.round(width * ratio)
          height = Math.round(height * ratio)
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob) {
              // 如果压缩后反而更大,返回原图
              if (blob.size >= file.size) {
                resolve(file)
              } else {
                resolve(blob)
              }
            } else {
              resolve(file)
            }
          },
          'image/jpeg',
          quality
        )
      }
      img.onerror = () => resolve(file)
      img.src = e.target.result
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}
