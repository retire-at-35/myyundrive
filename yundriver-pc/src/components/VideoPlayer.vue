<template>
  <div class="video-container">
    <video
      ref="videoPlayer"
      class="video-player"
      controls
    >
      您的浏览器不支持视频播放
    </video>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Hls from 'hls.js'
import { ElMessage } from 'element-plus'

const props = defineProps({
  fileId: {
    type: String,
    required: true
  },
  userId: {
    type: String,
    required: true
  }
})

const videoPlayer = ref(null)
let hls = null

onMounted(() => {
  const video = videoPlayer.value

  if (Hls.isSupported()) {
    try {
      hls = new Hls({
        xhrSetup: (xhr, url) => {
          // 检查是否是 ts 文件请求
          if (url.endsWith('.ts')) {
            // 从 URL 中提取文件名
            const tsFileName = url.split('/').pop()
            // 构建新的 URL，使用与 m3u8 相同的接口
            const newUrl = `${import.meta.env.VITE_API_URL}/api/file/ts/getVideoInfo?fileId=${tsFileName}&userId=${props.userId}`
            // 使用新的 URL 打开请求
            xhr.open('GET', newUrl, true)
            // 添加 withCredentials 设置
            xhr.withCredentials = true
          } else {
            // 非 ts 文件请求保持原样
            xhr.withCredentials = true
          }
        }
      })

      // 错误处理
      hls.on(Hls.Events.ERROR, (event, data) => {
        // 只记录致命错误
        if (data.fatal) {
          console.error('HLS Error:', event, data)
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad()
              break
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError()
              break
            default:
              hls.destroy()
              break
          }
        }
        // 非致命错误不需要处理和显示
      })

      // 构建 m3u8 URL
      const m3u8Url = `${import.meta.env.VITE_API_URL}/api/file/ts/getVideoInfo?fileId=${props.fileId}&userId=${props.userId}`
      console.log('Loading m3u8:', m3u8Url)
      
      hls.loadSource(m3u8Url)
      hls.attachMedia(video)

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(error => {
          console.warn('自动播放失败:', error)
        })
      })

    } catch (error) {
      console.error('视频初始化失败:', error)
      ElMessage.error('视频加载失败')
    }
  } else {
    ElMessage.warning('您的浏览器不支持视频播放')
  }
})

onBeforeUnmount(() => {
  if (hls) {
    hls.destroy()
  }
})
</script>

<style scoped>
.video-container {
  width: 100%;
  height: 100%;
  background: #000;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style> 