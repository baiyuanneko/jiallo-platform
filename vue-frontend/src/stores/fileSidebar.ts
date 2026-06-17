// 右侧「文件与任务」面板状态管理

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type FileTabType = 'mounted_fs' | 'pyodide_env' | 'jiallo_kb'

export interface FileTab {
  id: string
  type: FileTabType
  title: string
}

let tabCounter = 0

const TAB_TITLES: Record<FileTabType, string> = {
  mounted_fs: '已挂载的真实文件系统',
  pyodide_env: 'Pyodide 虚拟环境',
  jiallo_kb: 'Jiallo 知识库模块',
}

export const useFileSidebarStore = defineStore('fileSidebar', () => {
  const visible = ref(true)
  const panelWidth = ref(380)
  const tabs = ref<FileTab[]>([])
  const activeTabId = ref<string | null>(null)

  const activeTab = computed(() => tabs.value.find((t) => t.id === activeTabId.value))

  function toggle() {
    visible.value = !visible.value
  }

  function open() {
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  function addTab(type: FileTabType) {
    const id = `file-tab-${++tabCounter}`
    const tab: FileTab = { id, type, title: TAB_TITLES[type] }
    tabs.value.push(tab)
    activeTabId.value = id
    visible.value = true
  }

  function closeTab(id: string) {
    const idx = tabs.value.findIndex((t) => t.id === id)
    if (idx === -1) return
    tabs.value.splice(idx, 1)
    if (tabs.value.length === 0) {
      activeTabId.value = null
      close()
    } else if (activeTabId.value === id) {
      activeTabId.value = tabs.value[Math.min(idx, tabs.value.length - 1)]!.id
    }
  }

  function switchTab(id: string) {
    activeTabId.value = id
  }

  return {
    visible,
    panelWidth,
    tabs,
    activeTabId,
    activeTab,
    toggle,
    open,
    close,
    addTab,
    closeTab,
    switchTab,
  }
})
