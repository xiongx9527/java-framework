{
  "properties" : { },
  "id" : "621038bb7b394f00956ffcbd20092416",
  "script" : null,
  "groupId" : "b361a7616a8f4f65b2743b4cdc5223cb",
  "name" : "未定义名称7878",
  "createTime" : 1761809846529,
  "updateTime" : 1779625079542,
  "lock" : null,
  "createBy" : null,
  "updateBy" : null,
  "path" : "345345",
  "jsDependences" : null,
  "cssDependences" : null,
  "compileCss" : "",
  "compileJs" : "export default (async function() {\n/* Analyzed bindings: {\n  \"title\": \"props\",\n  \"count\": \"props\",\n  \"localData\": \"setup-maybe-ref\"\n} */\n/* Component metadata: {\n  \"id\": \"3cfd3211-e08e-4c3e-9c0a-1dfff1ca8684\",\n  \"hasScript\": true,\n  \"hasScriptSetup\": false,\n  \"hasTemplate\": true,\n  \"hasStyles\": false,\n  \"hasScopedStyles\": false,\n  \"hasModuleStyles\": false,\n  \"styleLangs\": []\n} */\n\nconst ref = await import('vue').then(m => m.ref);\nconst onMounted = await import('vue').then(m => m.onMounted);\nconst __sfc__ = {\n  props: {\n    title: {\n      type: String,\n      default: '默认标题33333'\n    },\n    count: {\n      type: Number,\n      default: 0\n    }\n  },\n  setup(props, context) {\n    // 需要手动返回模板中使用的数据和方法\n    const localData = ref('局部数据')\n    onMounted(() => {\n      console.log(props.title)\n    })\n    // 必须返回才能在模板中使用\n    return {\n      localData\n    }\n  }\n}\n\nconst _toDisplayString = await import('vue').then(m => m.toDisplayString);\nconst _openBlock = await import('vue').then(m => m.openBlock);\nconst _createElementBlock = await import('vue').then(m => m.createElementBlock);\n\nconst _hoisted_1 = { class: \"card\" }\nfunction render(_ctx, _cache, $props, $setup, $data, $options) {\n  return (_openBlock(), _createElementBlock(\"div\", _hoisted_1, _toDisplayString($props.title) + \" - \" + _toDisplayString($props.count), 1 /* TEXT */))\n}\n__sfc__.render = render\n__sfc__.__file = \"3cfd3211-e08e-4c3e-9c0a-1dfff1ca8684.vue\"\nreturn __sfc__\n})()",
  "description" : "345345989890898989",
  "params" : "{\n  \"title\":\"0000000000000099999999\"\n}"
}
================================
<template>
  <div class="card">
    {{ title }} - {{ count }}
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
export default {
  props: {
    title: {
      type: String,
      default: '默认标题33333'
    },
    count: {
      type: Number,
      default: 0
    }
  },
  setup(props, context) {
    // 需要手动返回模板中使用的数据和方法
    const localData = ref('局部数据')
    onMounted(() => {
      console.log(props.title)
    })
    // 必须返回才能在模板中使用
    return {
      localData
    }
  }
}
</script>