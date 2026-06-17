<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons-vue'
import {
  pageRagLibraryDocsApi,
  addRagLibraryDocApi,
  deleteRagLibraryDocApi,
  renameRagLibraryDocApi,
  getRagLibraryDocDetailApi,
} from '@/api/apiRagLibrary'
import type { RagLibraryDoc, DocPreviewVo, PageRagLibraryDocVo, AddRagLibraryDocVo, RenameDocVo } from '@/types/ragLibrary'
import { ui } from '@/utils/ui'

const route = useRoute()
const router = useRouter()
const libraryId = route.params.libraryId as string
const libraryName = (route.query.libraryName as string) || libraryId

const loading = ref(false)
const total = ref(0)
const tableData = ref<RagLibraryDoc[]>([])
const queryParams = reactive<PageRagLibraryDocVo>({
  libraryId,
  pageNum: 1,
  pageSize: 10,
})

function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const columns = [
  { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
  { title: '文件大小', key: 'fileSize', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '分块', key: 'chunkInfo', width: 100 },
  { title: '操作', key: 'actions', width: 320, fixed: 'right' as const },
]

const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref()
const createForm = reactive<AddRagLibraryDocVo>({
  libraryId,
  fileName: '',
  fileContent: '',
})
const selectedFileName = ref('')

const rules = {
  fileName: [{ required: true, message: '请输入文件名', trigger: 'blur' }],
  fileContent: [{ required: true, message: '请选择文件', trigger: 'change' }],
}

interface PendingFile {
  name: string
  content: string
}

const fileInputRef = ref<HTMLInputElement | null>(null)
const renameVisible = ref(false)
const renameSubmitting = ref(false)
const renameForm = reactive<RenameDocVo>({ id: '', fileName: '' })
const previewVisible = ref(false)
const previewDoc = ref<DocPreviewVo | null>(null)
const previewLoading = ref(false)
const pendingFiles = ref<PendingFile[]>([])
const uploadingIndex = ref(0)
const uploadTotal = ref(0)

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  pendingFiles.value = []
  let loaded = 0
  files.forEach((file) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const result = e.target?.result as string
      pendingFiles.value.push({
        name: file.name,
        content: result.split(',')[1] || '',
      })
      loaded++
      if (loaded === files.length) {
        selectedFileName.value = files.map(f => f.name).join('、')
      }
    }
    reader.readAsDataURL(file)
  })
}

