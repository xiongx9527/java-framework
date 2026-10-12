<template>
  <div class="mica-mqtt-dashboard-app" :class="{ dark: theme === 'dark' }">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/app'

const { theme } = storeToRefs(useAppStore())
</script>

<style>
.mica-mqtt-dashboard-app {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.mica-mqtt-dashboard-app,
.mica-mqtt-dashboard-app *,
.mica-mqtt-dashboard-app *::before,
.mica-mqtt-dashboard-app *::after {
  box-sizing: border-box;
}

.mica-mqtt-dashboard-app.dark {
  color-scheme: dark;
  --el-color-primary: #409eff;
  --el-color-primary-light-3: #3375b9;
  --el-color-primary-light-5: #2a598a;
  --el-color-primary-light-7: #213d5b;
  --el-color-primary-light-8: #1d3043;
  --el-color-primary-light-9: #18222b;
  --el-color-primary-dark-2: #66b1ff;
  --el-bg-color-page: #0a0a0a;
  --el-bg-color: #141414;
  --el-bg-color-overlay: #1d1e1f;
  --el-text-color-primary: #e5eaf3;
  --el-text-color-regular: #cfd3dc;
  --el-text-color-secondary: #a3a6ad;
  --el-text-color-placeholder: #8d9095;
  --el-text-color-disabled: #6c6e72;
  --el-border-color-darker: #636466;
  --el-border-color-dark: #58585b;
  --el-border-color: #4c4d4f;
  --el-border-color-light: #414243;
  --el-border-color-lighter: #363637;
  --el-border-color-extra-light: #2b2b2c;
  --el-fill-color-darker: #424243;
  --el-fill-color-dark: #39393a;
  --el-fill-color: #303030;
  --el-fill-color-light: #262727;
  --el-fill-color-lighter: #1d1d1d;
  --el-fill-color-extra-light: #191919;
  --el-fill-color-blank: #141414;
  --el-mask-color: rgba(0, 0, 0, .8);
}
</style>
