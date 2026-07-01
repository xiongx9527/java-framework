<template>
  <div class="magic-word-info">
    <form>
      <label>{{ $i('word.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('word.form.placeholder.name')" width="200px"/>
      <label>{{ $i('word.form.path') }}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('word.form.placeholder.path')" width="auto" style="flex:1"/>
    </form>
  </div>
  <magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false" tooltip-direction="bottom">
    <magic-navbar-item v-for="item in navbars" :key="item.id" v-bind="item">
      <component :is="item.component" />
    </magic-navbar-item>
  </magic-navbar>
</template>

<script setup>
import { inject } from 'vue'
import MagicWordDataStructure from './magic-word-data-structure.vue'
import MagicWordDescription from './magic-word-description.vue'

const $i = inject('i18n.format')
const info = inject('info')

const navbars = [
  { id: 'description', title: $i('word.form.description'), component: MagicWordDescription },
  { id: 'structure', title: $i('word.structure'), component: MagicWordDataStructure }
]
</script>

<style scoped>
.magic-word-info {
  display: flex;
  flex-direction: column;
}
.magic-word-info form {
  display: flex;
  padding: 5px;
}
.magic-word-info form label {
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-word-info + .magic-navbar {
  flex-direction: column;
  overflow: hidden;
}
.magic-word-info + .magic-navbar :deep(.magic-navbar-header) {
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item) {
  width: 100%;
  height: 100%;
}
</style>

