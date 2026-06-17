// 背景图片配置

export const BACKGROUNDS = [
  '/images/backgrounds/project_bynidentity_bg1.jpg',
  '/images/backgrounds/project_bynidentity_bg2.jpg',
  '/images/backgrounds/project_bynidentity_bg3.jpg',
  '/images/backgrounds/project_bynidentity_bg4.jpg',
]

/**
 * 获取随机背景图
 * @param exclude 要排除的图片路径
 */
export function getRandomBackground(exclude?: string): string {
  const available = exclude ? BACKGROUNDS.filter((bg) => bg !== exclude) : BACKGROUNDS

  // 如果过滤后没有可用背景，返回第一个背景
  if (available.length === 0) {
    return BACKGROUNDS[0]!
  }

  return available[Math.floor(Math.random() * available.length)]!
}
