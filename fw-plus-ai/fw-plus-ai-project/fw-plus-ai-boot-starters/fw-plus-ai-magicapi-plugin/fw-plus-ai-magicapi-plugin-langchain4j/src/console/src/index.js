import { installLangchain4jIcon } from './assets/icon.js'
import MagicLangchain4jDataSourceInfo from './components/magic-langchain4j-datasource-info.vue'

const LANGCHAIN4J_MODEL_CLASS = 'com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo'

const localZhCN = {
    langchain4j: {
        datasource: {
            title: 'Langchain4j \u6570\u636e\u6e90',
            name: 'Langchain4j \u6570\u636e\u6e90',
            extra: {
                modelType: {
                    EMBEDDING: 'EMBEDDING',
                    CHAT: 'CHAT',
                    STREAMING_CHAT: 'STREAMING_CHAT',
                    LANGUAGE: 'LANGUAGE',
                    STREAMING_LANGUAGE: 'STREAMING_LANGUAGE',
                    IMAGE: 'IMAGE'
                },
                provider: {
                    OPENAI: 'OPENAI',
                    QWEN: 'QWEN',
                    HUGGING_FACE: 'HUGGING_FACE',
                    OLLAMA: 'OLLAMA',
                    ZHIPU: 'ZHIPU',
                    QIANFAN: 'QIANFAN',
                    LOCAL_AI: 'LOCAL_AI',
                    GEMINI: 'GEMINI',
                    AZURE_OPENAI: 'AZURE_OPENAI',
                    ANTHROPIC: 'ANTHROPIC',
                    ALL_MINI_LM_L6_V2_QUANTIZED: 'ALL_MINI_LM_L6_V2_QUANTIZED'
                }
            },
            form: {
                name: '\u540d\u79f0',
                key: 'Key',
                baseUrl: 'BaseUrl',
                modelName: '\u6a21\u578b\u540d\u79f0',
                provider: '\u670d\u52a1\u5546',
                modelType: '\u6a21\u578b\u7c7b\u578b',
                apiKey: 'API Key',
                other: '\u5176\u4ed6\u914d\u7f6e',
                placeholder: {
                    name: '\u540d\u79f0\uff0c\u4ec5\u663e\u793a\u4f7f\u7528',
                    key: '\u6570\u636e\u6e90Key\uff0c\u5fc5\u586b',
                    baseUrl: '\u8bf7\u8f93\u5165BaseUrl\uff0c\u5fc5\u586b',
                    modelName: '\u8bf7\u8f93\u5165\u6a21\u578b\u540d\u79f0\uff0c\u5fc5\u586b',
                    modelType: '\u8bf7\u8f93\u5165\u6a21\u578b\u7c7b\u578b\uff0c\u5fc5\u586b',
                    provider: '\u8bf7\u8f93\u5165\u670d\u52a1\u5546\uff0c\u5fc5\u586b',
                    apiKey: '\u8bf7\u8f93\u5165API Key\uff0c\u5fc5\u586b',
                    other: '\u8bf7\u8f93\u5165 Langchain4j \u8bbf\u95ee\u76f8\u5173\u914d\u7f6e\uff0c\u5982\uff1a {xxxx: "yyyy"}'
                }
            }
        }
    }
}

const localEn = {
    langchain4j: {
        datasource: {
            title: 'Langchain4j DataSource',
            name: 'Langchain4j',
            extra: localZhCN.langchain4j.datasource.extra,
            form: {
                name: 'Name',
                key: 'Key',
                baseUrl: 'BaseUrl',
                modelName: 'Model Name',
                apiKey: 'API Key',
                provider: 'Provider',
                modelType: 'Model Type',
                other: 'Other Config',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    baseUrl: 'Please Enter Server BaseUrl, Required',
                    modelName: 'Please Enter Server Model Name, Required',
                    modelType: 'Please Enter Server Model Type, Required',
                    provider: 'Please Enter Server Provider, Required',
                    apiKey: 'Please Enter API Key, Required',
                    other: 'Please Enter Langchain4j Other Config\uff0ceg\uff1a {xxxx: "yyyy"}'
                }
            }
        }
    }
}

const createLangchain4jDataSourceService = (request, $i, modal, JavaClass) => {
    let findResources
    JavaClass.setExtensionAttribute(LANGCHAIN4J_MODEL_CLASS, () => {
        JavaClass.loadClass(LANGCHAIN4J_MODEL_CLASS)
		return findResources && (findResources('vector-datasource')[0]?.children || []).map(item => ({
            name: item.key,
            type: LANGCHAIN4J_MODEL_CLASS,
            comment: item.name
        })) || []
    })
    return {
        injectResources: fn => { findResources = fn },
        dialogWidth: '1100px',
        doTest: info => {
            request.sendJson('/langchain4j/datasource/test', info).success(res => {
                if (res === 'ok') {
                    modal.alert($i('datasource.connected'), $i('datasource.test'))
                } else {
                    modal.alert($i('datasource.connectFailed', res), $i('datasource.test'))
                }
            })
        }
    }
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
    installLangchain4jIcon(document)
}

export default opt => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        datasources: [{
            type: 'langchain4j-datasource',
            icon: '#magic-langchain4j-langchain4j',
            title: i18n.format('langchain4j.datasource.title'),
            name: i18n.format('langchain4j.datasource.name'),
            service: createLangchain4jDataSourceService(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicLangchain4jDataSourceInfo
        }],
        statusBarItems: [{
            icon: '#magic-langchain4j-langchain4j',
            title: 'Langchain4j',
            displayKey: 'langchain4j',
            order: 800,
            onClick: () => window.open('https://docs.langchain4j.dev/', '_blank')
        }]
    }
}