function showTotal(value: number): string {
  return `共 ${value} 条`
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchDocs() {
  loading.value = true
  try {
    const res = await pageRagLibraryDocsApi(queryParams)
    tableData.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch (error) {
    console.error('获取文档列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number, pageSize?: number) {
  queryParams.pageNum = page
  if (pageSize) {
    queryParams.pageSize = pageSize
  }
  fetchDocs()
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchDocs()
}

function goBack() {
  router.push({ name: 'ragLibrary' })
}

function openCreateModal() {
  createForm.fileName = ''
  createForm.fileContent = ''
  createForm.libraryId = libraryId
  selectedFileName.value = ''
  pendingFiles.value = []
  if (fileInputRef.value) fileInputRef.value.value = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!pendingFiles.value.length) {
    ui.warning('请先选择文件')
    return
  }

  submitting.value = true
  uploadTotal.value = pendingFiles.value.length
  let successCount = 0
  let failCount = 0

  let idx = 0
  for (const file of pendingFiles.value) {
    idx++
    uploadingIndex.value = idx
    try {
      await addRagLibraryDocApi({
        libraryId,
        fileName: file.name,
        fileContent: file.content,
      })
      successCount++
    } catch (error) {
      console.error(`上传文件 ${file.name} 失败:`, error)
      failCount++
    }
    }

  uploadingIndex.value = 0
  uploadTotal.value = 0
  ui.success('上传完成：成功 ' + successCount + ' 个' + (failCount > 0 ? '，失败 ' + failCount + ' 个' : ''))
  createVisible.value = false
  queryParams.pageNum = 1
  fetchDocs()
}

async function deleteDoc(doc: RagLibraryDoc) {
  try {
    await ui.confirm({
      title: '确认删除文档',
      content: `确定删除文档"${doc.fileName}"吗？`,
      type: 'warning',
    })
    await deleteRagLibraryDocApi({ id: doc.id })
    ui.success('文档已删除')
    fetchDocs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文档失败:', error)
    }
  }
}

function openRenameModal(doc: RagLibraryDoc) {
  renameForm.id = doc.id
  renameForm.fileName = doc.fileName
  renameVisible.value = true
}

async function submitRename() {
  if (!renameForm.fileName.trim()) {
    ui.warning('文件名不能为空')
    return
  }
  renameSubmitting.value = true
  try {
    await renameRagLibraryDocApi({ id: renameForm.id, fileName: renameForm.fileName.trim() })
    ui.success('重命名成功')
    renameVisible.value = false
    fetchDocs()
  } catch (error) {
    console.error('重命名失败:', error)
  } finally {
    renameSubmitting.value = false
  }
}

async function previewDocContent(doc: RagLibraryDoc) {
  previewLoading.value = true
  previewVisible.value = true
  try {
    const res = await getRagLibraryDocDetailApi(doc.id)
    previewDoc.value = res.data.data as any
  } catch (error) {
    console.error('获取文档详情失败:', error)
    ui.error('获取文档详情失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

onMounted(() => {
  fetchDocs()
})
</script>

<template>
  <div class="doc-management">
    <a-card class="content-card">
      <div class="page-path">
        <a-button type="link" class="back-button" @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">知识库文档</span>
      </div>

      <div class="card-header">
        <div>
          <h3>{{ libraryName }}</h3>
          <p class="card-subtitle">管理知识库中的文档。</p>
        </div>
        <a-space>
          <a-button @click="fetchDocs">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button type="primary" @click="openCreateModal">
            <template #icon><UploadOutlined /></template>
            上传文档
          </a-button>
        </a-space>
      </div>

      <div class="search-bar">
        <a-input-search
          v-model:value="queryParams.keyword"
          placeholder="搜索文件名..."
          allow-clear
          style="width: 280px"
          @search="handleSearch"
          @press-enter="handleSearch"
        />
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 700 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fileSize'">
            <span>{{ formatFileSize(record.fileSize) }}</span>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'chunkInfo'">
            <span v-if="record.parsed" style="color: #389e0d; font-weight: 500;">{{ record.chunkNum }} 块</span>
            <span v-else style="color: #8c8c8c;">未解析</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
          <a-button size="small" type="primary" ghost @click="previewDocContent(record)">
            <template #icon><EyeOutlined /></template>
            预览
          </a-button>
          <a-button size="small" @click="openRenameModal(record)">
            <template #icon><EditOutlined /></template>
            重命名
          </a-button>
              <a-button danger size="small" @click="deleteDoc(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="table-pagination">
        <a-pagination
          :current="queryParams.pageNum"
          :page-size="queryParams.pageSize"
          :total="total"
          show-size-changer
          show-quick-jumper
          :show-total="showTotal"
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="createVisible"
      title="上传文档"
      ok-text="添加"
      cancel-text="取消"
      :confirm-loading="submitting"
      @ok="submitCreate"
    >
      <a-form ref="createFormRef" :model="createForm" :rules="rules" layout="vertical">
        <a-form-item label="文件">
          <div style="margin-bottom: 6px; font-size: 12px; color: #8c8c8c;">仅支持 .md、.txt 等纯文本格式，可多选</div>
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept=".md,.txt"
            style="width: 100%"
            @change="handleFileSelect"
          />
          <div v-if="pendingFiles.length" style="margin-top: 8px;">
            <div v-for="(f, idx) in pendingFiles" :key="idx" style="font-size: 13px; color: #595959; padding: 2px 0;">
              {{ idx + 1 }}. {{ f.name }}
            </div>
          </div>
          <div v-if="submitting && uploadTotal > 0" style="margin-top: 8px; font-size: 13px; color: #1677ff;">
            正在上传 {{ uploadingIndex }}/{{ uploadTotal }}...
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="renameVisible"
      title="重命名文档"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="renameSubmitting"
      @ok="submitRename"
    >
      <a-form layout="vertical">
        <a-form-item label="文件名">
          <a-input v-model:value="renameForm.fileName" placeholder="请输入新文件名" />
        </a-form-item>
      </a-form>
    </a-modal>

  <a-modal
    v-model:open="previewVisible"
    title="文档预览"
    footer=""
    width="720px"
  >
    <a-skeleton :loading="previewLoading" active>
      <template v-if="previewDoc">
          <div style="margin-bottom: 16px;">
            <div style="font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 4px;">{{ previewDoc.fileName }}</div>
            <div style="font-size: 12px; color: #8c8c8c;">
              文件大小：{{ formatFileSize(previewDoc.fileSize) }}
            </div>
          </div>
          <a-divider style="margin: 8px 0 16px;" />
          <pre style="font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; max-height: 480px; overflow-y: auto; margin: 0; padding: 12px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 8px;">{{ previewDoc.content }}</pre>
      </template>
    </a-skeleton>
  </a-modal>
  </div>
</template>

<style scoped>
.doc-management {
  min-height: 100%;
}

.content-card {
  min-height: 100%;
  border-radius: 0;
  box-shadow: none;
}

.page-path {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.5;
  display: flex;
  align-items: center;
}

.back-button {
  padding: 0;
  height: auto;
  color: #262626;
  font-weight: 500;
}

.back-button:hover {
  color: #1677ff;
}

.page-path-separator {
  color: #8c8c8c;
  margin: 0 4px;
}

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
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.card-subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 13px;
}

.search-bar {
  margin-bottom: 16px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }

  :deep(.ant-table-wrapper) {
    overflow-x: auto;
  }
}
</style>
