import { reactive } from 'vue'

/**
 * 共享的折叠/展开状态存储。
 * key 由父组件（ChatMessageList）在模板中计算，确保流式→常规切换前后同一个逻辑块的 key 一致。
 * 组件销毁重建后仍能恢复展开状态。
 */
export const expandedBlocks = reactive(new Map<string, boolean>())

/**
 * 简化的模型中间过程显示中，用户点击"查看详情"后展开完整显示的状态。
 * key: assistant turn 的 firstMessageId
 */
export const expandedDetailViews = reactive(new Map<string, boolean>())
