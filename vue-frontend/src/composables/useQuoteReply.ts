import { ref } from 'vue'

const quotingText = ref<string | null>(null)

function startQuote(text: string) {
  quotingText.value = text
}

function cancelQuote() {
  quotingText.value = null
}

export function useQuoteReply() {
  return { quotingText, startQuote, cancelQuote }
}
