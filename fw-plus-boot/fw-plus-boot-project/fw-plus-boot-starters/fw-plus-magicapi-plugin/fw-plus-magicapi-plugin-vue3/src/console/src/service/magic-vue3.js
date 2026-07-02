import { compileCode } from '../scripts/compiler/sfc-compiler'
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
        // 执行测试的逻辑
        doTest: async (opened) => {
            opened.running = true ;
            const info = opened.item ;
            let compiledResult = {};
            try {
                // 编译 Vue 文件
                compiledResult = compileCode(info.script || opened.defaultScript);// 获取组件模块
                console.log('compileJs:', compiledResult.compileJs) ;
                console.log('compiledCss:', compiledResult.compiledCss) ;
            } catch (error) {
                console.error('Vue3编译错误:', error) ;
                alert(`Vue3编译错误:${error.message}`);
                bus.status(`Vue3编译错误`) ;
                opened.running = false ;
                return
            }
            const requestConfig = {
                baseURL: constants.SERVER_URL,
                url: '/vue3/execute',
                method: 'POST',
                responseType: 'text', // 改为text，因为返回的是HTML
                headers: {},
                withCredentials: true
            } ;
            bus.$emit(Message.SWITCH_TOOLBAR, 'log');
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID;
            requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id;
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE;
            // 设置断点
            requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join(',')
            const fullName = opened.path() ;
            bus.status(`开始测试vue3「${fullName}」`) ;
            // 发送编译后的代码而不是原始代码
            request.sendPost('/vue3/execute', {
                id: info.id,
                sourceCode: info.script,
                compiledJs: compiledResult.compileJs,
                compiledCss: compiledResult.compiledCss
            }, requestConfig).success(res => {
                opened.running = false
                // 打开新窗口显示返回的HTML内容
                const newWindow = window.open('', '_blank');
                newWindow.document.write(res);
                newWindow.document.close();
                // 设置窗口标题
                newWindow.document.title = `Vue3结果 - ${fullName}`;
            }).end(() => {
                bus.status(`vue3「${fullName}」测试完毕`) ;
                opened.running = false ;
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
