<script setup lang="ts">
import { computed } from 'vue'
import { GlobalOutlined } from '@ant-design/icons-vue'
import type { SearchResultItem } from '@/types/chat'

const props = defineProps<{
  item: SearchResultItem
}>()

const faviconSrc = computed(() => {
  if (props.item.siteIcon) return props.item.siteIcon
  try {
    const url = new URL(props.item.url)
    return `https://www.google.com/s2/favicons?domain=${url.hostname}&sz=16`
  } catch {
    return ''
  }
})

const formattedDate = computed(() => {
  if (!props.item.datePublished) return ''
  try {
    return new Date(props.item.datePublished).toLocaleDateString()
  } catch {
    return props.item.datePublished
  }
})

const snippet = computed(() => props.item.snippet || props.item.summary || '')
</script>

<template>
  <div class="search-result-card">
    <div class="search-result-card__meta">
      <img v-if="faviconSrc" :src="faviconSrc" alt="" class="search-result-card__favicon" />
      <GlobalOutlined v-else class="search-result-card__fallback-icon" />
      <span class="search-result-card__site">{{ item.siteName }}</span>
      <span v-if="formattedDate" class="search-result-card__date">{{ formattedDate }}</span>
    </div>

    <a :href="item.url" target="_blank" rel="noopener noreferrer" class="search-result-card__title">
      {{ item.title }}
    </a>

    <p v-if="snippet" class="search-result-card__snippet">{{ snippet }}</p>
  </div>
</template>

<style scoped>
.search-result-card {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #ffffff;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.search-result-card:hover {
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-result-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.search-result-card__favicon {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  object-fit: contain;
}

.search-result-card__fallback-icon {
  font-size: 14px;
  color: #8c8c8c;
}

.search-result-card__site {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-card__date {
  font-size: 11px;
  color: #8c8c8c;
  margin-left: auto;
  flex-shrink: 0;
}

.search-result-card__title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1677ff;
  text-decoration: none;
  line-height: 1.4;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-card__title:hover {
  text-decoration: underline;
}

.search-result-card__snippet {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #595959;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 768px) {
  .search-result-card {
    padding: 8px 10px;
  }
}
</style>
