<template>
  <el-container class="layout-container" :class="{ dark: theme === 'dark' }">
    <el-aside :width="sidebarCollapsed ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <img :src="logoUrl" alt="MICA MQTT" v-if="!sidebarCollapsed" />
        <span v-if="!sidebarCollapsed">MICA MQTT</span>
      </div>

      <el-menu
        :default-active="$route.path"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard"><el-icon><House /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/clients"><el-icon><User /></el-icon><span>客户端管理</span></el-menu-item>
        <el-menu-item index="/monitor"><el-icon><Monitor /></el-icon><span>状态监控</span></el-menu-item>
        <el-menu-item index="/debug"><el-icon><Tools /></el-icon><span>MQTT 调试</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="content-container">
      <el-header class="header">
        <div class="header-left">
          <el-button link @click="toggleSidebar" class="sidebar-toggle">
            <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ $route.meta.title || 'Mica MQTT Dashboard' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-alert v-if="serverError" :title="serverError" type="error" :closable="false" show-icon />
          <el-select
            v-model="selectedServerId"
            :teleported="false"
            class="server-select"
            placeholder="选择 MQTT 服务端"
            :loading="serverLoading"
            clearable
            @change="changeServer"
          >
            <el-option
              v-for="server in servers"
              :key="server.id"
              :label="server.defaultServer ? `${server.name}（默认）` : server.name"
              :value="server.id"
            />
          </el-select>
          <el-button link :loading="serverLoading" title="刷新服务端" @click="serverStore.loadServers()">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button link @click="toggleTheme" class="theme-toggle">
            <el-icon><Sunny v-if="theme === 'light'" /><Moon v-else /></el-icon>
          </el-button>
          <el-dropdown :teleported="false" @command="handleCommand">
            <div class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ authStore.username }}</span>
              <el-icon class="arrow-down"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <el-empty v-if="!selectedServer && !serverLoading" description="没有可用的 MQTT 服务端" />
        <router-view v-else :key="`${selectedServerId}:${generation}`" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessageBox, ElMessage } from 'element-plus'
import { House, User, Monitor, Tools, Fold, Expand, Sunny, Moon, ArrowDown, SwitchButton, Refresh } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { useServerStore } from '@/stores/server'
import logoUrl from '@/logo.png'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const serverStore = useServerStore()
const { sidebarCollapsed, theme } = storeToRefs(appStore)
const { servers, selectedServerId, selectedServer, loading: serverLoading, error: serverError, generation } = storeToRefs(serverStore)
const { toggleSidebar, toggleTheme, initTheme } = appStore

onMounted(async () => {
  initTheme()
  await serverStore.initialize()
})

const changeServer = (serverId: string) => {
  serverStore.selectServer(serverId || '')
  if (router.currentRoute.value.name === 'ClientDetail') router.push('/clients')
}

const handleCommand = async (command: string) => {
  if (command !== 'logout') return
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    authStore.logout()
    ElMessage.success('已退出登录')
    if (authStore.mode !== 'host') router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.layout-container { height: 100%; min-height: 0; }
.content-container { min-width: 0; min-height: 0; }
.sidebar { background-color: var(--el-bg-color); border-right: 1px solid var(--el-border-color); transition: width .3s; }
.logo { display: flex; align-items: center; min-height: 64px; padding: 16px; font-size: 18px; font-weight: bold; color: var(--el-text-color-primary); }
.logo img { width: 32px; height: 32px; margin-right: 8px; }
.sidebar-menu { border-right: none; }
.header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 20px; background: var(--el-bg-color); border-bottom: 1px solid var(--el-border-color); }
.header-left, .header-right { display: flex; align-items: center; gap: 12px; min-width: 0; }
.server-select { width: 240px; }
.header-right :deep(.el-alert) { width: auto; max-width: 320px; padding: 4px 10px; }
.user-info { display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer; border-radius: 4px; }
.user-info:hover { background-color: var(--el-fill-color-light); }
.arrow-down { font-size: 12px; }
.main-content { min-height: 0; overflow: auto; background-color: var(--el-bg-color-page); padding: 20px; }
</style>
