<script setup lang="ts">
import { ref, watch } from 'vue'
import { DownloadOutlined, FolderOutlined, FileOutlined } from '@ant-design/icons-vue'
import { usePyodide } from '@/composables/usePyodide'
import type { FileTabType } from '@/stores/fileSidebar'

const props = defineProps<{
  tabType: FileTabType
}>()

const pyodideCtx = usePyodide()

interface TreeFileNode {
  title: string
  key: string
  isLeaf?: boolean
  path: string
  children?: TreeFileNode[]
}

const treeData = ref<TreeFileNode[]>([])
const expandedKeys = ref<string[]>([])
const loading = ref(false)

function findNode(nodes: TreeFileNode[], key: string): TreeFileNode | undefined {
  for (const n of nodes) {
    if (n.key === key) return n
    if (n.children) {
      const found = findNode(n.children, key)
      if (found) return found
    }
  }
  return undefined
}

function refreshTree() {
  treeData.value = [...treeData.value]
}

// ==================== Native FS ====================

async function loadNativeFSChildren(node: TreeFileNode): Promise<void> {
  const handle = pyodideCtx.workspaceHandle.value
  if (!handle) return

  let dirHandle: FileSystemDirectoryHandle
  if (node.path === '/') {
    dirHandle = handle
  } else {
    let current = handle
    const parts = node.path.replace(/^\/|\/$/g, '').split('/').filter(Boolean)
    for (const part of parts) {
      current = await current.getDirectoryHandle(part)
    }
    dirHandle = current
  }

  const children: TreeFileNode[] = []
  for await (const entry of (dirHandle as any).values()) {
    if (entry.name.startsWith('.')) continue
    const childPath = node.path === '/' ? `/${entry.name}` : `${node.path}/${entry.name}`
    children.push({
      title: entry.name,
      key: childPath,
      isLeaf: entry.kind === 'file',
      path: childPath,
    })
  }
  children.sort((a, b) => {
    if (a.isLeaf !== b.isLeaf) return a.isLeaf ? 1 : -1
    return a.title.localeCompare(b.title)
  })
  node.children = children
  refreshTree()
}

// ==================== Pyodide FS ====================

async function loadPyodideFSChildren(node: TreeFileNode): Promise<void> {
  const py = pyodideCtx.pyodideInstance.value
  if (!py) return

  const path = node.path
  let items: string[]
  try {
    items = py.FS.readdir(path).filter(
      (name: string) => name !== '.' && name !== '..' && !name.startsWith('.'),
    )
  } catch {
    node.children = [{ title: '无法读取目录', key: `${path}/_error`, isLeaf: true, path: '' }]
    refreshTree()
    return
  }

  const children: TreeFileNode[] = []
  for (const name of items) {
    const childPath = path === '/' ? `/${name}` : `${path}/${name}`
    try {
      const stat = py.FS.stat(childPath)
      children.push({
        title: name,
        key: childPath,
        isLeaf: !py.FS.isDir(stat.mode),
        path: childPath,
      })
    } catch {
      // skip
    }
  }
  children.sort((a, b) => {
    if (a.isLeaf !== b.isLeaf) return a.isLeaf ? 1 : -1
    return a.title.localeCompare(b.title)
  })
  node.children = children
  refreshTree()
}

// ==================== Init ====================

async function initPyodideTree(): Promise<TreeFileNode[]> {
  if (!pyodideCtx.pyodideInstance.value) {
    try {
      await pyodideCtx.initPyodide()
    } catch {
      return [{ title: 'Pyodide 初始化失败', key: 'no-pyodide', isLeaf: true, path: '' }]
    }
  }
  const py = pyodideCtx.pyodideInstance.value!
  try {
    py.FS.readdir('/')
  } catch {
    return [{ title: '无法读取文件系统', key: 'fs-error', isLeaf: true, path: '' }]
  }
  const root: TreeFileNode = { title: '/', key: '/', isLeaf: false, path: '/' }
  await loadPyodideFSChildren(root)
  return [root]
}

