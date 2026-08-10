<template>
  <div class="magic-mybatis-xml-info">
    <form>
      <label>{{ $i('mybatisFlexXml.form.enabled') }}</label>
      <magic-checkbox v-model:value="info.enabled" />
      <label>{{ $i('mybatisFlexXml.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mybatisFlexXml.form.placeholder.name')" width="180px"/>
      <label>{{ $i('mybatisFlexXml.form.path') }}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('mybatisFlexXml.form.placeholder.path')" width="auto" style="flex:1"/>
    </form>
    <form>
      <label>{{ $i('mybatisFlexXml.form.dbKey') }}</label>
      <magic-input v-model:value="info.dbKey" :disabled="!!info.id" :placeholder="$i('mybatisFlexXml.form.placeholder.dbKey')" width="220px"/>
      <label>{{ $i('mybatisFlexXml.form.tableName') }}</label>
      <magic-input v-model:value="info.tableName" :placeholder="$i('mybatisFlexXml.form.placeholder.tableName')" width="260px"/>
      <span class="tip">{{ $i('mybatisFlexXml.form.uniqueTip') }}</span>
    </form>
    <div class="default-db-key-tip">{{ $i('mybatisFlexXml.form.defaultDbKeyTip') }}</div>
    <div class="magic-mybatis-xml-description">
      <label>{{ $i('mybatisFlexXml.form.description') }}</label>
      <magic-textarea v-model:value="info.description" :placeholder="$i('mybatisFlexXml.form.placeholder.description')"/>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>

<style scoped>
.magic-mybatis-xml-info{
  display: flex;
  flex-direction: column;
  height: 100%;
}
.magic-mybatis-xml-info form{
  display: flex;
  padding: 5px;
  align-items: center;
}
.magic-mybatis-xml-info label{
  display: inline-block;
  width: 80px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  text-align: right;
  padding: 0 5px;
}
.magic-mybatis-xml-info .tip{
  color: var(--magic-text-color-secondary);
  padding-left: 8px;
  white-space: nowrap;
}
.magic-mybatis-xml-info .default-db-key-tip{
  margin: 0 5px 5px 95px;
  padding: 6px 10px;
  color: #ad6800;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
}
.magic-mybatis-xml-description{
  display: flex;
  flex: 1;
  min-height: 100px;
  padding: 0 5px 5px;
}
.magic-mybatis-xml-description :deep(.magic-textarea){
  flex: 1;
}
</style>
