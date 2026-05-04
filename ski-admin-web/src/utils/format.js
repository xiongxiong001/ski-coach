import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDateTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDate(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD')
}

export function formatRelativeTime(time) {
  if (!time) return '-'
  return dayjs(time).fromNow()
}

export function formatNumber(num) {
  if (num == null) return '-'
  return num.toLocaleString('zh-CN')
}

export function formatYuan(num) {
  if (num == null) return '¥0.00'
  return '¥' + Number(num).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 10
  })
}

/** 字节大小:1024 -> 1 KB */
export function formatFileSize(bytes) {
  if (bytes === 0 || bytes == null) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/** 百分比 */
export function formatPercent(num, digits = 1) {
  if (num == null) return '-'
  return (num * 100).toFixed(digits) + '%'
}