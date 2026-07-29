import { ref, onMounted } from 'vue'

export function useTypingTheme() {
  const stored = localStorage.getItem('typing-theme')
  const isDark = ref(
    stored ? stored === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  )

  function applyTheme() {
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  }

  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme()
    localStorage.setItem('typing-theme', isDark.value ? 'dark' : 'light')
  }

  onMounted(() => applyTheme())

  return { isDark, toggleTheme, applyTheme }
}
