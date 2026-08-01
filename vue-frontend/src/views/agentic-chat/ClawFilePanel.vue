<script setup lang="ts">
// Claw-like 文件面板：scope=workspace 管理当前会话工作区（相对路径），scope=server 浏览服务器文件系统（绝对路径，需完全访问级别）
import { ref, computed, watch, onMounted } from 'vue'
import {
  ReloadOutlined,
  UploadOutlined,
  FolderOutlined,
  FileOutlined,
  DownloadOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { ui } from '@/utils/ui'
import {
  listClawFilesApi,
  downloadClawFileApi,
  uploadClawFileApi,
  renameClawFileApi,
  deleteClawFileApi,
  listClawServerFilesApi,
  downloadClawServerFileApi,
  uploadClawServerFileApi,
  renameClawServerFileApi,
  deleteClawServerFileApi,
} from '@/api/apiChat'
import type { ClawFileEntry } from '@/types/chat'

const props = defineProps<{ scope: 'workspace' | 'server' }>()

const chatStore = useChatStore()

/** 根目录：工作区为空字符串（相对路径），服务器为 /（绝对路径） */
const ROOT = props.scope === 'workspace' ? '' : '/'

const currentPath = ref(ROOT)
const entries = ref<ClawFileEntry[]>([])
const loading = ref(false)
const uploading = ref(false)

/** 工作区 scope 依赖当前激活 tab 的 sessionId（可选：工作区按用户隔离，无会话时也可访问） */
const sessionId = computed(() => chatStore.currentSessionId)

const isRoot = computed(() => currentPath.value === ROOT)
const displayPath = computed(() =>
  props.scope === 'workspace' ? `/${currentPath.value}` : currentPath.value,
)

/** 拼接子路径 */
function joinPath(dir: string, name: string): string {
  if (props.scope === 'workspace') return dir ? `${dir}/${name}` : name
  return dir === '/' ? `/${name}` : `${dir}/${name}`
}

/** 父目录路径 */
function parentPath(dir: string): string {
  const idx = dir.lastIndexOf('/')
  if (idx <= 0) return ROOT
  return dir.slice(0, idx)
}

function fullPath(entry: ClawFileEntry): string {
  return joinPath(currentPath.value, entry.name)
}

/** 自增请求序号：快速切换目录时丢弃过期响应，避免乱序渲染 */
let loadSeq = 0

async function loadFiles() {
  const seq = ++loadSeq
  loading.value = true
  try {
    const res =
      props.scope === 'workspace'
        ? await listClawFilesApi(sessionId.value || undefined, currentPath.value)
        : await listClawServerFilesApi(currentPath.value)
    if (seq !== loadSeq) return
    // 目录在上、文件在下，同类按名称排序
    entries.value = [...(res.data.data || [])].sort(
      (a, b) => Number(b.isDir) - Number(a.isDir) || a.name.localeCompare(b.name),
    )
  } catch {
    if (seq === loadSeq) entries.value = []
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function enterDir(entry: ClawFileEntry) {
  if (!entry.isDir) return
  currentPath.value = fullPath(entry)
  loadFiles()
}

function goUp() {
  if (isRoot.value) return
  currentPath.value = parentPath(currentPath.value)
  loadFiles()
}

// ===== 下载（blob → a[download]） =====
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

async function handleDownload(entry: ClawFileEntry) {
  try {
    const res =
      props.scope === 'workspace'
        ? await downloadClawFileApi(sessionId.value || undefined, fullPath(entry))
        : await downloadClawServerFileApi(fullPath(entry))
    triggerDownload(res.data, entry.name)
  } catch {
    // 拦截器已提示错误
  }
}

// ===== 重命名 =====
const renameOpen = ref(false)
const renameLoading = ref(false)
const renameTarget = ref<ClawFileEntry | null>(null)
const renameNewName = ref('')

function openRename(entry: ClawFileEntry) {
  renameTarget.value = entry
  renameNewName.value = entry.name
  renameOpen.value = true
}

async function handleRename() {
  const target = renameTarget.value
  const newName = renameNewName.value.trim()
  if (!target || newName === target.name) {
    renameOpen.value = false
    return
  }
  // 前端校验：空名称或含路径分隔符/上级目录引用时不提交
  if (!newName || newName.includes('/') || newName.includes('\\') || newName.includes('..')) {
    ui.error('名称不能为空，且不能包含 /、\\ 或 ..')
    return
  }
  renameLoading.value = true
  try {
    if (props.scope === 'workspace') {
      await renameClawFileApi({ sessionId: sessionId.value ?? undefined, path: fullPath(target), newName })
    } else {
      await renameClawServerFileApi({ path: fullPath(target), newName })
    }
    ui.success('重命名成功')
    renameOpen.value = false
    loadFiles()
  } catch {
    // 拦截器已提示错误
  } finally {
    renameLoading.value = false
  }
}

// ===== 删除 =====
async function handleDelete(entry: ClawFileEntry) {
  try {
    await ui.confirm({
      title: '确认删除',
      content: entry.isDir
        ? `确定删除目录「${entry.name}」及其全部内容吗？此操作不可恢复。`
        : `确定删除文件「${entry.name}」吗？此操作不可恢复。`,
      okText: '删除',
      type: 'error',
    })
  } catch {
    return
  }
  try {
    if (props.scope === 'workspace') {
      await deleteClawFileApi({ sessionId: sessionId.value ?? undefined, path: fullPath(entry) })
    } else {
      await deleteClawServerFileApi({ path: fullPath(entry) })
    }
    ui.success('已删除')
    loadFiles()
  } catch {
    // 拦截器已提示错误
  }
}

// ===== 上传 =====
const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''
  if (!file) return
  uploading.value = true
  try {
    if (props.scope === 'workspace') {
      await uploadClawFileApi(sessionId.value || undefined, currentPath.value, file)
    } else {
      await uploadClawServerFileApi(currentPath.value, file)
    }
    ui.success(`已上传 ${file.name}`)
    loadFiles()
  } catch {
    // 拦截器已提示错误
  } finally {
    uploading.value = false
  }
}

function formatSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

// 会话变化（含新会话产生 sessionId）时回到根目录并刷新（仅 workspace scope 依赖会话）
watch(sessionId, () => {
  if (props.scope !== 'workspace') return
  currentPath.value = ROOT
  loadFiles()
})

onMounted(loadFiles)
</script>

<template>
  <div class="claw-file-panel">
    <div class="claw-file-panel__toolbar">
      <button class="claw-file-panel__icon-btn" title="刷新" @click="loadFiles">
        <ReloadOutlined :spin="loading" />
      </button>
      <button class="claw-file-panel__icon-btn" title="上传到当前目录" :disabled="uploading" @click="triggerUpload">
        <UploadOutlined />
      </button>
      <span class="claw-file-panel__path" :title="displayPath">{{ displayPath }}</span>
      <input ref="fileInputRef" type="file" style="display: none" @change="handleFileUpload" />
    </div>

    <div class="claw-file-panel__list">
      <div v-if="loading && entries.length === 0" class="claw-file-panel__hint">加载中...</div>
      <template v-else>
        <div v-if="!isRoot" class="claw-file-panel__item" @click="goUp">
          <FolderOutlined class="claw-file-panel__item-icon" />
          <span class="claw-file-panel__item-name">..</span>
        </div>
        <div
          v-for="entry in entries"
          :key="entry.name"
          class="claw-file-panel__item"
          :class="{ 'claw-file-panel__item--dir': entry.isDir }"
          @click="enterDir(entry)"
        >
          <FolderOutlined v-if="entry.isDir" class="claw-file-panel__item-icon" />
          <FileOutlined v-else class="claw-file-panel__item-icon claw-file-panel__item-icon--file" />
          <span class="claw-file-panel__item-name" :title="entry.name">{{ entry.name }}</span>
          <span v-if="!entry.isDir" class="claw-file-panel__item-size">{{ formatSize(entry.size) }}</span>
          <span class="claw-file-panel__item-actions">
            <button
              v-if="!entry.isDir"
              class="claw-file-panel__action-btn"
              title="下载"
              @click.stop="handleDownload(entry)"
            >
              <DownloadOutlined />
            </button>
            <button class="claw-file-panel__action-btn" title="重命名" @click.stop="openRename(entry)">
              <EditOutlined />
            </button>
            <button
              class="claw-file-panel__action-btn claw-file-panel__action-btn--danger"
              title="删除"
              @click.stop="handleDelete(entry)"
            >
              <DeleteOutlined />
            </button>
          </span>
        </div>
        <div v-if="entries.length === 0" class="claw-file-panel__hint">当前目录为空</div>
      </template>
    </div>

    <a-modal
      v-model:open="renameOpen"
      title="重命名"
      :confirm-loading="renameLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleRename"
    >
      <a-input v-model:value="renameNewName" placeholder="请输入新名称" @pressEnter="handleRename" />
    </a-modal>
  </div>
</template>

<style scoped>
.claw-file-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  font-size: 13px;
}

.claw-file-panel__toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.claw-file-panel__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #8c8c8c;
  font-size: 14px;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
  padding: 0;
}

