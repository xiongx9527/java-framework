<template>
  <div class="magic-rocketmq-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model="info.enabled" />
      <!-- 其他表单字段类似 -->
    </form>
  </div>
</template>

<script>
import { ref } from 'vue';

export default {
  setup() {
    const info = ref({});
    return { info };
  }
};
</script>
