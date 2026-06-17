// RAG 知识库相关类型定义

import type { ApiResponse } from './user'
import type { PageResult } from './common'

export interface RagLibrary {
  id: string
  name: string
  description: string | null
  docCount: number
  totalFileSize: number
  createUser: string
  updateUser: string
  createTime: string
  updateTime: string
  isDel: boolean
}

export interface RagLibraryDoc {
  id: string
  ragLibraryId: string
  fileName: string
  fileContent: string | null
  fileSize: number
  chunkNum: number
  parsed: boolean | null
  createUser: string
  updateUser: string
  createTime: string
  updateTime: string
  isDel: boolean
}

// 请求参数
export interface PageVo {
  pageNum?: number
  pageSize?: number
}

export interface AddRagLibraryVo {
  name: string
  description?: string
}

export interface UpdateRagLibraryVo {
  id: string
  name?: string
  description?: string
}

export interface DeleteVo {
  id: string
}

export interface PageRagLibraryDocVo {
  libraryId: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface AddRagLibraryDocVo {
  libraryId: string
  fileName: string
  fileContent: string
}

export interface DeleteDocVo {
  id: string
}

export interface RenameDocVo {
  id: string
  fileName: string
}

export interface ParseDocVo {
  docId: string
}

export interface DocPreviewVo {
  id: string
  fileName: string
  fileSize: number
  content: string
}

// 响应别名
export type RPageRagLibrary = ApiResponse<PageResult<RagLibrary>>
export type RPageRagLibraryDoc = ApiResponse<PageResult<RagLibraryDoc>>

export interface SearchVo {
  libraryIds: string[]
  query: string
  limit?: number
}

export interface SearchResultVo {
  chunkId: string
  docId: string
  libraryId: string
  libraryName: string
  fileName: string
  chunkIndex: number
  chunkContent: string
  relevance: number
}

export type RSearchResult = ApiResponse<SearchResultVo[]>

export type RVoid = ApiResponse<void>
