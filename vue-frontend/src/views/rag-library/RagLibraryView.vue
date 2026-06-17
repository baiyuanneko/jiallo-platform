<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DeleteOutlined, PlusOutlined, ReloadOutlined, ReadOutlined, FolderOpenOutlined, EditOutlined } from '@ant-design/icons-vue'
import {
  pageRagLibrariesApi,
  addRagLibraryApi,
  updateRagLibraryApi,
  deleteRagLibraryApi,
} from '@/api/apiRagLibrary'
import type { RagLibrary, PageVo, AddRagLibraryVo, UpdateRagLibraryVo } from '@/types/ragLibrary'
import { ui } from '@/utils/ui'

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const libraries = ref<RagLibrary[]>([])
const queryParams = reactive<PageVo>({
  pageNum: 1,
  pageSize: 20,
})

const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref()
const createForm = reactive<AddRagLibraryVo>({
  name: '',
  description: '',
})

const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()
const editForm = reactive<UpdateRagLibraryVo>({
  id: '',
  name: '',
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
}

function showTotal(value: number): string {
  return `共 ${value} 条`
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function getValue(value?: string | null): string {
  return value?.trim() ? value : '-'
}

function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function fetchLibraries() {
  loading.value = true
  try {
    const res = await pageRagLibrariesApi(queryParams)
    libraries.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch (error) {
    console.error('获取知识库列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number, pageSize?: number) {
  queryParams.pageNum = page
  if (pageSize) {
    queryParams.pageSize = pageSize
  }
  fetchLibraries()
}

function openCreateModal() {
  createForm.name = ''
  createForm.description = ''
  createVisible.value = true
}

async function submitCreate() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await addRagLibraryApi({
      name: createForm.name.trim(),
      description: createForm.description?.trim() || '',
    })
    ui.success('知识库创建成功')
    createVisible.value = false
    queryParams.pageNum = 1
    fetchLibraries()
  } catch (error) {
    console.error('创建知识库失败:', error)
  } finally {
    submitting.value = false
  }
}

function openEditModal(library: RagLibrary) {
  editForm.id = library.id
  editForm.name = library.name
  editForm.description = library.description || ''
  editVisible.value = true
}

async function submitEdit() {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }

  editSubmitting.value = true
  try {
    await updateRagLibraryApi({
      id: editForm.id,
      name: (editForm.name || '').trim(),
      description: editForm.description?.trim() || '',
    })
    ui.success('知识库已更新')
    editVisible.value = false
    fetchLibraries()
  } catch (error) {
    console.error('更新知识库失败:', error)
  } finally {
    editSubmitting.value = false
  }
}

async function deleteLibrary(library: RagLibrary) {
  try {
    await ui.confirm({
      title: '确认删除知识库',
      content: `确定删除知识库"${library.name}"吗？该知识库下的文档也会一起删除。`,
      type: 'warning',
    })
    await deleteRagLibraryApi({ id: library.id })
    ui.success('知识库已删除')
    fetchLibraries()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除知识库失败:', error)
    }
  }
}

function viewDocs(library: RagLibrary) {
  router.push({
    name: 'ragLibraryDocs',
    params: { libraryId: library.id },
    query: { libraryName: library.name },
  })
}

onMounted(() => {
  fetchLibraries()
})
</script>

<template>
  <a-card class="content-card" :loading="loading">
    <div class="page-path">
      <span class="page-path-root">RAG 知识库</span>
      <span class="page-path-separator"> / </span>
      <span class="page-path-current">知识库列表</span>
    </div>

    <div class="card-header">
      <div>
        <h3>知识库列表</h3>
        <p class="card-subtitle">管理你的 RAG 知识库以及其中的文档。</p>
      </div>
      <a-space>
        <a-button @click="fetchLibraries">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建知识库
        </a-button>
      </a-space>
    </div>

    <div v-if="libraries.length === 0 && !loading" class="empty-state">
      <p>暂无知识库，点击上方按钮创建一个。</p>
    </div>

    <div v-else class="library-grid">
      <div
        v-for="lib in libraries"
        :key="lib.id"
        class="library-card"
        @click="viewDocs(lib)"
      >
        <div class="library-card__accent"></div>
        <div class="library-card__body">
          <div class="library-card__icon">
            <ReadOutlined />
          </div>
          <div class="library-card__info">
            <h4 class="library-card__title">{{ lib.name }}</h4>
            <p class="library-card__desc">{{ getValue(lib.description) }}</p>
          </div>
        </div>
        <div class="library-card__stats">
          <div class="library-card__stat">
            <span class="library-card__stat-value">{{ lib.docCount }}</span>
            <span class="library-card__stat-label">篇文档</span>
          </div>
          <div class="library-card__stat">
            <span class="library-card__stat-value">{{ formatFileSize(lib.totalFileSize) }}</span>
            <span class="library-card__stat-label">总大小</span>
          </div>
          <div class="library-card__stat">
            <span class="library-card__stat-value">{{ formatTime(lib.createTime).split(' ')[0] }}</span>
            <span class="library-card__stat-label">创建日期</span>
          </div>
        </div>
        <div class="library-card__actions" @click.stop>
          <a-button size="small" type="primary" ghost @click="viewDocs(lib)">
            <template #icon><FolderOpenOutlined /></template>
            查看文档
          </a-button>
          <a-button size="small" @click="openEditModal(lib)">
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
          <a-button size="small" danger @click="deleteLibrary(lib)">
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </div>
      </div>
    </div>

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
    title="新建知识库"
    ok-text="创建"
    cancel-text="取消"
    :confirm-loading="submitting"
    @ok="submitCreate"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="rules" layout="vertical">
      <a-form-item label="名称" name="name">
        <a-input v-model:value="createForm.name" placeholder="请输入知识库名称" />
      </a-form-item>
      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="createForm.description" placeholder="可选" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="editVisible"
    title="编辑知识库"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="editSubmitting"
    @ok="submitEdit"
  >
    <a-form ref="editFormRef" :model="editForm" :rules="rules" layout="vertical">
      <a-form-item label="名称" name="name">
        <a-input v-model:value="editForm.name" placeholder="请输入知识库名称" />
      </a-form-item>
      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="editForm.description" placeholder="可选" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
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

.library-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.library-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
  transition:
    box-shadow 0.25s ease,
    border-color 0.25s ease,
    transform 0.25s ease;
}

.library-card:hover {
  border-color: #d9d9d9;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.library-card__accent {
  height: 4px;
  background: linear-gradient(90deg, #1677ff, #69b1ff);
  flex-shrink: 0;
}

.library-card__body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 20px 0;
}

.library-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e6f4ff 0%, #d6eaff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #1677ff;
  flex-shrink: 0;
}

.library-card__info {
  flex: 1;
  min-width: 0;
}

.library-card__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.library-card__desc {
  margin: 0;
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 0;
}

.library-card__stats {
  display: flex;
  gap: 24px;
  padding: 16px 20px;
}

.library-card__stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.library-card__stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.library-card__stat-label {
  font-size: 12px;
  color: #bfbfbf;
  line-height: 1.2;
}

.library-card__actions {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }

  .library-grid {
    grid-template-columns: 1fr;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
