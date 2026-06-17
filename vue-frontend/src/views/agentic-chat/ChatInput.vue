<script setup lang="ts">
import { ref, computed, h, watch, onMounted } from 'vue'
import {
  SendOutlined,
  PlusOutlined,
  PictureOutlined,
  PauseCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  RocketOutlined,
  StarOutlined,
  GlobalOutlined,
  FolderOutlined,
  ApartmentOutlined,
  CodeOutlined,
  BarChartOutlined,
  FolderOpenOutlined,
  LoadingOutlined,
  QuestionCircleOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { Modal, Button, Checkbox, message } from 'ant-design-vue'
import { useChatStore } from '@/stores/chat'
import { AgentType } from '@/types/chat'
import { useQuoteReply } from '@/composables/useQuoteReply'
import { usePyodide } from '@/composables/usePyodide'
import { usePreferences } from '@/composables/usePreferences'
import { listAllRagLibrariesApi } from '@/api/apiRagLibrary'

const chatStore = useChatStore()
const { ctrlEnterToSend, rememberReasoningEffort, savedReasoningEffort } = usePreferences()
const { quotingText, cancelQuote } = useQuoteReply()
const pyodide = usePyodide()

const inputText = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const textareaRef = ref<any>(null)

function focusInput() {
  textareaRef.value?.focus()
}

defineExpose({ focusInput })
const pendingImages = ref<string[]>([])

const allAgentOptions = [
  { value: AgentType.CHAT_CLIENT, label: 'LLM 工具调用', desc: '将工具调用以函数调用格式进行发送', tag: '推荐' },
  { value: AgentType.REACT_AGENT, label: 'ReAct Agent', desc: 'ReAct 推理与行动循环', tag: '实验性' },
  // { value: AgentType.DIGITAL_BYN, label: 'byn-medium-v1', desc: 'byn 的数字分身', tag: '角色扮演' },
]

const agentOptions = computed(() =>
  allAgentOptions.filter(opt => chatStore.availableAgentTypes.includes(opt.value)),
)

const enabledToolsOptions = computed(() => [
  { value: 'bocha_web_search', label: '联网搜索（博查）' },
  { value: 'web_fetch', label: '阅读网页' },
  { value: 'get_datetime', label: '日期时间' },
  { value: 'pyodide_code_runner', label: 'Python 执行' },
  { value: 'memory_tools', label: '长期记忆' },
  { value: 'rag_knowledge_base', label: 'RAG 知识库' },
])

const enabledSkillsOptions = computed<{ value: string; label: string }[]>(() => [
  // Skills will be added here in the future
])

const enabledCount = computed(
  () => chatStore.enabledTools.length + chatStore.enabledSkills.length,
)

const currentAgentLabel = computed(
  () => agentOptions.value.find((o) => o.value === chatStore.currentAgentType)?.label ?? '',
)

// Popover visibility
const agentPopoverVisible = ref(false)
const toolsPopoverVisible = ref(false)
const toolsGearVisible = ref(false)

function applyToolScheme(tools: string[]) {
  const tab = chatStore.getActiveTab()
  if (!tab) return
  tab.enabledTools = tools.filter(
    t => t !== 'rag_knowledge_base' || chatStore.ragModuleAuthorized,
  )
  toolsGearVisible.value = false
  focusInput()
}

const isPyodideEnabled = computed(() =>
  chatStore.enabledTools.includes('pyodide_code_runner'),
)

// 记住推理级别设置
onMounted(() => {
  if (rememberReasoningEffort.value && savedReasoningEffort.value) {
    chatStore.reasoningEffort = savedReasoningEffort.value
  }
})
watch(() => chatStore.reasoningEffort, (val) => {
  if (rememberReasoningEffort.value) {
    savedReasoningEffort.value = val
  }
})

const reasoningEffortOptions = [
  { value: '', label: '自动' },
  { value: 'none', label: 'none' },
  { value: 'minimal', label: 'minimal' },
  { value: 'low', label: 'low' },
  { value: 'medium', label: 'medium' },
  { value: 'high', label: 'high' },
  { value: 'xhigh', label: 'xhigh' },
  { value: 'max', label: 'max' },
]
const ragLibraryOptions = ref<{ value: string; label: string }[]>([])
let ragLibrariesLoaded = false

function refreshRagLibraries() {
  ragLibrariesLoaded = false
  loadRagLibraries()
  message.success('知识库列表已刷新')
}

async function loadRagLibraries() {
  if (ragLibrariesLoaded) return
  try {
    const res = await listAllRagLibrariesApi()
    ragLibraryOptions.value = (res.data.data || []).map((lib: any) => ({
      value: lib.id,
      label: lib.name,
    }))
    ragLibrariesLoaded = true
  } catch {
    console.error('加载知识库列表失败')
  }
}

const effortPopoverVisible = ref(false)
const currentEffortLabel = computed(() => {
  if (!chatStore.reasoningEffort) return '自动'
  return chatStore.reasoningEffort
})

const MOUNT_WARNING_KEY = 'pyodide-mount-warning-dismissed'

function isMountWarningSuppressed(): boolean {
  try {
    const raw = sessionStorage.getItem(MOUNT_WARNING_KEY)
    if (!raw) return false
    const data = JSON.parse(raw)
    const today = new Date().toISOString().slice(0, 10)
    return data.date === today
  } catch {
    return false
  }
}

function suppressMountWarningToday(): void {
  sessionStorage.setItem(MOUNT_WARNING_KEY, JSON.stringify({ date: new Date().toISOString().slice(0, 10) }))
}

async function handleSelectWorkspace() {
  if (!isMountWarningSuppressed()) {
    const confirmed = await showMountSafetyWarning()
    if (!confirmed) return
  }
  try {
    await pyodide.selectWorkspace()
  } catch {
    // User cancelled directory picker or permission denied
  }
  focusInput()
}

function showMountSafetyWarning(): Promise<boolean> {
  return new Promise((resolve) => {
    let dontShowAgain = false

    const modal = Modal.confirm({
      title: '安全提示',
      icon: null,
      width: 480,
      centered: true,
      content: h('div', { style: 'line-height: 1.7; font-size: 14px; color: #595959;' }, [
        '建议你在本机创建一个专门的目录作为 LLM 的工作区，并将此目录进行挂载。',
        h('span', { style: 'color: #cf1322; font-weight: 600;' },
          '强烈不建议挂载敏感目录或系统目录，可能导致敏感凭据被 LLM 明文读取而产生泄漏风险、因虚拟文件系统同步造成选中的目录结构损坏甚至数据丢失。',
        ),
        h('div', { style: 'margin-top: 16px;' }, [
          h(Checkbox, {
            onChange: (e: any) => { dontShowAgain = e.target.checked },
          }, {
            default: () => '今天不再提示',
          }),
        ]),
      ]),
      okText: '确定',
      footer: () =>
        h('div', { style: 'display: flex; justify-content: flex-end; gap: 8px;' }, [
          h(Button, { key: 'cancel', onClick: () => { modal.destroy(); resolve(false) } }, () => '取消'),
          h(Button, { key: 'ok', type: 'primary', onClick: () => {
            if (dontShowAgain) suppressMountWarningToday()
            modal.destroy()
            resolve(true)
          } }, () => '确定'),
        ]),
    })
  })
}

function isToolActive(tool: { value: string }): boolean {
  if (tool.value === 'rag_knowledge_base' && !chatStore.ragModuleAuthorized) return false
  return chatStore.enabledTools.includes(tool.value)
}

function handleToolToggle(toolId: string) {
  const wasEnabled = chatStore.enabledTools.includes(toolId)
  chatStore.toggleTool(toolId)
  // Pre-initialize Pyodide when the tool is enabled
  if (!wasEnabled && toolId === 'pyodide_code_runner') {
    pyodide.initPyodide()
  }
  if (toolId === 'rag_knowledge_base') {
    if (!wasEnabled) {
      // 启用时强制设为手动选择模式，并加载知识库列表
      loadRagLibraries()
    } else {
      // 关闭时清空选择
      chatStore.setRagLibraryIds([])
    }
  }
  focusInput()
}

function showMountNotSupportedModal() {
  Modal.info({
    title: '当前浏览器无法挂载真实本机目录',
    centered: true,
    content: h('div', { style: 'line-height: 1.7; font-size: 14px; color: #595959;' }, [
      '如果是 Firefox 浏览器，可安装',
      h('a', { href: 'https://addons.mozilla.org/en-US/firefox/addon/file-system-access/', target: '_blank', rel: 'noopener noreferrer' }, '第三方支持插件'),
      '；或更换 Chrome 等支持 File System Access API 的浏览器。',
    ]),
  })
}

function hasUsedPyodideInConversation(): boolean {
  return chatStore.currentMessages.some(
    (m) => m.toolContent?.includes('pyodide_code_runner'),
  )
}

async function handleSend() {
  let text = inputText.value.trim()
  const images = pendingImages.value
  if ((!text && !images.length) || chatStore.isStreaming) return

  // Prepend quoted text if present
  if (quotingText.value) {
    const quoteLines = quotingText.value.split('\n').map(line => `> ${line}`).join('\n')
    text = `${quoteLines}\n\n${text}`
    cancelQuote()
  }

  chatStore.sendMessage(text || '请描述这些图片', images.length ? images : undefined)
  pendingImages.value = []
  // a-textarea 的内部事件处理会在 keydown 期间回写旧值，
  // 必须延迟到事件处理完毕后再清空
  setTimeout(() => {
    inputText.value = ''
  })
}

/** 手机端 UA 检测：手机上 Enter 应换行而非发送 */
const isMobileUA = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
  navigator.userAgent,
)

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.isComposing) {
    // 手机端：Enter 始终换行（不发送）
    if (isMobileUA) return
    if (ctrlEnterToSend.value) {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        handleSend()
      }
      // Otherwise (Enter without Ctrl): let the browser insert newline
    } else {
      if (!e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    }
  }
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files?.length) return

  for (const file of files) {
    const reader = new FileReader()
    reader.onload = () => {
      pendingImages.value.push(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  target.value = ''
}

function removePendingImage(index: number) {
  pendingImages.value.splice(index, 1)
}

function autoResize() {
  const textarea = document.querySelector(
    '.chat-input__textarea textarea',
  ) as HTMLTextAreaElement | null
  if (!textarea) return
  textarea.style.height = 'auto'
  textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`
}

function handleInput() {
  autoResize()
}
</script>

<template>
  <div class="chat-input">
    <div class="chat-input__container">
      <div v-if="quotingText" class="chat-input__quote-bar">
        <span class="chat-input__quote-text">正在回复"{{ quotingText.length > 50 ? quotingText.slice(0, 50) + '...' : quotingText }}"</span>
        <button class="chat-input__quote-cancel" @click="cancelQuote">
          <CloseCircleOutlined />
          <span>放弃回复</span>
        </button>
      </div>
      <div class="chat-input__wrapper">
        <a-dropdown trigger="click" placement="topLeft" overlay-class-name="chat-input__popover">
          <button class="chat-input__icon-btn">
            <PlusOutlined />
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="upload" @click="triggerFileUpload">
                <PictureOutlined /> 上传图片
              </a-menu-item>
              <a-sub-menu key="reasoning" title="推理级别">
                <a-menu-item
                  v-for="opt in reasoningEffortOptions"
                  :key="'effort-' + opt.value"
                  @click="chatStore.reasoningEffort = opt.value || null"
                >{{ opt.label }}</a-menu-item>
              </a-sub-menu>
            </a-menu>
          </template>
        </a-dropdown>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          multiple
          class="chat-input__file"
          @change="handleFileChange"
        />

        <div class="chat-input__textarea">
          <a-textarea
            ref="textareaRef"
            v-model:value="inputText"
            placeholder="输入消息..."
            :auto-size="{ minRows: 1, maxRows: 4 }"
            :disabled="chatStore.isStreaming"
            @keydown="handleKeyDown"
            @input="handleInput"
          />
        </div>

        <button
          v-if="chatStore.isStreaming"
          class="chat-input__icon-btn chat-input__icon-btn--stop"
          @click="chatStore.stopStreaming()"
        >
          <PauseCircleOutlined />
        </button>

        <button
          class="chat-input__icon-btn chat-input__icon-btn--send"
          :disabled="(!inputText.trim() && !pendingImages.length) || chatStore.isStreaming"
          @click="handleSend"
        >
          <SendOutlined />
        </button>
      </div>

      <div v-if="pendingImages.length" class="chat-input__preview">
        <div v-for="(img, idx) in pendingImages" :key="idx" class="chat-input__preview-item">
          <img :src="img" alt="待发送图片" class="chat-input__preview-img" />
          <button class="chat-input__preview-remove" @click="removePendingImage(idx)">
            <CloseCircleOutlined />
          </button>
        </div>
      </div>

      <div class="chat-input__toolbar">
        <!-- Agent Harness 选择 -->
        <a-popover
          v-model:open="agentPopoverVisible"
          trigger="click"
          placement="topLeft"
          overlay-class-name="chat-input__popover"
        >
          <template #content>
            <div class="chip-menu">
              <div
                v-for="opt in agentOptions"
                :key="opt.value"
                class="chip-menu__item chip-menu__item--two-line"
                :class="{ 'chip-menu__item--active': chatStore.currentAgentType === opt.value }"
                @click="
                  chatStore.currentAgentType = opt.value;
                  agentPopoverVisible = false;
                  focusInput()
                "
              >
                <div class="chip-menu__text">
                  <div class="chip-menu__label-row">
                    <span class="chip-menu__label">{{ opt.label }}</span>
                    <span class="chip-menu__tag">{{ opt.tag }}</span>
                  </div>
                  <span class="chip-menu__desc">{{ opt.desc }}</span>
                </div>
                <span v-if="chatStore.currentAgentType === opt.value" class="chip-menu__check"
                  >✓</span
                >
              </div>
            </div>
          </template>
          <button class="toolbar-chip">
            <span class="toolbar-chip__label">调用模式</span>
            <span class="toolbar-chip__value">{{ currentAgentLabel }}</span>
            <span class="toolbar-chip__arrow">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
          </button>
        </a-popover>

        <!-- 工具列表 -->
        <a-popover
          v-model:open="toolsPopoverVisible"
          trigger="click"
          placement="topLeft"
          overlay-class-name="chat-input__popover"
        >
          <template #content>
            <div class="chip-menu">
              <div class="chip-menu__group">
                <div class="chip-menu__group-title">工具</div>
                 <div
                   v-for="tool in enabledToolsOptions"
                   :key="tool.value"
                   class="chip-menu__item chip-menu__item--checkbox"
                   :class="{
                     'chip-menu__item--active': isToolActive(tool),
                     'chip-menu__item--disabled': tool.value === 'rag_knowledge_base' && !chatStore.ragModuleAuthorized,
                   }"
                   @click="handleToolToggle(tool.value)"
                 >
                   <span class="chip-menu__checkbox">
                     <svg
                       v-if="isToolActive(tool)"
                       width="14"
                       height="14"
                       viewBox="0 0 14 14"
                     >
                       <rect width="14" height="14" rx="3" fill="#1677ff" />
                       <path d="M4 7L6.5 9.5L10 4.5" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                     </svg>
                     <svg v-else width="14" height="14" viewBox="0 0 14 14">
                       <rect width="14" height="14" rx="3" fill="none" stroke="#d9d9d9" stroke-width="1.2" />
                     </svg>
                   </span>
                   <span class="chip-menu__label">
                     <template v-if="tool.value === 'rag_knowledge_base' && !chatStore.ragModuleAuthorized">
                       <a-tooltip placement="right">
                         <template #title>当前角色无权限使用此工具，请联系管理员授权</template>
                         <span class="chip-menu__label--disabled">{{ tool.label }}</span>
                       </a-tooltip>
                     </template>
                     <template v-else>{{ tool.label }}</template>
                   </span>
                  <a-tooltip v-if="tool.value === 'pyodide_code_runner'" placement="topRight">
                    <template #title>
                      <div style="max-width: 280px; white-space: pre-line;">
                         默认在沙盒环境中进行操作，可选挂载本机真实目录（仅 Chrome 支持）。环境中的文件列表会在右边的文件管理器侧边栏中显示。
                      </div>
                    </template>
                    <QuestionCircleOutlined class="chip-menu__help" @click.stop />
                  </a-tooltip>
                </div>
              </div>
              <div v-if="enabledSkillsOptions.length" class="chip-menu__group">
                <div class="chip-menu__group-title">Skill</div>
                <div
                  v-for="skill in enabledSkillsOptions"
                  :key="skill.value"
                  class="chip-menu__item chip-menu__item--checkbox"
                  :class="{ 'chip-menu__item--active': chatStore.enabledSkills.includes(skill.value) }"
                  @click="chatStore.toggleSkill(skill.value); focusInput()"
                >
                  <span class="chip-menu__checkbox">
                    <svg
                      v-if="chatStore.enabledSkills.includes(skill.value)"
                      width="14"
                      height="14"
                      viewBox="0 0 14 14"
                    >
                      <rect width="14" height="14" rx="3" fill="#1677ff" />
                      <path d="M4 7L6.5 9.5L10 4.5" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <svg v-else width="14" height="14" viewBox="0 0 14 14">
                      <rect width="14" height="14" rx="3" fill="none" stroke="#d9d9d9" stroke-width="1.2" />
                    </svg>
                  </span>
                  <span class="chip-menu__label">{{ skill.label }}</span>
                </div>
              </div>
            </div>
          </template>
          <button class="toolbar-chip toolbar-chip--tools">
            <span class="toolbar-chip__label">模型能力</span>
            <span class="toolbar-chip__value">已启用 {{ enabledCount }} 项</span>
            <span class="toolbar-chip__arrow">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
          </button>
        </a-popover>

        <!-- 模型能力方案 -->
        <a-popover
          v-model:open="toolsGearVisible"
          trigger="click"
          placement="topLeft"
          overlay-class-name="chat-input__popover"
        >
          <template #content>
            <div class="chip-menu">
              <div class="chip-menu__item" @click="applyToolScheme([])">
                <DeleteOutlined style="font-size: 14px; color: #8c8c8c;" />
                <span class="chip-menu__label">禁用所有能力</span>
              </div>
              <div class="chip-menu__item" @click="applyToolScheme(['bocha_web_search', 'web_fetch', 'get_datetime', 'pyodide_code_runner', 'memory_tools', 'rag_knowledge_base'])">
                <RocketOutlined style="font-size: 14px; color: #1677ff;" />
                <span class="chip-menu__label">启用全部能力</span>
              </div>
              <div class="chip-menu__group chip-menu__group--scheme">
                <div class="chip-menu__group-title">能力方案</div>
                <a-tooltip placement="right" title="日期时间、长期记忆">
                  <div class="chip-menu__item" @click="applyToolScheme(['get_datetime', 'memory_tools'])">
                    <StarOutlined style="font-size: 14px; color: #faad14;" />
                    <span class="chip-menu__label">最小化能力</span>
                  </div>
                </a-tooltip>
                <a-tooltip placement="right" title="联网搜索、阅读网页、日期时间、长期记忆">
                  <div class="chip-menu__item" @click="applyToolScheme(['bocha_web_search', 'web_fetch', 'get_datetime', 'memory_tools'])">
                    <GlobalOutlined style="font-size: 14px; color: #1677ff;" />
                    <span class="chip-menu__label">智能联网问答</span>
                  </div>
                </a-tooltip>
                <template v-if="chatStore.ragModuleAuthorized">
                  <a-tooltip placement="right" title="日期时间、长期记忆、RAG 知识库">
                    <div class="chip-menu__item" @click="applyToolScheme(['get_datetime', 'memory_tools', 'rag_knowledge_base'])">
                      <FolderOutlined style="font-size: 14px; color: #fa8c16;" />
                      <span class="chip-menu__label">本地知识库问答</span>
                    </div>
                  </a-tooltip>
                </template>
                <template v-if="chatStore.ragModuleAuthorized">
                  <a-tooltip placement="right" title="联网搜索、阅读网页、长期记忆、日期时间、RAG 知识库">
                    <div class="chip-menu__item" @click="applyToolScheme(['bocha_web_search', 'web_fetch', 'memory_tools', 'get_datetime', 'rag_knowledge_base'])">
                      <ApartmentOutlined style="font-size: 14px; color: #722ed1;" />
                      <span class="chip-menu__label">综合知识问答</span>
                    </div>
                  </a-tooltip>
                </template>
                <a-tooltip placement="right" title="Python 执行、日期时间">
                  <div class="chip-menu__item" @click="applyToolScheme(['pyodide_code_runner', 'get_datetime'])">
                    <CodeOutlined style="font-size: 14px; color: #13c2c2;" />
                    <span class="chip-menu__label">本地数据分析</span>
                  </div>
                </a-tooltip>
                <a-tooltip placement="right" title="联网搜索、阅读网页、Python 执行、日期时间">
                  <div class="chip-menu__item" @click="applyToolScheme(['bocha_web_search', 'web_fetch', 'pyodide_code_runner', 'get_datetime'])">
                    <BarChartOutlined style="font-size: 14px; color: #52c41a;" />
                    <span class="chip-menu__label">数据分析</span>
                  </div>
                </a-tooltip>
              </div>
            </div>
          </template>
          <button class="toolbar-chip toolbar-chip--gear">
            <SettingOutlined />
          </button>
        </a-popover>

        <!-- RAG 知识库选择 -->
        <template v-if="chatStore.enabledTools.includes('rag_knowledge_base')">
          <a-popover trigger="click" placement="top" @visibleChange="(visible: boolean) => { if (visible) loadRagLibraries() }">
            <template #content>
              <div style="min-width: 260px; padding: 8px 0;">
                <div style="font-size: 13px; font-weight: 500; color: #262626; margin-bottom: 8px; padding: 0 4px; display: flex; align-items: center; gap: 6px;">
                  <span>选择知识库</span>
                  <a-tooltip placement="right" :overlay-style="{ maxWidth: '320px' }">
                    <template #title>为确保查询性能和效果，建议每次会话时按需选择需要查询的知识库，避免全选造成在大量文档范围中查询导致性能降级和结果不准确问题</template>
                    <QuestionCircleOutlined style="font-size: 14px; color: #bfbfbf; cursor: help;" />
                  </a-tooltip>
                  <a-button size="small" style="margin-left: auto;" @click.stop="refreshRagLibraries">
                    <template #icon><ReloadOutlined /></template>
                    刷新列表
                  </a-button>
                </div>
                <a-select
                  :value="chatStore.ragLibraryIds"
                  mode="multiple"
                  placeholder="搜索并选择知识库..."
                  style="width: 100%"
                  :options="ragLibraryOptions"
                  @change="(val: any) => chatStore.setRagLibraryIds(val)"
                />
              </div>
            </template>
            <button class="toolbar-chip">
              <span class="toolbar-chip__label">RAG 知识库</span>
              <span class="toolbar-chip__value">已选择 {{ chatStore.ragLibraryIds.length }} 个</span>
              <span class="toolbar-chip__arrow">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                  <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
            </button>
          </a-popover>
        </template>

        <!-- Reasoning effort tag -->
        <template v-if="chatStore.reasoningEffort">
          <button class="toolbar-chip toolbar-chip--effort" @click="chatStore.reasoningEffort = null">
            <span class="toolbar-chip__label">推理级别：{{ chatStore.reasoningEffort }}</span>
            <span class="toolbar-chip__close">&times;</span>
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  flex-shrink: 0;
  padding: 12px 20px 16px;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;
}

.chat-input__container {
  max-width: 800px;
  margin: 0 auto;
}

.chat-input__quote-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin-bottom: 8px;
  background: #e6f4ff;
  border-radius: 8px;
  border: 1px solid #bae0ff;
}

.chat-input__quote-text {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #1677ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-input__quote-cancel {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: inherit;
  transition: color 0.15s, background 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.chat-input__quote-cancel:hover {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.06);
}

.chat-input__wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
  transition: border-color 0.2s;
}

.chat-input__wrapper:focus-within {
  border-color: #1677ff;
}

.chat-input__file {
  display: none;
}

.chat-input__textarea {
  flex: 1;
  min-width: 0;
}

.chat-input__textarea :deep(.ant-input) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  color: #262626;
}

.chat-input__textarea :deep(.ant-input:focus) {
  box-shadow: none;
}

.chat-input__textarea :deep(.ant-input::placeholder) {
  color: #8c8c8c;
}

.chat-input__icon-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 16px;
  cursor: pointer;
  border-radius: 6px;
  transition:
    color 0.2s,
    background 0.2s;
}

.chat-input__icon-btn:hover {
  color: #595959;
  background: rgba(0, 0, 0, 0.04);
}

.chat-input__icon-btn--send:not(:disabled) {
  color: #1677ff;
}

.chat-input__icon-btn--send:not(:disabled):hover {
  background: #e6f4ff;
}

.chat-input__icon-btn--stop {
  color: #ff4d4f;
}

.chat-input__icon-btn--stop:hover {
  background: #fff1f0;
}

.chat-input__icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ===== Toolbar: Material Design chip style ===== */
.chat-input__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.chat-input__preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.chat-input__preview-item {
  position: relative;
}

.chat-input__preview-img {
  height: 64px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.chat-input__preview-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  border: none;
  background: #ffffff;
  color: #8c8c8c;
  font-size: 16px;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: color 0.2s;
}

.chat-input__preview-remove:hover {
  color: #ff4d4f;
}

.toolbar-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  font-size: 12px;
  color: #595959;
  transition:
    background 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
}

.toolbar-chip:hover {
  background: #f0f0f0;
  border-color: #d0d0d0;
}

.toolbar-chip:active {
  background: #e8e8e8;
}

.toolbar-chip__label {
  font-weight: 400;
  color: #8c8c8c;
  font-size: 12px;
}

.toolbar-chip__value {
  color: #262626;
  font-size: 11px;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toolbar-chip__arrow {
  display: flex;
  align-items: center;
  color: #bfbfbf;
  margin-left: 2px;
}

.toolbar-chip--tools .toolbar-chip__value {
  color: #1677ff;
  font-weight: 500;
}

.toolbar-chip--gear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  color: #8c8c8c;
  font-size: 14px;
  transition:
    background 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.toolbar-chip--gear:hover {
  background: #f0f0f0;
  border-color: #d0d0d0;
  color: #595959;
}

.toolbar-chip--effort {
  background: #1677ff;
  border-color: #1677ff;
}

.toolbar-chip--effort:hover {
  background: #4096ff;
  border-color: #4096ff;
}

.toolbar-chip--effort .toolbar-chip__label {
  color: #ffffff;
}

.toolbar-chip__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  font-size: 14px;
  line-height: 1;
  color: #ffffff;
  margin-left: 4px;
  cursor: pointer;
  border-radius: 2px;
  opacity: 0.8;
}

.toolbar-chip__close:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.2);
}

.toolbar-chip--static {
  cursor: default;
}

.toolbar-chip__loading-icon {
  animation: spin 1s linear infinite;
  font-size: 12px;
}

.toolbar-chip__folder-icon {
  color: #fa8c16;
  font-size: 13px;
}

.toolbar-btn-text {
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 6px;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
  transition: color 0.2s;
}

.toolbar-btn-text:hover:not(:disabled) {
  color: #1677ff;
}

.toolbar-btn-text:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ===== Popover menu ===== */
.chip-menu {
  min-width: 160px;
  padding: 4px 0;
}

.chip-menu__group + .chip-menu__group {
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
  padding-top: 4px;
}

.chip-menu__group-title {
  padding: 4px 12px 2px;
  font-size: 11px;
  color: #8c8c8c;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.chip-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: #262626;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 6px;
  margin: 0 4px;
}

.chip-menu__item:hover {
  background: #f5f5f5;
}

.chip-menu__item--active {
  color: #1677ff;
}

.chip-menu__item--active .chip-menu__label {
  font-weight: 500;
}

.chip-menu__item--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chip-menu__label--disabled {
  cursor: not-allowed;
  color: #8c8c8c;
}

.chip-menu__group--scheme .chip-menu__item {
  justify-content: space-between;
  gap: 12px;
}

.chip-menu__help {
  font-size: 14px;
  color: #8c8c8c;
  cursor: pointer;
  margin-left: 4px;
}

.chip-menu__help:hover {
  color: #1677ff;
}

.chip-menu__help:hover {
  color: #1677ff;
}

.chip-menu__config {
  font-size: 14px;
  color: #8c8c8c;
  cursor: pointer;
  margin-left: 4px;
}

.chip-menu__config:hover {
  color: #1677ff;
}

.chip-menu__item--loading {
  color: #bfbfbf;
  pointer-events: none;
}

.chip-menu__check {
  margin-left: auto;
  color: #1677ff;
  font-size: 14px;
  font-weight: 600;
}

.chip-menu__label {
  flex: 1;
  min-width: 0;
}

.chip-menu__item--two-line {
  align-items: flex-start;
  min-width: 220px;
}

.chip-menu__item--two-line .chip-menu__label {
  flex: none;
}

.chip-menu__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.chip-menu__desc {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.3;
}

.chip-menu__label-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chip-menu__tag {
  font-size: 10px;
  padding: 0 5px;
  line-height: 16px;
  border-radius: 3px;
  background: rgba(22, 119, 255, 0.08);
  color: #1677ff;
  white-space: nowrap;
  font-weight: 400;
}

.chip-menu__item--active .chip-menu__tag {
  background: rgba(22, 119, 255, 0.12);
}

.chip-menu__item--active .chip-menu__desc {
  color: #8c8c8c;
}

.chip-menu__checkbox {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.chip-menu__item--checkbox {
  padding-left: 8px;
}

@media (max-width: 768px) {
  .chat-input {
    padding: 8px 12px 12px;
  }

  .chat-input__icon-btn {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .chat-input__toolbar {
    gap: 6px;
  }

  .toolbar-chip {
    max-width: 100%;
  }

  .toolbar-chip__label {
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }

  .toolbar-chip__value {
    max-width: 100px;
  }
}
</style>

<style>
/* Global styles for popover (scoped doesn't apply to popover content) */
.chat-input__popover .ant-popover-inner {
  padding: 4px 0 !important;
  border-radius: 10px !important;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.1),
    0 1px 3px rgba(0, 0, 0, 0.06) !important;
}

.chat-input__popover .ant-popover-inner-content {
  padding: 0 !important;
}
</style>
