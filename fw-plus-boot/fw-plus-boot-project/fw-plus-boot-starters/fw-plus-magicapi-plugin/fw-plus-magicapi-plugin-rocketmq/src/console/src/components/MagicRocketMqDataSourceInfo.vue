<template>
  <div class="magic-form-row">
    <label>{{ $i('rocketmq.datasource.form.name') }}</label>
    <magic-input
        v-model="info.name"
        :placeholder="$i('rocketmq.datasource.form.placeholder.name')"
        width="100%"
    />
  </div>
  <!-- 其他表单字段类似 -->
</template>

<script>
import {inject, ref} from 'vue';

export default {
  props: {
    info: {
      type: Object,
      default: () => ({ other: '{}' })
    }
  },
  setup(props) {
    const $i = inject('i18n.format');
    const info = ref(props.info);
    const options = [
      { text: $i('datasource.extra.producer'), value: 'producer' },
      { text: $i('datasource.extra.consumer'), value: 'consumer' }
    ];
    return { $i, info, options };
  }
};
</script>
