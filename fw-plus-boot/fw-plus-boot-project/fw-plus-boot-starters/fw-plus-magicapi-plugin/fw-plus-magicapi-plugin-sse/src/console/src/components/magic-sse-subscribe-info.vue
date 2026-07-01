<template>
  <div class="magic-sse-info">
    <form>
      <label style="width:70px">{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label style="width:70px">{{ $i('sse.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('sse.subscribe.form.placeholder.key')" width="150px" />

      <label style="width:70px">{{ $i('sse.subscribe.form.businessId') }}</label>
      <magic-input v-model:value="info.businessId" :placeholder="$i('sse.subscribe.form.placeholder.businessId')" width="150px" />

      <label style="width:70px">{{ $i('sse.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('sse.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
    </form>
    <div style="flex:1;padding-top:5px">
      <magic-textarea v-model:value="info.description" :placeholder="$i('sse.subscribe.form.placeholder.description')" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>

<style scoped>
.magic-sse-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 5px;
}
.magic-sse-info form {
  display: flex;
}
.magic-sse-info form label {
  display: inline-block;
  width: 100px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-sse-info form .magic-checkbox {
  width: 22px;
  height: 22px;
}
.magic-sse-info form .magic-textarea {
  margin: 5px;
}
</style>