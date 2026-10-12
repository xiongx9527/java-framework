<!--
  Thin Magic-API host wrapper for the upstream mica-mqtt-dashboard application.
  Upstream: https://gitee.com/dreamlu/mica-mqtt-dashboard
  Commit: 2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80
  Licensed under Apache-2.0. See ../dashboard/LICENSE.
-->
<template>
  <div ref="dashboardRoot" class="magic-mqtt-dashboard-host"></div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { createMicaMqttDashboard } from '../dashboard/bootstrap'

const dashboardRoot = ref(null)
let dashboard = null

onMounted(() => {
  dashboard = createMicaMqttDashboard(dashboardRoot.value)
})

onUnmounted(() => {
  if (dashboard) {
    dashboard.unmount()
    dashboard = null
  }
})
</script>

<style scoped>
.magic-mqtt-dashboard-host {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
</style>
