<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { BulbOutlined } from '@ant-design/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import { expandedBlocks } from '@/stores/blockState'

const props = defineProps<{
  content: string
  active?: boolean
  blockKey?: string
}>()

const resolvedBlockKey = computed(() => props.blockKey ?? '')

const manuallyOpened = ref(expandedBlocks.get(resolvedBlockKey.value) ?? false)

// Sync local ref when blockKey resolves (e.g. on mount after streaming→regular transition)
watch(resolvedBlockKey, (key) => {
  if (key && expandedBlocks.has(key)) {
    manuallyOpened.value = expandedBlocks.get(key)!
  }
}, { immediate: true })

const activeKeys = computed(() => {
  if (props.active) return ['1']
  if (manuallyOpened.value) return ['1']
  return []
})

function handleCollapseChange(keys: string[] | string) {
  const arr = Array.isArray(keys) ? keys : [keys]
  const expanded = arr.includes('1')
  manuallyOpened.value = expanded
  if (resolvedBlockKey.value) {
    expandedBlocks.set(resolvedBlockKey.value, expanded)
  }
}

// When the system auto-opens, reset any manual state
watch(() => props.active, (val) => {
  if (val) {
    manuallyOpened.value = false
  }
})
</script>

<template>
  <div class="reasoning-block">
    <a-collapse :activeKey="activeKeys" @change="handleCollapseChange" :bordered="false" class="reasoning-block__collapse">
      <a-collapse-panel key="1">
        <template #header>
          <div class="reasoning-block__header">
            <BulbOutlined class="reasoning-block__icon" />
            <span class="reasoning-block__label">思考过程</span>
          </div>
        </template>

        <div class="reasoning-block__content" v-html="renderMarkdown(content)" />
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<style scoped>
.reasoning-block {
  margin: 4px 0;
}

.reasoning-block__collapse {
  background: transparent;
}

.reasoning-block__collapse :deep(.ant-collapse-header) {
  padding: 8px 12px !important;
  align-items: center !important;
}

.reasoning-block__collapse :deep(.ant-collapse-content-box) {
  padding: 8px 12px !important;
}

.reasoning-block__collapse :deep(.ant-collapse-item) {
  border: none;
}

.reasoning-block__collapse :deep(.ant-collapse) {
  border: none;
  background: #fffbe6;
  border-radius: 8px;
}

.reasoning-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reasoning-block__icon {
  color: #faad14;
  font-size: 14px;
}

.reasoning-block__label {
  font-size: 13px;
  font-weight: 500;
  color: #595959;
}

.reasoning-block__content {
  font-size: 13px;
  line-height: 1.6;
  color: #595959;
}

.reasoning-block__content :deep(p) {
  margin: 0 0 6px;
}

.reasoning-block__content :deep(p:last-child) {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .reasoning-block__collapse :deep(.ant-collapse-header) {
    padding: 6px 10px !important;
  }

  .reasoning-block__collapse :deep(.ant-collapse-content-box) {
    padding: 6px 10px !important;
  }
}
</style>
