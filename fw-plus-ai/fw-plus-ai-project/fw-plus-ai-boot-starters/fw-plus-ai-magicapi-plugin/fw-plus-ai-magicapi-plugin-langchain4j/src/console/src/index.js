import { defineComponent, h, inject, resolveComponent, unref } from 'vue'
import { installLangchain4jIcon } from './assets/icon.js'

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

const modelTypes = ['EMBEDDING', 'CHAT', 'STREAMING_CHAT', 'LANGUAGE', 'STREAMING_LANGUAGE', 'IMAGE']
const providers = [
    'OPENAI',
    'QWEN',
    'HUGGING_FACE',
    'OLLAMA',
    'ZHIPU',
    'QIANFAN',
    'LOCAL_AI',
    'GEMINI',
    'AZURE_OPENAI',
    'ANTHROPIC',
    'ALL_MINI_LM_L6_V2_QUANTIZED'
]

const formRow = (label, control) => h('div', { class: 'magic-form-row' }, [h('label', label), control])

const MagicLangchain4jDataSourceInfo = defineComponent({
    name: 'magic-langchain4j-datasource-info',
    __name: 'magic-langchain4j-datasource-info',
    props: {
        info: {
            type: Object,
            default: {
                other: '{}'
            }
        }
    },
    setup(props) {
        const $i = inject('i18n.format')
        const info = props.info
        const translate = key => unref($i)(key)

        return () => {
            const MagicInput = resolveComponent('magic-input')
            const MagicSelect = resolveComponent('magic-select')
            const MagicMonacoEditor = resolveComponent('magic-monaco-editor')
            const modelTypeOptions = modelTypes.map(value => ({
                text: translate(`langchain4j.datasource.extra.modelType.${value}`),
                value
            }))
            const providerOptions = providers.map(value => ({
                text: translate(`langchain4j.datasource.extra.provider.${value}`),
                value
            }))

            return [
                formRow(translate('langchain4j.datasource.form.name'), h(MagicInput, {
                    value: info.name,
                    'onUpdate:value': value => { info.name = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.name'),
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.key'), h(MagicInput, {
                    value: info.key,
                    'onUpdate:value': value => { info.key = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.key'),
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.baseUrl'), h(MagicInput, {
                    value: info.baseUrl,
                    'onUpdate:value': value => { info.baseUrl = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.baseUrl'),
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.apiKey'), h(MagicInput, {
                    value: info.apiKey,
                    'onUpdate:value': value => { info.apiKey = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.apiKey'),
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.provider'), h(MagicSelect, {
                    value: info.provider,
                    'onUpdate:value': value => { info.provider = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.provider'),
                    options: providerOptions,
                    style: { width: '20px' }
                })),
                formRow(translate('langchain4j.datasource.form.modelType'), h(MagicSelect, {
                    value: info.modelType,
                    'onUpdate:value': value => { info.modelType = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.modelType'),
                    options: modelTypeOptions,
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.modelName'), h(MagicInput, {
                    value: info.modelName,
                    'onUpdate:value': value => { info.modelName = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.modelName'),
                    width: '100%'
                })),
                formRow(translate('langchain4j.datasource.form.other'), h(MagicMonacoEditor, {
                    value: info.other,
                    'onUpdate:value': value => { info.other = value },
                    placeholder: translate('langchain4j.datasource.form.placeholder.other'),
                    language: 'json',
                    style: { height: '150px' }
                }))
            ]
        }
    }
})

const createLangchain4jDataSourceService = (request, $i, modal, JavaClass) => {
    let findResources
    JavaClass.setExtensionAttribute(LANGCHAIN4J_MODEL_CLASS, () => {
        JavaClass.loadClass(LANGCHAIN4J_MODEL_CLASS)
        return findResources && (findResources('qdrant-datasource')[0]?.children || []).map(item => ({
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

const installStyle = targetDocument => {
    const style = targetDocument.createElement('style')
    style.innerHTML = `
.magic-langchain4j-info { display: flex; flex-direction: column; flex: 1; padding: 5px; }
.magic-langchain4j-info form { display: flex; }
.magic-langchain4j-info form label { display: inline-block; width: 100px; height: var(--magic-input-height); line-height: var(--magic-input-height); font-weight: 400; text-align: right; padding: 0 5px; }
.magic-langchain4j-info form .magic-checkbox { width: 22px; height: 22px; }
.magic-langchain4j-info form .magic-textarea { margin: 5px; }
.magic-langchain4j-info form .magic-form-row.align-left label { text-align: left; width: 100%; }
.magic-langchain4j-info form .magic-form-row.align-left .magic-checkbox { margin-left: 0 !important; }
`
    targetDocument.head.appendChild(style)
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
    installStyle(document)
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
