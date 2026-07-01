<template>
	<magic-editor :config="defaultConfig"/>
</template>
<script setup>
// 工具函数
const getQueryString = (name) => {
    const params = new URLSearchParams(window.location.search)
    return params.get(name)
}

// 获取配置源
const getConfigFromSources = () => {
    const sources = []
    
    // 从父窗口获取配置
    try {
        if (parent?.MAGIC_EDITOR_CONFIG) {
            sources.push({ ...parent.MAGIC_EDITOR_CONFIG })
        }
    } catch (ignored) {}
    
    // 从当前窗口获取配置
    if (window.MAGIC_EDITOR_CONFIG) {
        sources.push({ ...window.MAGIC_EDITOR_CONFIG })
    }
    
    return sources
}

// 合并配置
const mergeConfigs = (configs) => {
    return configs.reduce((merged, config) => ({ ...merged, ...config }), {})
}



// 设置请求头配置
const setRequestHeaders = (config) => {
    const headerName = getQueryString('headerName')
    const headerValue = getQueryString('headerValue')
    
    if (headerName && headerValue) {
        config.request = config.request || {}
        const originalBeforeSend = config.request.beforeSend
        
        config.request.beforeSend = (cfg) => {
            cfg.headers[headerName] = headerValue
            // 保留原有的 beforeSend 逻辑
            return originalBeforeSend ? originalBeforeSend(cfg) : cfg
        }
    }
    
    return config
}

// 初始化配置
const defaultConfig = setRequestHeaders({
    ...mergeConfigs(getConfigFromSources()),
    baseURL: import.meta.env.MAGIC_API_BASE_URL,
    serverURL: import.meta.env.MAGIC_API_SERVER_URL,
    inJar: import.meta.env.MAGIC_API_IN_JAR
})
</script>
<style>
html,body,#app{
  width: 100%;
  height: 100%;
  margin:0 !important;
}
</style>