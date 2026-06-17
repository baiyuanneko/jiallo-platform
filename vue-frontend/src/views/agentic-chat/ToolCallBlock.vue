<script setup lang="ts">
import { computed, ref } from 'vue'
import { ToolOutlined, LoadingOutlined, AppstoreOutlined, UnorderedListOutlined, CodeOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import type { SearchResultItem, PyodideExecutionResult, RagSearchResultItem } from '@/types/chat'
import SearchResultCard from './SearchResultCard.vue'
import { usePyodide } from '@/composables/usePyodide'
import { useChatStore } from '@/stores/chat'
import { expandedBlocks } from '@/stores/blockState'

const chatStore = useChatStore()

const props = defineProps<{
  name: string
  arguments?: string
  result?: string | SearchResultItem[] | RagSearchResultItem[] | PyodideExecutionResult
  isSearching?: boolean
  toolCallId?: string
  blockKey?: string
}>()

const pyodide = usePyodide()

const resolvedBlockKey = computed(() => props.blockKey ?? `tool-${props.toolCallId ?? ''}`)

const activeKey = computed<string[]>(() => {
  if (expandedBlocks.get(resolvedBlockKey.value)) return ['1']
  return []
})

function onCollapseChange(keys: string[] | string) {
  const arr = Array.isArray(keys) ? keys : [keys]
  expandedBlocks.set(resolvedBlockKey.value, arr.includes('1'))
}

const isSearchTool = computed(() => props.name === 'bocha_web_search')
const isPyodideTool = computed(() => props.name === 'pyodide_code_runner')
const isRagSearchTool = computed(() => props.name === 'searchRagKnowledgeBase')

const searchResults = computed<SearchResultItem[]>(() => {
  if (!isSearchTool.value || !props.result) return []
  if (Array.isArray(props.result)) return props.result.filter((r): r is SearchResultItem => 'url' in r)
  // Result may be a JSON string (persisted tool response)
  if (typeof props.result === 'string') {
    try {
      const parsed = JSON.parse(props.result)
      if (Array.isArray(parsed)) return parsed as SearchResultItem[]
    } catch {
      // not valid JSON, fall through to text display
    }
  }
  return []
})

const ragSearchResults = computed<RagSearchResultItem[]>(() => {
  if (!isRagSearchTool.value || !props.result) return []
  if (Array.isArray(props.result)) return props.result as RagSearchResultItem[]
  if (typeof props.result === 'string') {
    try {
      const parsed = JSON.parse(props.result)
      if (Array.isArray(parsed)) return parsed as RagSearchResultItem[]
    } catch {
      // not valid JSON
    }
  }
  return []
})

const textResult = computed<string>(() => {
  if (!props.result) return ''
  if (typeof props.result === 'string') return props.result
  return JSON.stringify(props.result, null, 2)
})

const pyodideResult = computed<PyodideExecutionResult | null>(() => {
  if (!isPyodideTool.value || !props.result) return null
  if (typeof props.result === 'object' && !Array.isArray(props.result)) return props.result as PyodideExecutionResult
  if (typeof props.result === 'string') {
    try {
      const parsed = JSON.parse(props.result)
      if (parsed && typeof parsed === 'object' && 'stdout' in parsed) return parsed as PyodideExecutionResult
    } catch {
      // not valid JSON
    }
  }
  return null
})

const decodedArguments = computed(() => {
  if (!props.arguments) return null
  try {
    return JSON.parse(props.arguments)
  } catch {
    return null
  }
})

const displayName = computed(() => {
  const nameMap: Record<string, string> = {
    bocha_web_search: '网页搜索',
    web_fetch: '阅读网页',
    pyodide_code_runner: 'Python 执行',
    readMemory: '回忆',
    overwriteMemory: '整理记忆',
    appendMemory: '更新记忆',
    searchRagKnowledgeBase: 'RAG 知识库检索',
  }
  return nameMap[props.name] || props.name
})

type ViewMode = 'card' | 'list'
const viewMode = ref<ViewMode>('card')
</script>

<template>
  <div class="tool-call-block">
    <a-collapse :bordered="false" class="tool-call-block__collapse" :activeKey="activeKey" @change="onCollapseChange">
      <a-collapse-panel key="1">
        <template #header>
          <div class="tool-call-block__header">
            <ToolOutlined class="tool-call-block__icon" />
            <span class="tool-call-block__name">{{ displayName }}</span>
            <LoadingOutlined v-if="isSearching" class="tool-call-block__loading" />
          </div>
        </template>

        <div class="tool-call-block__content">
          <template v-if="isSearchTool && searchResults.length > 0">
            <!-- View toggle -->
            <div class="tool-call-block__view-toggle">
              <button
                class="view-toggle__btn"
                :class="{ 'view-toggle__btn--active': viewMode === 'card' }"
                @click="viewMode = 'card'"
              >
                <AppstoreOutlined />
                <span>卡片视图</span>
              </button>
              <button
                class="view-toggle__btn"
                :class="{ 'view-toggle__btn--active': viewMode === 'list' }"
                @click="viewMode = 'list'"
              >
                <UnorderedListOutlined />
                <span>列表视图</span>
              </button>
            </div>

            <!-- Card view: horizontal scroll -->
            <div v-if="viewMode === 'card'" class="tool-call-block__card-scroll">
              <SearchResultCard
                v-for="(item, idx) in searchResults"
                :key="idx"
                :item="item"
                class="tool-call-block__card-item"
              />
            </div>

            <!-- List view: vertical stack -->
            <div v-else class="tool-call-block__list">
              <SearchResultCard v-for="(item, idx) in searchResults" :key="idx" :item="item" />
            </div>
          </template>

          <template v-else-if="isRagSearchTool && ragSearchResults.length > 0">
            <!-- View toggle -->
            <div class="tool-call-block__view-toggle">
              <button
                class="view-toggle__btn"
                :class="{ 'view-toggle__btn--active': viewMode === 'card' }"
                @click="viewMode = 'card'"
              >
                <AppstoreOutlined />
                <span>卡片视图</span>
              </button>
              <button
                class="view-toggle__btn"
                :class="{ 'view-toggle__btn--active': viewMode === 'list' }"
                @click="viewMode = 'list'"
              >
                <UnorderedListOutlined />
                <span>列表视图</span>
              </button>
            </div>

            <!-- Card view: horizontal scroll -->
            <div v-if="viewMode === 'card'" class="tool-call-block__card-scroll">
              <div v-for="(item, idx) in ragSearchResults" :key="idx" class="rag-search-card rag-search-card--compact" @click="chatStore.selectedRagChunk = item">
                <div class="rag-search-card__header">
                  <a-tag color="blue" style="font-size: 11px; line-height: 1.2;">{{ item.libraryName }}</a-tag>
                  <span class="rag-search-card__file">{{ item.fileName }}</span>
                </div>
                <div class="rag-search-card__content">{{ item.chunkContent }}</div>
                <div class="rag-search-card__footer">
                  <span>#{{ idx + 1 }}</span>
                  <span>相关度 {{ item.relevance.toFixed(4) }}</span>
                </div>
              </div>
            </div>

            <!-- List view: vertical stack -->
            <div v-else class="rag-search-list">
              <div v-for="(item, idx) in ragSearchResults" :key="idx" class="rag-search-card">
                <div class="rag-search-card__header">
                  <span class="rag-search-card__index">#{{ idx + 1 }}</span>
                  <a-tag color="blue">{{ item.libraryName }}</a-tag>
                  <span class="rag-search-card__file">{{ item.fileName }}</span>
                  <span class="rag-search-card__relevance">相关度 {{ item.relevance.toFixed(4) }}</span>
                </div>
                <div class="rag-search-card__content">{{ item.chunkContent }}</div>
              </div>
            </div>
          </template>

          <template v-else-if="isPyodideTool">
            <!-- Initializing / Executing state -->
            <div v-if="isSearching" class="tool-call-block__waiting">
              <LoadingOutlined />
              <span>{{ pyodide.isLoading.value ? '正在初始化本地沙盒环境...' : '正在执行 Python 代码...' }}</span>
            </div>
            <!-- Execution result -->
            <template v-else-if="pyodideResult">
              <!-- Code display -->
              <div v-if="decodedArguments" class="pyodide-result__code">
                <div class="pyodide-result__code-header">
                  <CodeOutlined /> <span>Python</span>
                </div>
                <pre>{{ decodedArguments.code || decodedArguments }}</pre>
              </div>
              <!-- stdout -->
              <div v-if="pyodideResult.stdout" class="pyodide-result__output">
                <div class="pyodide-result__output-header">
                  <span>输出</span>
                  <span v-if="pyodideResult.exitCode === 0" class="pyodide-result__exit-code pyodide-result__exit-code--success">退出码: 0</span>
                </div>
                <pre>{{ pyodideResult.stdout }}</pre>
              </div>
              <!-- stderr -->
              <div v-if="pyodideResult.stderr" class="pyodide-result__output pyodide-result__output--stderr">
                <div class="pyodide-result__output-header"><span>标准错误</span></div>
                <pre>{{ pyodideResult.stderr }}</pre>
              </div>
              <!-- error -->
              <div v-if="pyodideResult.error" class="pyodide-result__error">
                <CloseCircleOutlined /> <span>{{ pyodideResult.error }}</span>
              </div>
            </template>
          </template>

          <template v-else-if="textResult">
            <pre class="tool-call-block__raw">{{ textResult }}</pre>
          </template>

          <template v-else-if="isSearching">
            <div class="tool-call-block__waiting">
              <LoadingOutlined />
              <span>正在执行工具调用...</span>
            </div>
          </template>

          <template v-else>
            <div class="tool-call-block__empty">无返回结果</div>
          </template>
        </div>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<style scoped>
.tool-call-block {
  margin: 4px 0;
}

.tool-call-block__collapse {
  background: transparent;
}

.tool-call-block__collapse :deep(.ant-collapse-header) {
  padding: 8px 12px !important;
  align-items: center !important;
}

.tool-call-block__collapse :deep(.ant-collapse-content-box) {
  padding: 8px 12px !important;
}

.tool-call-block__collapse :deep(.ant-collapse-item) {
  border: none;
}

.tool-call-block__collapse :deep(.ant-collapse) {
  border: none;
  background: #fafafa;
  border-radius: 8px;
}

.tool-call-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-call-block__icon {
  color: #1677ff;
  font-size: 14px;
}

.tool-call-block__name {
  font-size: 13px;
  font-weight: 500;
  color: #595959;
}

.tool-call-block__loading {
  color: #1677ff;
  font-size: 14px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.tool-call-block__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ===== View toggle ===== */
.tool-call-block__view-toggle {
  display: flex;
  justify-content: flex-end;
  gap: 0;
}

.view-toggle__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s,
    border-color 0.15s;
  font-family: inherit;
  line-height: 1.4;
}

.view-toggle__btn:first-child {
  border-radius: 6px 0 0 6px;
}

.view-toggle__btn:last-child {
  border-radius: 0 6px 6px 0;
  border-left: none;
}

.view-toggle__btn:hover {
  color: #595959;
  background: #f5f5f5;
}

.view-toggle__btn--active {
  color: #1677ff;
  background: #e6f4ff;
  border-color: #91caff;
}

.view-toggle__btn--active + .view-toggle__btn--active {
  border-left-color: #91caff;
}

/* ===== Card view: horizontal scroll ===== */
.tool-call-block__card-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 6px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

.tool-call-block__card-scroll::-webkit-scrollbar {
  height: 4px;
}

.tool-call-block__card-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.tool-call-block__card-scroll::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.tool-call-block__card-scroll::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

.tool-call-block__card-item {
  flex-shrink: 0;
  width: 260px;
  min-width: 260px;
}

.tool-call-block__card-item :deep(.search-result-card__title) {
  white-space: normal;
}

.tool-call-block__card-item :deep(.search-result-card__snippet) {
  -webkit-line-clamp: 3;
}

/* ===== List view: vertical stack ===== */
.tool-call-block__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-call-block__raw {
  margin: 0;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', monospace;
  max-height: 300px;
  overflow-y: auto;
}

.tool-call-block__waiting {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: #8c8c8c;
  font-size: 13px;
}

.tool-call-block__empty {
  color: #8c8c8c;
  font-size: 13px;
}

@media (max-width: 768px) {
  .tool-call-block__collapse :deep(.ant-collapse-header) {
    padding: 6px 10px !important;
  }

  .tool-call-block__collapse :deep(.ant-collapse-content-box) {
    padding: 6px 10px !important;
  }

  .tool-call-block__card-item {
    width: 220px;
    min-width: 220px;
  }
}

/* ===== Pyodide result styles ===== */
.pyodide-result__code {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.pyodide-result__code-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f5f5f5;
  font-size: 12px;
  color: #8c8c8c;
}

.pyodide-result__code pre {
  margin: 0;
  padding: 10px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', monospace;
  max-height: 200px;
  overflow-y: auto;
}

.pyodide-result__output {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.pyodide-result__output--stderr {
  border-color: #ffd8bf;
}

.pyodide-result__output-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 10px;
  background: #f5f5f5;
  font-size: 12px;
  color: #8c8c8c;
}

.pyodide-result__output--stderr .pyodide-result__output-header {
  background: #fff7e6;
  color: #d46b08;
}

.pyodide-result__output pre {
  margin: 0;
  padding: 10px;
  background: #fafafa;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', monospace;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.pyodide-result__output--stderr pre {
  background: #fffbe6;
}

.pyodide-result__exit-code--success {
  color: #52c41a;
  font-size: 11px;
}

.pyodide-result__error {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  font-size: 12px;
  color: #cf1322;
  line-height: 1.5;
}

.pyodide-result__error span {
  white-space: pre-wrap;
  word-break: break-all;
}

.rag-search-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rag-search-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.rag-search-card--compact {
  flex-shrink: 0;
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s ease;
}

.rag-search-card--compact:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.rag-search-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 12px;
}

.rag-search-card--compact .rag-search-card__header {
  margin-bottom: 0;
}

.rag-search-card__index {
  font-weight: 600;
  color: #1677ff;
  min-width: 24px;
}

.rag-search-card__file {
  color: #595959;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rag-search-card__relevance {
  margin-left: auto;
  color: #8c8c8c;
  font-size: 11px;
}

.rag-search-card__content {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.rag-search-card--compact .rag-search-card__content {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12px;
}

.rag-search-card__footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #bfbfbf;
  margin-top: 4px;
}

.rag-search-card--compact .rag-search-card__footer {
  margin-top: auto;
}
</style>
