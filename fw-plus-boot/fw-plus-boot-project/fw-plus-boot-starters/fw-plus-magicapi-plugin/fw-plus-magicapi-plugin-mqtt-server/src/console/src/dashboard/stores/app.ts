import { defineStore } from 'pinia'
import { ref } from 'vue'

const THEME_KEY = 'magic-mqtt-dashboard-theme'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const sidebarCollapsed = ref(false)

  const initTheme = () => {
    const savedTheme = localStorage.getItem(THEME_KEY) as 'light' | 'dark' | null
    theme.value = savedTheme || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  }

  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem(THEME_KEY, theme.value)
  }

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { loading, theme, sidebarCollapsed, setLoading, toggleTheme, toggleSidebar, initTheme }
})
