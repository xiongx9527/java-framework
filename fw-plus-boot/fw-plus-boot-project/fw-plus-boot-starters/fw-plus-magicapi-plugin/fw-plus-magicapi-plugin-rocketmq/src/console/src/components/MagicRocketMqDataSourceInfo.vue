<template>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.name') }}</label>
    <magic-input
        v-model:value="info.name"
        :placeholder="$i('rocketmq.datasource.form.placeholder.name')"
        width="100%"
    />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.key') }}</label>
    <magic-input v-model:value="info.key" :placeholder="$i('rocketmq.datasource.form.placeholder.key')" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.nameServer') }}</label>
    <magic-input v-model:value="info.nameServer" :placeholder="$i('rocketmq.datasource.form.placeholder.nameServer')" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.accessKey') }}</label>
    <magic-input v-model:value="info.accessKey" :placeholder="$i('rocketmq.datasource.form.placeholder.accessKey')" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.secretKey') }}</label>
    <magic-input v-model:value="info.secretKey" type="password" :placeholder="$i('rocketmq.datasource.form.placeholder.secretKey')" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.extra') }}</label>
    <magic-select v-model:value="info.extra" :placeholder="$i('rocketmq.datasource.form.placeholder.extra')" :options="options" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.group') }}</label>
    <magic-input v-model:value="info.group" :placeholder="$i('rocketmq.datasource.form.placeholder.group')" width="100%" />
  </div>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.properties') }}</label>
    <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('rocketmq.datasource.form.placeholder.properties')" language="json" style="height:150px" />
  </div>
</template>

<script>
import {inject} from 'vue';

export default {
  props: {
    info: {
      type: Object,
      default: () => ({ properties: {} })
    }
  },
  setup(props) {
    const $i = inject('i18n.format');
    const info = props.info;
    const options = [
      { text: $i('datasource.extra.producer'), value: 'producer' },
      { text: $i('datasource.extra.consumer'), value: 'consumer' }
    ];
    return { $i, info, options };
  }
};
</script>
