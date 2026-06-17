<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  pageRagLibrariesApi,
  searchRagLibraryApi,
} from '@/api/apiRagLibrary'
import type { RagLibrary, SearchResultVo } from '@/types/ragLibrary'
import { ui } from '@/utils/ui'

const libraries = ref<RagLibrary[]>([])
const selectedLibraryIds = ref<string[]>([])
const query = ref('')
const results = ref<SearchResultVo[]>([])
const searching = ref(false)
const searched = ref(false)
const expandedMap = ref<Record<string, boolean>>({})
const limit = ref(10)

function toggleExpand(chunkId: string) {
  expandedMap.value[chunkId] = !expandedMap.value[chunkId]
}

function isExpanded(chunkId: string): boolean {
  return !!expandedMap.value[chunkId]
}

function truncate(text: string, maxLen: number = 150): string {
  if (!text || text.length <= maxLen) return text
  return text.slice(0, maxLen) + '...'
}

async function fetchLibraries() {
  try {
    const res = await pageRagLibrariesApi({ pageNum: 1, pageSize: 999 })
    libraries.value = res.data.data.records || []
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    ui.error('获取知识库列表失败')
  }
}

async function doSearch() {
  if (!query.value.trim()) {
    ui.warning('请输入查询内容')
    return
  }
  if (selectedLibraryIds.value.length === 0) {
    ui.warning('请选择要检索的知识库')
    return
  }

  searching.value = true
  searched.value = true
  try {
    const res = await searchRagLibraryApi({
      libraryIds: selectedLibraryIds.value,
      query: query.value,
      limit: limit.value,
    })
    results.value = res.data.data || []
  } catch (error) {
    console.error('检索失败:', error)
    ui.error('检索失败')
    results.value = []
  } finally {
    searching.value = false
  }
}

function formatRelevance(score: number): string {
  return score.toFixed(4)
}

function relevanceColor(score: number): string {
  if (score > 5) return '#52c41a'
  if (score > 1) return '#1677ff'
  return '#8c8c8c'
}

onMounted(() => {
  fetchLibraries()
})
</script>

<template>
  <a-card class="content-card">
    <div class="page-path">
      <span class="page-path-root">RAG 知识库</span>
      <span class="page-path-separator"> / </span>
      <span class="page-path-current">检索测试</span>
    </div>

    <div class="card-header">
      <h3>检索测试</h3>
    </div>

    <div class="search-layout">
      <!-- Left: Search Controls -->
      <aside class="search-sidebar">
        <div class="search-controls">
          <div class="search-field">
            <label class="search-label">选择知识库</label>
            <a-select
              v-model:value="selectedLibraryIds"
              mode="multiple"
              placeholder="请选择要检索的知识库"
              style="width: 100%"
              :options="libraries.map(l => ({ value: l.id, label: l.name }))"
            />
          </div>

          <div class="search-field">
            <label class="search-label">查询内容</label>
            <a-input
              v-model:value="query"
              placeholder="输入要检索的内容..."
              size="large"
              @press-enter="doSearch"
            />
          </div>

          <div class="search-field">
            <label class="search-label">返回条数：{{ limit }}</label>
            <a-slider v-model:value="limit" :min="5" :max="20" :marks="{ 5: '5', 10: '10', 15: '15', 20: '20' }" />
          </div>

          <a-button type="primary" size="large" block :loading="searching" @click="doSearch">
            <template #icon><SearchOutlined /></template>
            检索
          </a-button>

          <div v-if="searched && !searching" class="search-summary">
            共找到 <strong>{{ results.length }}</strong> 条结果
          </div>
        </div>
      </aside>

      <!-- Right: Results -->
      <main class="search-results">
        <div v-if="!searched" class="search-hint">
          <SearchOutlined style="font-size: 48px; color: #d9d9d9; margin-bottom: 16px" />
          <p>选择知识库并输入查询内容开始检索</p>
        </div>

        <div v-else-if="searching" class="search-hint">
          <a-spin />
          <p>正在检索...</p>
        </div>

        <div v-else-if="results.length === 0" class="search-hint">
          <p>未找到匹配结果</p>
        </div>

        <div v-else class="result-list">
          <div v-for="(item, index) in results" :key="item.chunkId" class="result-card">
            <div class="result-header">
              <span class="result-index">#{{ index + 1 }}</span>
              <a-tag color="blue">{{ item.libraryName }}</a-tag>
              <span class="result-file-name">{{ item.fileName }}</span>
              <a-tag v-if="item.chunkIndex != null" color="purple" class="result-chunk-tag">第 {{ item.chunkIndex + 1 }} 块</a-tag>
            </div>
            <div class="result-relevance">
              <span class="result-relevance-label">关键词相似度</span>
              <a-progress
                :percent="Math.min(item.relevance * 10, 100)"
                :show-info="false"
                size="small"
                :stroke-color="relevanceColor(item.relevance)"
              />
              <span class="result-relevance-value">{{ formatRelevance(item.relevance) }}</span>
            </div>
            <div class="result-content" :class="{ 'result-content--collapsed': !isExpanded(item.chunkId) }">
              {{ isExpanded(item.chunkId) ? item.chunkContent : truncate(item.chunkContent) }}
            </div>
            <div v-if="item.chunkContent && item.chunkContent.length > 150" class="result-toggle" @click="toggleExpand(item.chunkId)">
              {{ isExpanded(item.chunkId) ? '收起' : '展开全部' }}
            </div>
          </div>
        </div>
      </main>
    </div>
  </a-card>
</template>

<style scoped>
.content-card {
  flex: 1;
  min-height: 100%;
  border-radius: 0;
  box-shadow: none;
}

.page-path {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.5;
}

.page-path-root {
  color: #262626;
  font-weight: 500;
}

.page-path-separator,
.page-path-current {
  color: #8c8c8c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.search-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.search-sidebar {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
}

.search-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.search-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.search-summary {
  text-align: center;
  font-size: 13px;
  color: #8c8c8c;
  padding-top: 4px;
}

.search-results {
  flex: 1;
  min-width: 0;
}

.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #8c8c8c;
  gap: 12px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.result-index {
  font-weight: 600;
  color: #1677ff;
  min-width: 28px;
}

.result-file-name {
  color: #595959;
  font-weight: 500;
}

.result-chunk-tag {
  margin-left: auto;
}

.result-relevance {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 0 2px;
}

.result-relevance-label {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
}

.result-relevance :deep(.ant-progress) {
  flex: 1;
  max-width: 120px;
}

.result-relevance-value {
  font-size: 12px;
  color: #8c8c8c;
  font-variant-numeric: tabular-nums;
  min-width: 48px;
}

.result-content {
  font-size: 14px;
  color: #262626;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-content--collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-toggle {
  margin-top: 8px;
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
  user-select: none;
}

.result-toggle:hover {
  color: #4096ff;
}

@media (max-width: 768px) {
  .search-layout {
    flex-direction: column;
  }

  .search-sidebar {
    width: 100%;
    position: static;
  }
}
</style>