async function initNativeFSTree(): Promise<TreeFileNode[]> {
  const handle = pyodideCtx.workspaceHandle.value
  if (!handle) {
    return [{ title: '未挂载工作区', key: 'no-workspace', isLeaf: true, path: '' }]
  }
  const root: TreeFileNode = { title: handle.name, key: '/', isLeaf: false, path: '/' }
  await loadNativeFSChildren(root)
  return [root]
}

async function initTree() {
  loading.value = true
  try {
    if (props.tabType === 'pyodide_env') {
      treeData.value = await initPyodideTree()
    } else if (props.tabType === 'mounted_fs') {
      treeData.value = await initNativeFSTree()
    }
    if (treeData.value.length > 0 && !treeData.value[0]!.isLeaf) {
      expandedKeys.value = ['/']
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.tabType, () => { treeData.value = []; expandedKeys.value = []; initTree() }, { immediate: true })

// Auto-refresh after Pyodide code execution
watch(() => pyodideCtx.fsChangeVersion.value, () => {
  if (props.tabType === 'pyodide_env' && treeData.value.length > 0) {
    initTree()
  }
})

// ==================== Expand handler ====================

function handleExpand(keys: string[]) {
  const newlyExpanded = keys.find((k) => !expandedKeys.value.includes(k))
  expandedKeys.value = keys
  if (newlyExpanded) {
    const node = findNode(treeData.value, newlyExpanded)
    if (node && !node.children && !node.isLeaf) {
      if (props.tabType === 'mounted_fs') loadNativeFSChildren(node)
      else if (props.tabType === 'pyodide_env') loadPyodideFSChildren(node)
    }
  }
}

// ==================== Download ====================

async function handleDownload(node: TreeFileNode): Promise<void> {
  if (node.isLeaf !== true) return
  try {
    let blob: Blob
    if (props.tabType === 'mounted_fs') {
      blob = await downloadFromNativeFS(node.path)
    } else {
      blob = await downloadFromPyodideFS(node.path)
    }
    triggerDownload(blob, node.title)
  } catch (e) {
    console.error('下载失败:', e)
  }
}

async function downloadFromNativeFS(filePath: string): Promise<Blob> {
  const handle = pyodideCtx.workspaceHandle.value!
  let current = handle
  const parts = filePath.replace(/^\/+|\/+$/g, '').split('/')
  const fileName = parts.pop()!
  for (const part of parts) {
    current = await current.getDirectoryHandle(part)
  }
  const fileHandle = await current.getFileHandle(fileName)
  const file = await fileHandle.getFile()
  return file
}

async function downloadFromPyodideFS(filePath: string): Promise<Blob> {
  const py = pyodideCtx.pyodideInstance.value!
  const data = py.FS.readFile(filePath) as Uint8Array<ArrayBuffer>
  return new Blob([data])
}

function triggerDownload(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="file-tree">
    <div v-if="loading" class="file-tree__loading">加载中...</div>
    <a-tree
      v-else
      :expanded-keys="expandedKeys"
      :tree-data="treeData"
      :field-names="{ children: 'children', title: 'title', key: 'key' }"
      block-node
      @expand="handleExpand"
    >
      <template #title="nodeData">
        <span class="file-tree__node-title">
          <FolderOutlined v-if="!nodeData.isLeaf" class="file-tree__node-icon" />
          <FileOutlined v-else class="file-tree__node-icon" />
          <span class="file-tree__node-name">{{ nodeData.title }}</span>
          <button
            v-if="nodeData.isLeaf"
            class="file-tree__download-btn"
            title="下载"
            @click.stop="handleDownload(nodeData)"
          >
            <DownloadOutlined />
          </button>
        </span>
      </template>
    </a-tree>
  </div>
</template>

<style scoped>
.file-tree {
  padding: 4px 0;
  font-size: 13px;
}

.file-tree__loading {
  padding: 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 13px;
}

.file-tree__node-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  line-height: 1.6;
}

.file-tree__node-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: #8c8c8c;
}

.file-tree__node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-tree__download-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #1677ff;
  font-size: 13px;
  flex-shrink: 0;
  padding: 0;
  transition: background 0.15s;
}

.file-tree__node-title:hover .file-tree__download-btn {
  display: inline-flex;
}

.file-tree__download-btn:hover {
  background: #e6f4ff;
}
</style>
