// RAG 知识库相关 API 接口

import request from '@/utils/request'
import type {
  PageVo,
  AddRagLibraryVo,
  UpdateRagLibraryVo,
  DeleteVo,
  PageRagLibraryDocVo,
  AddRagLibraryDocVo,
  DocPreviewVo,
  DeleteDocVo,
  RenameDocVo,
  ParseDocVo,
  SearchVo,
  RagLibrary,
  RPageRagLibrary,
  RPageRagLibraryDoc,
  RVoid,
  RSearchResult,
} from '@/types/ragLibrary'
import type { ApiResponse } from '@/types/user'

/**
 * 分页查询知识库列表
 */
export function pageRagLibrariesApi(data: PageVo) {
  return request.post<RPageRagLibrary>('/ragLibrary/page', data)
}

/**
 * 查询全部知识库列表（不分页）
 */
export function listAllRagLibrariesApi() {
  return request.post<ApiResponse<RagLibrary[]>>('/ragLibrary/listAll')
}

/**
 * 新增知识库
 */
export function addRagLibraryApi(data: AddRagLibraryVo) {
  return request.post<RPageRagLibrary>('/ragLibrary/add', data)
}

/**
 * 更新知识库
 */
export function updateRagLibraryApi(data: UpdateRagLibraryVo) {
  return request.post<RPageRagLibrary>('/ragLibrary/update', data)
}

/**
 * 删除知识库
 */
export function deleteRagLibraryApi(data: DeleteVo) {
  return request.post<RPageRagLibrary>('/ragLibrary/delete', data)
}

/**
 * 分页查询知识库文档列表
 */
export function pageRagLibraryDocsApi(data: PageRagLibraryDocVo) {
  return request.post<RPageRagLibraryDoc>('/ragLibrary/docs/page', data)
}

/**
 * 新增文档
 */
export function addRagLibraryDocApi(data: AddRagLibraryDocVo) {
  return request.post<RPageRagLibraryDoc>('/ragLibrary/docs/add', data)
}

/**
 * 删除文档
 */
export function deleteRagLibraryDocApi(data: DeleteDocVo) {
  return request.post<RPageRagLibraryDoc>('/ragLibrary/docs/delete', data)
}

/**
 * 重命名文档
 */
export function renameRagLibraryDocApi(data: RenameDocVo) {
  return request.post<RVoid>('/ragLibrary/docs/rename', data)
}

/**
 * 获取文档详情（含完整内容）
 */
export function getRagLibraryDocDetailApi(id: string) {
  return request.post<ApiResponse<DocPreviewVo>>('/ragLibrary/docs/detail', { id })
}

/**
 * 检索知识库
 * 在选中的知识库中全文检索，返回最匹配的分块
 */
export function searchRagLibraryApi(data: SearchVo) {
  return request.post<RSearchResult>('/ragLibrary/search', data)
}
