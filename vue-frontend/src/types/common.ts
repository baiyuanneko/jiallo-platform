// 通用类型定义

/**
 * 分页结果通用结构
 * 从 admin.ts 提取到公共位置，供多处复用
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}