.claw-file-panel__icon-btn:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.06);
  color: #1677ff;
}

.claw-file-panel__icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.claw-file-panel__path {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  direction: rtl;
  text-align: left;
}

.claw-file-panel__list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.claw-file-panel__hint {
  padding: 32px 16px;
  text-align: center;
  color: #bfbfbf;
  font-size: 13px;
}

.claw-file-panel__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  font-size: 13px;
  color: #262626;
  border-bottom: 1px solid #fafafa;
}

.claw-file-panel__item--dir {
  cursor: pointer;
}

.claw-file-panel__item:hover {
  background: #f5f5f5;
}

.claw-file-panel__item-icon {
  color: #fa8c16;
  font-size: 14px;
  flex-shrink: 0;
}

.claw-file-panel__item-icon--file {
  color: #8c8c8c;
}

.claw-file-panel__item-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.claw-file-panel__item-size {
  color: #bfbfbf;
  font-size: 11px;
  flex-shrink: 0;
}

.claw-file-panel__item-actions {
  display: none;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.claw-file-panel__item:hover .claw-file-panel__item-actions {
  display: inline-flex;
}

.claw-file-panel__action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #8c8c8c;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
  padding: 0;
}

.claw-file-panel__action-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1677ff;
}

.claw-file-panel__action-btn--danger:hover {
  color: #ff4d4f;
}
</style>
