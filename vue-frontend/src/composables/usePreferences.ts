import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'app-preferences'

export interface Preferences {
  showTokenCount: boolean
  showMessageTime: boolean
  wideMode: boolean
  showQuoteButton: boolean
  rememberToolSelection: boolean
  savedEnabledTools: string[] | null
  savedEnabledSkills: string[] | null
  rememberAgentType: boolean
  savedAgentType: number | null
  enableMultiTab: boolean
  ctrlEnterToSend: boolean
  simplifiedProcess: boolean
  rememberReasoningEffort: boolean
  savedReasoningEffort: string | null
}

function loadPreferences(): Preferences {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<Preferences>
      return {
        showTokenCount: parsed.showTokenCount ?? true,
        showMessageTime: parsed.showMessageTime ?? true,
        wideMode: parsed.wideMode ?? true,
        showQuoteButton: parsed.showQuoteButton ?? true,
        rememberToolSelection: parsed.rememberToolSelection ?? true,
        savedEnabledTools: parsed.savedEnabledTools ?? null,
        savedEnabledSkills: parsed.savedEnabledSkills ?? null,
        rememberAgentType: parsed.rememberAgentType ?? true,
        savedAgentType: parsed.savedAgentType ?? null,
        enableMultiTab: parsed.enableMultiTab ?? true,
        ctrlEnterToSend: parsed.ctrlEnterToSend ?? false,
        simplifiedProcess: parsed.simplifiedProcess ?? true,
        rememberReasoningEffort: parsed.rememberReasoningEffort ?? true,
        savedReasoningEffort: parsed.savedReasoningEffort ?? null,
      }
    }
  } catch {
    // ignore
  }
  return { showTokenCount: true, showMessageTime: true, wideMode: true, showQuoteButton: true, rememberToolSelection: true, savedEnabledTools: null, savedEnabledSkills: null, rememberAgentType: true, savedAgentType: null, enableMultiTab: true, ctrlEnterToSend: false, simplifiedProcess: true, rememberReasoningEffort: true, savedReasoningEffort: null }
}

function savePreferences(prefs: Preferences) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs))
}

const loaded = loadPreferences()

const showTokenCount = ref(loaded.showTokenCount)
const showMessageTime = ref(loaded.showMessageTime)
const wideMode = ref(loaded.wideMode)
const showQuoteButton = ref(loaded.showQuoteButton)
const rememberToolSelection = ref(loaded.rememberToolSelection)
const savedEnabledTools = ref<string[] | null>(loaded.savedEnabledTools)
const savedEnabledSkills = ref<string[] | null>(loaded.savedEnabledSkills)
const rememberAgentType = ref(loaded.rememberAgentType)
const savedAgentType = ref<number | null>(loaded.savedAgentType)
const enableMultiTab = ref(loaded.enableMultiTab)
const ctrlEnterToSend = ref(loaded.ctrlEnterToSend)
const simplifiedProcess = ref(loaded.simplifiedProcess)
const rememberReasoningEffort = ref(loaded.rememberReasoningEffort)
const savedReasoningEffort = ref<string | null>(loaded.savedReasoningEffort)

// Detect mobile PWA: small viewport + standalone display mode
const isMobilePwa = ref(false)

function updateMobilePwa() {
  const isMobile = window.innerWidth <= 768
  const isPwa =
    window.matchMedia('(display-mode: standalone)').matches ||
    ('standalone' in window.navigator && (window.navigator as any).standalone)
  isMobilePwa.value = isMobile && isPwa
}

updateMobilePwa()
window.addEventListener('resize', updateMobilePwa)

// Capture viewport width at load time (resize after load does not re-evaluate)
const isMobileOnLoad = typeof window !== 'undefined' ? window.innerWidth <= 768 : false

// Effective multi-tab: forced off when viewport is mobile size at load time
const effectiveMultiTab = computed(() => enableMultiTab.value && !isMobileOnLoad)

watch([showTokenCount, showMessageTime, wideMode, showQuoteButton, rememberToolSelection, savedEnabledTools, savedEnabledSkills, rememberAgentType, savedAgentType, enableMultiTab, ctrlEnterToSend, simplifiedProcess, rememberReasoningEffort, savedReasoningEffort], () => {
  savePreferences({
    showTokenCount: showTokenCount.value,
    showMessageTime: showMessageTime.value,
    wideMode: wideMode.value,
    showQuoteButton: showQuoteButton.value,
    rememberToolSelection: rememberToolSelection.value,
    savedEnabledTools: savedEnabledTools.value,
    savedEnabledSkills: savedEnabledSkills.value,
    rememberAgentType: rememberAgentType.value,
    savedAgentType: savedAgentType.value,
    enableMultiTab: enableMultiTab.value,
    ctrlEnterToSend: ctrlEnterToSend.value,
    simplifiedProcess: simplifiedProcess.value,
    rememberReasoningEffort: rememberReasoningEffort.value,
    savedReasoningEffort: savedReasoningEffort.value,
  })
})

export function usePreferences() {
  return { showTokenCount, showMessageTime, wideMode, showQuoteButton, rememberToolSelection, savedEnabledTools, savedEnabledSkills, rememberAgentType, savedAgentType, enableMultiTab, ctrlEnterToSend, simplifiedProcess, rememberReasoningEffort, savedReasoningEffort, isMobilePwa, effectiveMultiTab }
}
