{
  "properties" : { },
  "id" : "eff8690f493642d8a6614a4ea73196b8",
  "script" : null,
  "groupId" : "b361a7616a8f4f65b2743b4cdc5223cb",
  "name" : "未定义名称test",
  "createTime" : 1762332659286,
  "updateTime" : 1779625087610,
  "lock" : null,
  "createBy" : null,
  "updateBy" : null,
  "path" : "test",
  "jsDependences" : null,
  "cssDependences" : null,
  "compileCss" : "",
  "compileJs" : "export default (async function() {\n/* Analyzed bindings: {\n  \"remoteComponent\": \"setup-maybe-ref\"\n} */\n/* Component metadata: {\n  \"id\": \"5221daaf-ca9c-4f53-bae3-98098c895d0e\",\n  \"hasScript\": true,\n  \"hasScriptSetup\": false,\n  \"hasTemplate\": true,\n  \"hasStyles\": false,\n  \"hasScopedStyles\": false,\n  \"hasModuleStyles\": false,\n  \"styleLangs\": []\n} */\n\nconst ref = await import('vue').then(m => m.ref);\nconst onMounted = await import('vue').then(m => m.onMounted);\nconst defineAsyncComponent = await import('vue').then(m => m.defineAsyncComponent);\nconst __sfc__ = {\n  name: 'RemoteDemo',\n  setup() {\n    const remoteComponent = ref(null);\n    onMounted(async () => {\n      try {\n        const module = await import(\n          /* @vite-ignore */ \n          '/magic-api/vue3/component/export.js?id=74d72ddb020c4c58817f0021d9efa116'\n        );\n        remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));\n      } catch (error) {\n        console.error('加载远程组件失败:', error);\n      }\n    });\n    return {\n      remoteComponent\n    };\n  }\n};\n\nconst _createElementVNode = await import('vue').then(m => m.createElementVNode);\nconst _resolveDynamicComponent = await import('vue').then(m => m.resolveDynamicComponent);\nconst _openBlock = await import('vue').then(m => m.openBlock);\nconst _createBlock = await import('vue').then(m => m.createBlock);\nconst _createCommentVNode = await import('vue').then(m => m.createCommentVNode);\nconst _createElementBlock = await import('vue').then(m => m.createElementBlock);\n\nconst _hoisted_1 = { key: 1 }\nfunction render(_ctx, _cache, $props, $setup, $data, $options) {\n  return (_openBlock(), _createElementBlock(\"div\", null, [\n    _cache[0] || (_cache[0] = _createElementVNode(\"h1\", null, \"远程组件演示\", -1 /* CACHED */)),\n    ($setup.remoteComponent)\n      ? (_openBlock(), _createBlock(_resolveDynamicComponent($setup.remoteComponent), { key: 0 }))\n      : (_openBlock(), _createElementBlock(\"div\", _hoisted_1, \"等待加载组件...\"))\n  ]))\n}\n__sfc__.render = render\n__sfc__.__file = \"5221daaf-ca9c-4f53-bae3-98098c895d0e.vue\"\nreturn __sfc__\n})()",
  "description" : "test",
  "params" : null
}
================================
<template>
  <div>
    <h1>远程组件演示</h1>
    <component :is="remoteComponent" v-if="remoteComponent" />
    <div v-else>等待加载组件...</div>
  </div>
</template>
<script>
import { ref, onMounted, defineAsyncComponent } from 'vue';
export default {
  name: 'RemoteDemo',
  setup() {
    const remoteComponent = ref(null);
    onMounted(async () => {
      try {
        const module = await import(
          /* @vite-ignore */ 
          '/magic-api/vue3/component/export.js?id=74d72ddb020c4c58817f0021d9efa116'
        );
        remoteComponent.value = defineAsyncComponent(() => Promise.resolve(module.default));
      } catch (error) {
        console.error('加载远程组件失败:', error);
      }
    });
    return {
      remoteComponent
    };
  }
};
</script>