import {compileCode} from '../scripts/compiler/sfc-compiler'

export default function (bus, constants, $i, Message, request, opt) {
    return {
        // svg text
        getIcon: item => ['vue3', '#42b883'],
        // 任务名称
        name: $i('vue.name'),
        // 脚本语言
        language: 'vue',
        defaultScript: `<template>

</template>
<script setup>

</script>
<style scoped>

</style>`,
        doSave: async (opened) => {
            const info = opened.item ;
            console.log('Vue3保存文件：', info.script)
            // 编译 Vue 文件 (async)
            let compiledResult = await compileCode(info.script || opened.defaultScript);
            console.log('Vue3编译结果：', compiledResult)
            // 输出编译 warnings
            if (compiledResult.warnings && compiledResult.warnings.length) {
                compiledResult.warnings.forEach(w => console.warn('[Vue SFC Warning]', w.message || w));
            }
            // 输出编译 errors（降级编译时）
            if (compiledResult.errors && compiledResult.errors.length) {
                compiledResult.errors.forEach(e => console.error('[Vue SFC Error]', e.message || e));
            }
            opened.item.compileJs = compiledResult.compileJs;
            opened.item.compileCss = compiledResult.compileCss;
            // 存储自定义块和样式元数据
            if (compiledResult.customBlocks) {
                opened.item.customBlocks = JSON.stringify(compiledResult.customBlocks);
            }
            if (compiledResult.styleMeta) {
                opened.item.styleMeta = JSON.stringify(compiledResult.styleMeta);
            }
        },
        // 执行测试的逻辑
        doTest: async (opened) => {
            opened.running = true ;
            const info = opened.item ;
            const requestConfig = {
                url: '/vue3/execute',
                method: 'POST',
                responseType: 'text',
                headers: {},
                withCredentials: true,
                data: { id: info.id }
            } ;
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID;
            requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id;
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE;
            // 设置断点
            requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join(',')
            const fullName = opened.path() ;
            const startTime = new Date().getTime() ;
            bus.status(`开始测试vue3「${fullName}」`) ;
            bus.$emit(Message.SWITCH_TOOLBAR, 'log');
            request.execute(requestConfig).then(response => {
                opened.running = false
                info.responseBody = response.data ;
                info.responseStatus = response.status ;
                info.responseHeaders = response.headers ;
                bus.status(`vue3「${fullName}」测试成功`, true, new Date().getTime() - startTime + 'ms') ;
                bus.$emit(Message.SWITCH_TOOLBAR, 'vue3-result') ;
            }).catch(error => {
                opened.running = false
                bus.status(`vue3「${fullName}」测试失败`, false) ;
                request.processError(error)
            })
        },
        // 是否允许执行测试
        runnable: true,
        // 是否需要填写路径
        requirePath: true,
        // 合并
        merge: item => item
    }
}
