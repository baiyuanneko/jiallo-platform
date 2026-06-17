// Pyodide 代码执行引擎 — 管理生命周期、代码执行、File System Access API 集成

import { shallowRef, ref } from 'vue'
import type { PyodideAPI } from 'pyodide'
import type { PyodideExecutionResult } from '@/types/chat'

// ==================== IndexedDB helpers for persisting directory handle ====================

const DB_NAME = 'pyodide-workspace-db'
const STORE_NAME = 'handles'
const KEY = 'workspace'

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => {
      req.result.createObjectStore(STORE_NAME)
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function saveHandle(handle: FileSystemDirectoryHandle): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put(handle, KEY)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function loadHandle(): Promise<FileSystemDirectoryHandle | undefined> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const req = tx.objectStore(STORE_NAME).get(KEY)
    req.onsuccess = () => resolve(req.result as FileSystemDirectoryHandle | undefined)
    req.onerror = () => reject(req.error)
  })
}

async function removeHandle(): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).delete(KEY)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

// ==================== Composable ====================

const pyodideInstance = shallowRef<PyodideAPI | null>(null)
const isLoading = ref(false)
const isInitialized = ref(false)

const workspaceHandle = ref<FileSystemDirectoryHandle | null>(null)
const workspaceName = ref<string>('')
const isMounting = ref(false)

const fsChangeVersion = ref(0)

let syncfsFn: (() => Promise<void>) | null = null
let initPromise: Promise<PyodideAPI> | null = null

export function usePyodide() {
  async function initPyodide(): Promise<PyodideAPI> {
    if (pyodideInstance.value) return pyodideInstance.value
    if (initPromise) return initPromise

    initPromise = (async () => {
      isLoading.value = true
      try {
        const { loadPyodide } = await import('pyodide')
        const pyodide = await loadPyodide({
          indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.29.3/full/',
        })
        pyodideInstance.value = pyodide
        isInitialized.value = true

        // Restore workspace if previously saved
        await restoreWorkspace(pyodide)

        return pyodide
      } finally {
        isLoading.value = false
      }
    })()

    return initPromise
  }

  async function restoreWorkspace(pyodide: PyodideAPI): Promise<void> {
    try {
      const saved = await loadHandle()
      if (saved) {
        // Re-request permission (required after page reload)
        const perm = await (saved as unknown as { queryPermission(desc: { mode: string }): Promise<PermissionState> }).queryPermission({ mode: 'readwrite' })
        if (perm === 'granted') {
          workspaceHandle.value = saved
          workspaceName.value = saved.name
          await mountWorkspace(pyodide, saved)
        }
      }
    } catch {
      // Ignore restore errors — workspace is optional
    }
  }

  async function selectWorkspace(): Promise<void> {
    if (!('showDirectoryPicker' in window)) return

    const dirHandle = await (window as unknown as { showDirectoryPicker(opts: { mode: string }): Promise<FileSystemDirectoryHandle> }).showDirectoryPicker({ mode: 'readwrite' })
    await saveHandle(dirHandle)
    workspaceHandle.value = dirHandle
    workspaceName.value = dirHandle.name

    if (pyodideInstance.value) {
      await mountWorkspace(pyodideInstance.value, dirHandle)
    }
  }

  async function mountWorkspace(pyodide: PyodideAPI, dirHandle: FileSystemDirectoryHandle): Promise<void> {
    isMounting.value = true
    try {
      // Unmount existing workspace if any
      if (syncfsFn) {
        try {
          await syncfsFn()
          pyodide.FS.unmount('/workspace')
        } catch {
          // Ignore unmount errors
        }
        syncfsFn = null
      }

      pyodide.FS.mkdirTree('/workspace')
      const nativeFS = await pyodide.mountNativeFS('/workspace', dirHandle)
      syncfsFn = nativeFS.syncfs
    } finally {
      isMounting.value = false
    }
  }

  async function unmountWorkspace(): Promise<void> {
    if (!pyodideInstance.value) return

    if (syncfsFn) {
      try {
        await syncfsFn()
        pyodideInstance.value.FS.unmount('/workspace')
      } catch {
        // Ignore
      }
      syncfsFn = null
    }

    workspaceHandle.value = null
    workspaceName.value = ''
    await removeHandle()
  }

  async function executeCode(code: string): Promise<PyodideExecutionResult> {
    const pyodide = await initPyodide()

    let stdout = ''
    let stderr = ''

    pyodide.setStdout({ batched: (msg: string) => { stdout += msg + '\n' } })
    pyodide.setStderr({ batched: (msg: string) => { stderr += msg + '\n' } })

    try {
      // If workspace is mounted, change CWD so relative paths resolve to /workspace
      if (syncfsFn) {
        await pyodide.runPythonAsync('import os; os.chdir("/workspace")')
      }

      await pyodide.loadPackagesFromImports(code)
      await pyodide.runPythonAsync(code, { filename: 'user_script.py' })

      return { stdout: stdout || null, stderr: stderr || null, error: null, exitCode: 0 }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      return { stdout: stdout || null, stderr: stderr || null, error: message, exitCode: 1 }
    } finally {
      // Always sync file changes to disk, even if code threw an error
      if (syncfsFn) {
        await syncfsFn()
      }
      fsChangeVersion.value++
    }
  }

  const supportsFileSystemAccess = 'showDirectoryPicker' in window

  return {
    pyodideInstance,
    isLoading,
    isInitialized,
    workspaceHandle,
    workspaceName,
    isMounting,
    fsChangeVersion,
    supportsFileSystemAccess,
    initPyodide,
    executeCode,
    selectWorkspace,
    unmountWorkspace,
  }
}
