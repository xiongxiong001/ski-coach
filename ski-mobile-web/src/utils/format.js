import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDateTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

export function formatDate(time) {
  if (!time) return '-'
  return dayjs(time).format('MM-DD HH:mm')
}

export function formatRelativeTime(time) {
  if (!time) return '-'
  return dayjs(time).fromNow()
}

export function formatFileSize(bytes) {
  if (bytes === 0 || bytes == null) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export function formatDuration(seconds) {
  if (!seconds) return '-'
  const s = Math.floor(seconds)
  const minutes = Math.floor(s / 60)
  const remainSeconds = s % 60
  if (minutes === 0) return `${remainSeconds}秒`
  return `${minutes}分${remainSeconds}秒`
}

export function formatPercent(num, digits = 1) {
  if (num == null) return '-'
  return (num * 100).toFixed(digits) + '%'
}
