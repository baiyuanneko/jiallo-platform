import { ref, onMounted } from 'vue'
import { getRandomBackground } from '@/config/backgrounds'

/**
 * 随机背景图 composable
 */
export function useBackgroundCarousel() {
  const currentBg = ref('')
  const imageLoaded = ref(false)

  onMounted(() => {
    const img = new Image()
    img.src = getRandomBackground()
    img.onload = () => {
      currentBg.value = img.src
      imageLoaded.value = true
    }
  })

  return {
    currentBg,
    imageLoaded,
  }
}
