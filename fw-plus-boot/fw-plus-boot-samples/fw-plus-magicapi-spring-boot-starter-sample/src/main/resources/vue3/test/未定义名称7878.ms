{
  "properties" : { },
  "id" : "621038bb7b394f00956ffcbd20092416",
  "script" : null,
  "groupId" : "b361a7616a8f4f65b2743b4cdc5223cb",
  "name" : "未定义名称7878",
  "createTime" : 1761809846529,
  "updateTime" : 1764509210769,
  "lock" : null,
  "createBy" : null,
  "updateBy" : null,
  "path" : "345345",
  "jsDependences" : null,
  "cssDependences" : null,
  "compileCss" : "",
  "compileJs" : "export default (async function() {\nconst ref = await import('vue').then(m => m.ref);\nconst onMounted = await import('vue').then(m => m.onMounted);\nconst __sfc__ = {\n  props: {\n    title: {\n      type: String,\n      default: '默认标题33333'\n    },\n    count: {\n      type: Number,\n      default: 0\n    }\n  },\n  setup(props, context) {\n    // 需要手动返回模板中使用的数据和方法\n    const localData = ref('局部数据')\n    onMounted(() => {\n      console.log(props.title)\n    })\n    // 必须返回才能在模板中使用\n    return {\n      localData\n    }\n  }\n}\n\nconst _toDisplayString = await import('vue').then(m => m.toDisplayString);\nconst _openBlock = await import('vue').then(m => m.openBlock);\nconst _createElementBlock = await import('vue').then(m => m.createElementBlock);\n\nconst _hoisted_1 = { class: \"card\" }\nfunction render(_ctx, _cache, $props, $setup, $data, $options) {\n  return (_openBlock(), _createElementBlock(\"div\", _hoisted_1, _toDisplayString($props.title) + \" - \" + _toDisplayString($props.count), 1 /* TEXT */))\n}\n__sfc__.render = render\n__sfc__.__file = \"ae22d0b2-2e59-4bb8-b293-e09ff8ca6b3e.vue\"\nreturn __sfc__\n})()",
  "description" : "345345989890898989",
  "params" : "{\n  \"title\":\"0000000000000099999999\"\n}",
  "history" : "[{\"createDate\":1761809850812,\"createTime\":1761809850812,\"id\":\"8b632da6-955d-463d-a9ab-3f7bbb26a8d6\",\"sourceCode\":\"<template>\\n\\n</template>\\n<script setup>\\n\\n</script>\\n<style scoped>\\n\\n</style>\",\"updateBy\":\"\",\"updateTime\":1761809872993},{\"createDate\":1761809873019,\"createTime\":1761809873019,\"id\":\"800415e2-42bb-4428-a4cf-a7a6c1ddaa88\",\"sourceCode\":\"<template>\\n\\n</template>\\n<script setup>\\n\\n</script>\\n<style scoped>\\n78798797987\\n</style>\",\"updateBy\":\"\",\"updateTime\":1761809883889},{\"createDate\":1761809883891,\"createTime\":1761809883891,\"id\":\"f33f79e3-1008-4291-9c29-97b7208839d6\",\"sourceCode\":\"<template>\\n\\n</template>\\n<script setup>\\n\\n</script>\\n<style scoped>\\n7879879798787979798798\\n</style>\",\"updateBy\":\"\",\"updateTime\":1761809894795},{\"createDate\":1762091991515,\"createTime\":1762091991515,\"id\":\"dd2fcc99-09a8-4223-a3b3-3defe1d9f732\",\"sourceCode\":\"<template>\\n89089089089089\\n</template>\\n<script setup>\\n\\n</script>\\n<style scoped>\\n</style>\",\"updateBy\":\"\",\"updateTime\":1762327887506},{\"createDate\":1762441668444,\"createTime\":1762441668444,\"id\":\"dcf2a2da-e534-4ed4-ab62-742b062c48cb\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1762441668459},{\"createDate\":1762488719725,\"createTime\":1762488719725,\"id\":\"acfc7d40-edba-48bc-9820-98f840b7c378\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1762488719748},{\"createDate\":1762488719749,\"createTime\":1762488719749,\"id\":\"1006fdbc-6a6f-460f-a1e4-c4719fcf9477\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1762559251068},{\"createDate\":1762559251070,\"createTime\":1762559251070,\"id\":\"936d9d2f-f404-4381-9052-de843374aff4\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1762559340956},{\"createDate\":1762559340957,\"createTime\":1762559340957,\"id\":\"256886a9-a4ad-4620-97f9-c6b372ff73ed\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1762568855498},{\"createDate\":1764509209745,\"createTime\":1764509209745,\"id\":\"01bd790c-0378-47ea-8865-a8f0e8774cfa\",\"sourceCode\":\"<template>\\n  <div class=\\\"card\\\">\\n    {{ title }} - {{ count }}\\n  </div>\\n</template>\\n\\n<script>\\nimport { ref, onMounted } from 'vue'\\nexport default {\\n  props: {\\n    title: {\\n      type: String,\\n      default: '默认标题33333'\\n    },\\n    count: {\\n      type: Number,\\n      default: 0\\n    }\\n  },\\n  setup(props, context) {\\n    // 需要手动返回模板中使用的数据和方法\\n    const localData = ref('局部数据')\\n    onMounted(() => {\\n      console.log(props.title)\\n    })\\n    // 必须返回才能在模板中使用\\n    return {\\n      localData\\n    }\\n  }\\n}\\n</script>\",\"updateBy\":\"\",\"updateTime\":1764509210769}]"
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