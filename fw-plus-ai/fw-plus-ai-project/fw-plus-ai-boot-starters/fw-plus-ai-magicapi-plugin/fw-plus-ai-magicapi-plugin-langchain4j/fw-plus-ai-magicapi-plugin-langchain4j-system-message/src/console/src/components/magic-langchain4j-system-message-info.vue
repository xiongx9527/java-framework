<template>
  <div class="magic-langchain4j-system-message-info">
    <form>
      <label>{{ $i('langchain4j.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('langchain4j.hint.name')" width="200px"/>
      <label>{{ $i('langchain4j.form.path') }}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('langchain4j.hint.path')" width="auto" style="flex:1"/>
    </form>
  </div>
  <magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false" tooltip-direction="bottom">
    <magic-navbar-item v-for="(navbar, key) in navbars" :key="key" v-bind="navbar">
      <component :is="navbar.component"/>
    </magic-navbar-item>
  </magic-navbar>
</template>
<script setup>
import {inject} from 'vue'
import MagicLangchain4jSystemMessageParams from './magic-langchain4j-system-message-params.vue'
import MagicLangchain4jSystemMessageDescription from './magic-langchain4j-system-message-description.vue'

const $i = inject('i18n.format')
const info = inject('info')
const navbars = [{
  title: $i('langchain4j.form.params'),
  component: MagicLangchain4jSystemMessageParams
}, {
  title: $i('langchain4j.form.description'),
  component: MagicLangchain4jSystemMessageDescription
}]
</script>
<style scoped>
.magic-langchain4j-system-message-info {
  display: flex;
  flex-direction: column;
}
.magic-langchain4j-system-message-info form {
  display: flex;
  padding: 5px;
}
.magic-langchain4j-system-message-info form label {
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-langchain4j-system-message-info + .magic-navbar {
  flex-direction: column;
  overflow: hidden;
}
.magic-langchain4j-system-message-info + .magic-navbar :deep(.magic-navbar-header) {
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item) {
  width: 100%;
  height: 100%;
}
</style>
