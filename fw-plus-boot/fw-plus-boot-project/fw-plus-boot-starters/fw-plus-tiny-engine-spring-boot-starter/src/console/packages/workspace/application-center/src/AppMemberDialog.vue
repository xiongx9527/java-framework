<template>
  <tiny-dialog-box
    :visible="visible"
    :title="`应用成员 - ${application?.name || ''}`"
    width="720px"
    append-to-body
    destroy-on-close
    @close="close"
  >
    <div class="member-add">
      <tiny-input v-model="newMember.userId" placeholder="输入当前租户用户 ID" clearable></tiny-input>
      <tiny-select v-model="newMember.roleCode" :options="roleOptions"></tiny-select>
      <tiny-button type="primary" :disabled="!newMember.userId || mutating" @click="addMember">添加成员</tiny-button>
    </div>

    <div v-if="loading" class="member-state">正在加载成员...</div>
    <div v-else-if="loadError" class="member-state error-state">
      <span>{{ loadError }}</span>
      <tiny-button type="text" @click="loadMembers">重试</tiny-button>
    </div>
    <div v-else-if="!members.length" class="member-state">暂无成员</div>
    <div v-else class="member-list">
      <div class="member-row member-header">
        <span>用户</span>
        <span>角色</span>
        <span>有效期</span>
        <span>操作</span>
      </div>
      <div v-for="member in members" :key="member.userId" class="member-row">
        <span class="member-user">
          <strong>{{ member.username || member.userId }}</strong>
          <small v-if="member.username">{{ member.userId }}</small>
        </span>
        <span>
          <span v-if="isProtected(member)">{{ roleLabel(member.roleCode) }}</span>
          <tiny-select v-else v-model="member.pendingRoleCode" :options="roleOptions"></tiny-select>
        </span>
        <span>{{ member.expiredTime || '长期有效' }}</span>
        <span class="member-actions">
          <span v-if="member.roleCode === 'app_owner'" class="protected-label">Owner 不可操作</span>
          <span v-else-if="isCurrentUser(member)" class="protected-label">不可操作自己</span>
          <template v-else>
            <tiny-button
              type="text"
              :disabled="mutating || member.pendingRoleCode === member.roleCode"
              @click="changeMember(member)"
            >
              保存
            </tiny-button>
            <tiny-button type="text" :disabled="mutating" @click="removeMember(member)">移除</tiny-button>
          </template>
        </span>
      </div>
    </div>

    <template #footer>
      <tiny-button @click="close">关闭</tiny-button>
    </template>
  </tiny-dialog-box>
</template>

<script>
import { reactive, ref, watch } from 'vue'
import { Button, DialogBox, Input, Modal, Notify, Select } from '@opentiny/vue'
import { addAppMember, deleteAppMember, fetchAppMembers, updateAppMember } from './js/http'

const ROLE_OPTIONS = [
  { label: '应用管理者', value: 'app_admin' },
  { label: '应用开发者', value: 'app_dev' }
]

export default {
  components: {
    TinyButton: Button,
    TinyDialogBox: DialogBox,
    TinyInput: Input,
    TinySelect: Select
  },
  props: {
    visible: { type: Boolean, default: false },
    application: { type: Object, default: null },
    currentUserId: { type: [String, Number], default: '' }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    const members = ref([])
    const loading = ref(false)
    const mutating = ref(false)
    const loadError = ref('')
    const newMember = reactive({ userId: '', roleCode: 'app_dev' })

    const errorMessage = (error, fallback) =>
      error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback

    const notifyError = (error, fallback) => {
      Notify({ type: 'error', message: errorMessage(error, fallback), position: 'top-right', duration: 5000 })
    }

    const loadMembers = async () => {
      if (!props.application?.id) return
      loading.value = true
      loadError.value = ''
      try {
        const result = await fetchAppMembers(props.application.id)
        members.value = (Array.isArray(result) ? result : result?.members || []).map((member) => ({
          ...member,
          pendingRoleCode: member.roleCode
        }))
      } catch (error) {
        loadError.value = errorMessage(error, '成员列表加载失败')
      } finally {
        loading.value = false
      }
    }

    const addMember = async () => {
      const userId = String(newMember.userId || '').trim()
      if (!userId || String(props.currentUserId) === userId) {
        notifyError(null, !userId ? '请输入用户 ID' : '不可操作自己')
        return
      }
      if (members.value.some((member) => String(member.userId) === userId)) {
        notifyError(null, '该用户已是应用成员')
        return
      }
      mutating.value = true
      try {
        await addAppMember(props.application.id, { userId, roleCode: newMember.roleCode })
        newMember.userId = ''
        await loadMembers()
      } catch (error) {
        notifyError(error, '添加成员失败')
      } finally {
        mutating.value = false
      }
    }

    const changeMember = async (member) => {
      if (isProtected(member)) return
      mutating.value = true
      try {
        await updateAppMember(props.application.id, member.userId, { roleCode: member.pendingRoleCode })
        await loadMembers()
      } catch (error) {
        notifyError(error, '修改成员角色失败')
      } finally {
        mutating.value = false
      }
    }

    const removeMember = (member) => {
      if (isProtected(member)) return
      Modal.confirm({ title: '移除成员', message: `确认移除“${member.username || member.userId}”？` }).then(
        async (result) => {
          if (result !== 'confirm') return
          mutating.value = true
          try {
            await deleteAppMember(props.application.id, member.userId)
            await loadMembers()
          } catch (error) {
            notifyError(error, '移除成员失败')
          } finally {
            mutating.value = false
          }
        }
      )
    }

    const isCurrentUser = (member) => String(member.userId) === String(props.currentUserId)
    const isProtected = (member) => member.roleCode === 'app_owner' || isCurrentUser(member)
    const roleLabel = (roleCode) =>
      ({ app_owner: '应用创建者', app_admin: '应用管理者', app_dev: '应用开发者' })[roleCode] || roleCode
    const close = () => emit('update:visible', false)

    watch(
      () => [props.visible, props.application?.id],
      ([visible]) => {
        if (visible) loadMembers()
      }
    )

    return {
      members,
      loading,
      mutating,
      loadError,
      newMember,
      roleOptions: ROLE_OPTIONS,
      loadMembers,
      addMember,
      changeMember,
      removeMember,
      isCurrentUser,
      isProtected,
      roleLabel,
      close
    }
  }
}
</script>

<style lang="less" scoped>
.member-add {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px 96px;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}
.member-state {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--te-template-center-common-item-desc-text-color);
}
.error-state {
  color: var(--te-base-color-error-6);
}
.member-list {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--te-template-app-center-item-border-color);
}
.member-row {
  display: grid;
  grid-template-columns: minmax(150px, 1.2fr) 150px 130px 150px;
  gap: 12px;
  min-height: 52px;
  padding: 8px 12px;
  align-items: center;
  border-bottom: 1px solid var(--te-template-app-center-item-border-color);
}
.member-row:last-child {
  border-bottom: 0;
}
.member-header {
  min-height: 40px;
  font-weight: 600;
  background: var(--te-template-common-bg-color-hover);
}
.member-user {
  display: flex;
  min-width: 0;
  flex-direction: column;
}
.member-user strong,
.member-user small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.member-user small,
.protected-label {
  color: var(--te-template-center-common-item-desc-text-color);
}
.member-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
