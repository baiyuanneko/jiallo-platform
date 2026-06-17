<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CloseOutlined, ReloadOutlined, LinkOutlined, UploadOutlined, DatabaseOutlined } from '@ant-design/icons-vue'
import { useFileSidebarStore } from '@/stores/fileSidebar'
import { useChatStore } from '@/stores/chat'
import { usePyodide } from '@/composables/usePyodide'
import { ui } from '@/utils/ui'
import FileTree from './FileTree.vue'

const store = useFileSidebarStore()
const chatStore = useChatStore()
const pyodide = usePyodide()

type ViewType = 'python_env' | 'jiallo_kb' | 'rag_references'

const selectedView = ref<ViewType>('python_env')
const refreshCounter = ref(0)

const isPyodideEnabled = computed(() =>
  chatStore.enabledTools.includes('pyodide_code_runner'),
)

const isMounted = computed(() => !!pyodide.workspaceName.value)

const pythonEnvLabel = computed(() => {
  if (!isPyodideEnabled.value) return '[Python 环境]: 未启用'
  return isMounted.value
    ? `[Python 环境]: 已挂载本机目录 ${pyodide.workspaceName.value}`
    : '[Python 环境]: 沙盒文件系统'
})

watch(() => chatStore.selectedRagChunk, (chunk) => {
  if (chunk) {
    selectedView.value = 'jiallo_kb'
  }
})

const treeTabType = computed(() =>
  isMounted.value ? ('mounted_fs' as const) : ('pyodide_env' as const),
)

const ragLabel = computed(() => {
  const chunk = chatStore.selectedRagChunk
  return chunk ? `[RAG 知识分块]: ${chunk.fileName}` : '[RAG 知识分块]: 未打开文件'
})

const ragRefLabel = computed(() => {
  const refs = chatStore.selectedRagReferences
  return refs ? `[会话 RAG 引用列表]: Turn #${refs.turnIndex + 1}` : '[会话 RAG 引用列表]: 未选择对话'
})

watch(() => chatStore.selectedRagReferences, (refs) => {
  if (refs) {
    selectedView.value = 'rag_references'
  }
})

const viewOptions = computed(() => [
  { value: 'python_env' as ViewType, label: pythonEnvLabel.value },
  { value: 'jiallo_kb' as ViewType, label: ragLabel.value },
  { value: 'rag_references' as ViewType, label: ragRefLabel.value },
])

function handleRefresh() {
  refreshCounter.value++
}

async function handleSelectWorkspace() {
  if (!pyodide.supportsFileSystemAccess) {
    ui.info('当前浏览器不支持 File System Access API，无法挂载本机目录。推荐使用 Chrome 浏览器。')
    return
  }
  try {
    await pyodide.selectWorkspace()
  } catch {
    // user cancelled
  }
}

const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  target.value = ''

  const py = pyodide.pyodideInstance.value
  if (!py) {
    ui.error('Pyodide 未初始化')
    return
  }

  try {
    const buffer = await file.arrayBuffer()
    const data = new Uint8Array(buffer)
    py.FS.writeFile(`/${file.name}`, data)
    ui.success(`已上传 ${file.name}`)
    refreshCounter.value++
  } catch (err) {
    ui.error('上传失败')
    console.error(err)
  }
}
</script>

