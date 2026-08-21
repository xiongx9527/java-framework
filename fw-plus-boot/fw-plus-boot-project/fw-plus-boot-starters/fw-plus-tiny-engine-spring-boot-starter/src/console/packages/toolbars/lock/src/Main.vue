<template>
  <toolbar-base
    v-if="!state.isGuest"
    :icon="iconName"
    :content="statusMessageMap[state.status]?.nextOptName"
    :options="options"
    @click-api="lockOrUnlock"
  >
  </toolbar-base>
</template>

<script lang="ts">
/* metaService: engine.toolbars.lock.Main */
import { computed, reactive } from 'vue'
import { useCanvas, useLayout, useBlock, useNotify, getMetaApi, META_SERVICE } from '@opentiny/tiny-engine-meta-register'
import { constants } from '@opentiny/tiny-engine-utils'
import { ToolbarBase } from '@opentiny/tiny-engine-common'
import { getCanvasStatus } from '@opentiny/tiny-engine-common/js/canvas'
import { requestBlockPage } from './http'

const { COMPONENT_NAME, PAGE_STATUS } = constants

const componentType: Record<string, any> = {
  block: '区块',
  page: '页面'
}

const statusMessageMap = {
  [PAGE_STATUS.Release]: {
    message: (type: string) => `${componentType[type]}解锁成功！`,
    currentOptName: '解锁',
    nextOptName: '锁定'
  },
  [PAGE_STATUS.Lock]: {
    message: (type: string) => `${componentType[type]}解锁成功！`,
    currentOptName: '解锁',
    nextOptName: '解锁'
  },
  [PAGE_STATUS.Occupy]: {
    message: (type: string) => `${componentType[type]}锁定成功`,
    currentOptName: '锁定',
    nextOptName: '解锁'
  }
}

const isValidLockTargetId = (id: any) => id !== undefined && id !== null && id !== ''

export default {
  components: {
    ToolbarBase
  },
  props: {
    options: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props) {
    const { pageState } = useCanvas()
    const { layoutState } = useLayout()
    const { getCurrentBlock } = useBlock()

    const state = reactive({
      status: computed(() => layoutState.pageStatus.state),
      isGuest: computed(() => layoutState.pageStatus.state === PAGE_STATUS.Guest),
      type: '',
      disabled: false
    })

    const iconName = computed(() => {
      switch (state.status) {
        case PAGE_STATUS.Occupy:
          return props.options.icon.locked
        case PAGE_STATUS.Lock:
          return props.options.icon.userLocked
        default:
          return props.options.icon.unlocked
      }
    })

    const syncLockStatus = (type: string, newState: string) => {
      const occupier = newState === PAGE_STATUS.Occupy ? getMetaApi(META_SERVICE.GlobalService).getUserInfo() : undefined

      if (type === 'page' && pageState.currentPage) {
        pageState.currentPage.occupier = occupier
      }

      if (type === 'block') {
        const currentBlock = getCurrentBlock()
        if (currentBlock) {
          currentBlock.occupier = occupier
        }
      }

      layoutState.pageStatus = getCanvasStatus(occupier)
    }

    const lockPage = (id: any, type: string, newState: string) => {
      if (!isValidLockTargetId(id)) {
        useNotify({
          type: 'warning',
          message: type === 'page' ? '当前应用还没有可锁定的页面，请先新建页面' : '当前区块不存在，无法锁定'
        })
        state.disabled = false
        return
      }

      requestBlockPage(`id=${id}&state=${newState}&type=${type}`)
        .then((data: { operate: string }) => {
          if (data?.operate === 'success') {
            useNotify({
              type: 'success',
              message: statusMessageMap[newState].message(type)
            })
            syncLockStatus(type, newState)
          } else {
            const pageInfo = layoutState.pageStatus?.data
            useNotify({
              type: 'warning',
              message: `当前页面被 ${pageInfo?.username || ''} 锁定，请联系解锁`
            })
          }
        })
        .catch((error: { message: any }) => {
          useNotify({
            type: 'error',
            title: `页面${statusMessageMap[newState].currentOptName}失败`,
            message: JSON.stringify(error?.message || error)
          })
        })
        .finally(() => {
          state.disabled = false
        })
    }

    const lockOrUnlock = () => {
      if (state.disabled) {
        return
      }

      const optType =
        state.status === PAGE_STATUS.Occupy || state.status === PAGE_STATUS.Lock
          ? PAGE_STATUS.Release
          : PAGE_STATUS.Occupy

      pageState.currentSchema = {}
      pageState.properties = null

      state.disabled = true

      if (pageState.pageSchema?.componentName !== COMPONENT_NAME.Block) {
        const currentPageId = pageState.currentPage?.id ?? pageState.pageSchema?.id
        lockPage(currentPageId, 'page', optType)
      } else {
        lockPage(getCurrentBlock()?.id, 'block', optType)
      }
    }

    return {
      state,
      lockOrUnlock,
      PAGE_STATUS,
      statusMessageMap,
      iconName
    }
  }
}
</script>
