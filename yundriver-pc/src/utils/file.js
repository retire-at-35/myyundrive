// 格式化文件大小
export function formatFileSize(size) {
  if (!size) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }
  
  return `${size.toFixed(2)} ${units[index]}`
}

// 判断是否为文档文件
export function isDocumentFile(fileName) {
  const docExtensions = ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.pdf', '.txt']
  const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  return docExtensions.includes(extension)
}

// 判断是否为图片文件
export function isImageFile(fileName) {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  return imageExtensions.includes(extension)
} 