<template>
  <div class="file-sidebar">
    <div class="file-sidebar__header">
      <span class="file-sidebar__title">可观测性（文件、任务、中间过程等）</span>
      <button class="file-sidebar__close-btn" title="关闭侧边栏" @click="store.close()">
        <CloseOutlined />
      </button>
    </div>

    <div class="file-sidebar__selector">
      <a-select
        v-model:value="selectedView"
        class="file-sidebar__select"
        :options="viewOptions"
      />
      <button
        v-if="selectedView === 'python_env' && isPyodideEnabled"
        class="file-sidebar__refresh-btn"
        title="刷新"
        @click="handleRefresh"
      >
        <ReloadOutlined />
      </button>
    </div>

    <button
      v-if="isPyodideEnabled && selectedView === 'python_env'"
      class="file-sidebar__mount-btn"
      :class="{ 'file-sidebar__mount-btn--mounted': isMounted }"
      @click="isMounted ? (pyodide.unmountWorkspace(), refreshCounter++) : handleSelectWorkspace()"
    >
      <LinkOutlined />
      <span class="file-sidebar__mount-label">{{ isMounted ? `取消挂载（${pyodide.workspaceName.value}）` : '连接到本机真实文件系统' }}</span>
    </button>
    <div
      v-if="isPyodideEnabled && selectedView === 'python_env' && !isMounted"
      class="file-sidebar__mount-hint"
    >
      Python 沙盒环境中的内容将在浏览器刷新或切换挂载选项后清除，请及时下载，或挂载本机真实文件系统以保留。
    </div>

    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="handleFileUpload"
    />
    <button
      v-if="isPyodideEnabled && selectedView === 'python_env'"
      class="file-sidebar__upload-btn"
      @click="triggerUpload"
    >
      <UploadOutlined />
      <span>上传文件到沙盒</span>
    </button>

    <div class="file-sidebar__content">
      <div v-if="selectedView === 'jiallo_kb'" class="file-sidebar__rag-chunk">
        <template v-if="chatStore.selectedRagChunk">
          <div class="rag-chunk__header">
            <a-tag color="blue">{{ chatStore.selectedRagChunk.libraryName }}</a-tag>
            <span class="rag-chunk__file">{{ chatStore.selectedRagChunk.fileName }}</span>
            <span class="rag-chunk__relevance">相关度 {{ chatStore.selectedRagChunk.relevance.toFixed(4) }}</span>
          </div>
          <div class="rag-chunk__content">{{ chatStore.selectedRagChunk.chunkContent }}</div>
        </template>
        <template v-else>
          <div class="file-sidebar__coming-soon">
            <DatabaseOutlined class="file-sidebar__coming-soon-icon" />
            <span>在对话中点击 RAG 检索结果卡片查看分块内容</span>
          </div>
        </template>
      </div>
      <div v-else-if="selectedView === 'rag_references'" class="file-sidebar__rag-references">
        <template v-if="chatStore.selectedRagReferences && chatStore.selectedRagReferences.items.length > 0">
          <div class="rag-ref-header">Turn #{{ chatStore.selectedRagReferences.turnIndex + 1 }} 的 RAG 引用</div>
          <div class="rag-ref-list">
          <div v-for="(item, idx) in chatStore.selectedRagReferences.items" :key="idx" class="rag-ref-card" @click="chatStore.selectedRagChunk = item">
            <div class="rag-ref-card__header">
              <a-tag color="blue" style="font-size: 11px; line-height: 1.2;">{{ item.libraryName }}</a-tag>
              <span class="rag-ref-card__file">{{ item.fileName }}</span>
            </div>
            <div class="rag-ref-card__content">{{ item.chunkContent }}</div>
            <div class="rag-ref-card__footer">
              <span>#{{ idx + 1 }}</span>
              <span>相关度 {{ item.relevance.toFixed(4) }}</span>
            </div>
          </div>
          </div>
        </template>
        <template v-else>
          <div class="file-sidebar__coming-soon">
            <DatabaseOutlined class="file-sidebar__coming-soon-icon" />
            <span>在对话中点击消息底部的「N 引用」查看</span>
          </div>
        </template>
      </div>
      <div v-else-if="!isPyodideEnabled" class="file-sidebar__disabled">
        请在对话中启用 Python 执行工具以查看文件系统
      </div>
      <FileTree
        v-else
        :key="`${selectedView}-${refreshCounter}`"
        :tab-type="treeTabType"
      />
    </div>
  </div>
</template>

<style scoped>
.file-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  font-size: 13px;
}

.file-sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.file-sidebar__title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.file-sidebar__close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #595959;
  font-size: 14px;
  transition: background 0.15s, color 0.15s;
  padding: 0;
  flex-shrink: 0;
}

.file-sidebar__close-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #262626;
}

.file-sidebar__mount-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 0 12px 6px;
  padding: 9px 12px;
  border: 2px dashed #bfbfbf;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.file-sidebar__mount-btn:hover {
  border-color: #4096ff;
  border-style: dashed;
  color: #1677ff;
  background: #e6f4ff;
}

.file-sidebar__mount-btn--mounted {
  border-style: solid;
  border-color: #91caff;
  background: #e6f4ff;
  color: #1677ff;
}

.file-sidebar__mount-btn--mounted:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
  background: #fff1f0;
  border-style: solid;
}

.file-sidebar__mount-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.file-sidebar__selector {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.file-sidebar__select {
  flex: 1;
  min-width: 0;
}

.file-sidebar__select :deep(.ant-select-selector) {
  font-size: 13px !important;
  border-radius: 6px !important;
}

.file-sidebar__refresh-btn {
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

.file-sidebar__refresh-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1677ff;
}

.file-sidebar__content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.file-sidebar__rag-chunk {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.rag-chunk__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.rag-chunk__file {
  color: #595959;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rag-chunk__relevance {
  margin-left: auto;
  color: #8c8c8c;
  font-size: 11px;
  white-space: nowrap;
}

.rag-chunk__content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  font-size: 13px;
  color: #262626;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.file-sidebar__rag-references {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.rag-ref-list {
  flex: 1;
  overflow-y: auto;
}

.rag-ref-header {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.rag-ref-card {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.rag-ref-card:hover {
  background: #f5f5f5;
}

.rag-ref-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 4px;
}

.rag-ref-card__file {
  color: #595959;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rag-ref-card__content {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.rag-ref-card__footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #bfbfbf;
}

.file-sidebar__coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  color: #8c8c8c;
  font-size: 13px;
  text-align: center;
}

.file-sidebar__coming-soon-icon {
  font-size: 32px;
  color: #d9d9d9;
}

.file-sidebar__mount-hint {
  margin: 0 12px 6px;
  padding: 0 2px;
  font-size: 11px;
  line-height: 1.5;
  color: #8c8c8c;
}

.file-sidebar__upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0 12px 8px;
  padding: 8px 12px;
  border: 2px dashed #bfbfbf;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.file-sidebar__upload-btn:hover {
  border-color: #4096ff;
  color: #1677ff;
  background: #e6f4ff;
}

.file-sidebar__disabled {
  padding: 32px 16px;
  text-align: center;
  color: #bfbfbf;
  font-size: 13px;
  line-height: 1.6;
}
</style>
