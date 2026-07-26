import { createHash } from 'node:crypto'
import vm from 'node:vm'

const SAMPLE_INFO = {
    name: 'Sample name',
    key: 'sample-key',
    baseUrl: 'https://example.test',
    apiKey: 'sample-api-key',
    provider: 'OPENAI',
    modelType: 'CHAT',
    modelName: 'sample-model',
    other: '{"temperature":0.7}'
}

const clone = value => JSON.parse(JSON.stringify(value))
const sha256 = value => createHash('sha256').update(value).digest('hex')

const normalizeCss = css => {
    const rules = []
    const normalized = css.replace(/\r/g, '').replace(/data-v-[a-f0-9]+/g, 'data-v-<scope>')
    const pattern = /([^{}]+)\{([^{}]*)\}/g
    let match
    while ((match = pattern.exec(normalized)) !== null) {
        const selector = match[1].trim().replace(/\s+/g, ' ')
        const declarations = match[2]
            .split(';')
            .map(item => item.trim())
            .filter(Boolean)
            .map(item => {
                const separator = item.indexOf(':')
                return [item.slice(0, separator).trim(), item.slice(separator + 1).trim().replace(/\s+/g, ' ')]
            })
        rules.push({ selector, declarations })
    }
    return {
        scoped: /data-v-[a-f0-9]+/.test(css),
        rules
    }
}

const createVueRuntime = translationState => {
    const vnode = (kind, type, props, children) => ({
        kind,
        type,
        props: props || {},
        children
    })
    return {
        Fragment: 'Fragment',
        createElementBlock: (type, props, children) => vnode('element-block', type, props, children),
        createElementVNode: (type, props, children) => vnode('element', type, props, children),
        createVNode: (type, props, children) => vnode('component', type, props, children),
        defineComponent: component => component,
        h(type, props, children) {
            if (arguments.length === 2 && (typeof props === 'string' || Array.isArray(props))) {
                return vnode('h', type, null, props)
            }
            return vnode('h', type, props, children)
        },
        inject: key => {
            if (key !== 'i18n.format') {
                throw new Error(`Unexpected injection: ${key}`)
            }
            return value => `${translationState.prefix}${value}`
        },
        openBlock() {},
        resolveComponent: name => name,
        toDisplayString: value => String(value),
        unref: value => value
    }
}

const createDocument = observations => ({
    readyState: 'complete',
    body: {
        firstChild: {},
        insertBefore(node) {
            observations.bodyNodes.push(node)
        }
    },
    head: {
        appendChild(node) {
            observations.headNodes.push(node)
        }
    },
    addEventListener() {},
    getElementById() {
        return null
    },
    createElement(tagName) {
        return createDomNode(tagName)
    },
    createElementNS(namespace, tagName) {
        const node = createDomNode(tagName)
        node.namespace = namespace
        return node
    }
})

const createDomNode = tagName => ({
    tagName,
    attributes: {},
    style: {},
    setAttribute(name, value) {
        this.attributes[name] = value
    }
})

const createOptions = requestResult => {
    const observations = {
        alerts: [],
        extensionAttributes: [],
        formatCalls: [],
        loadedClasses: [],
        locales: [],
        requests: [],
        resourceLookups: []
    }
    const options = {
        i18n: {
            add(locale, messages) {
                observations.locales.push({ locale, messages: clone(messages) })
            },
            format(...args) {
                observations.formatCalls.push(clone(args))
                return `translated:${JSON.stringify(args)}`
            }
        },
        JavaClass: {
            setExtensionAttribute(type, provider) {
                observations.extensionAttributes.push({ type, provider })
            },
            loadClass(type) {
                observations.loadedClasses.push(type)
            }
        },
        request: {
            sendJson(url, payload) {
                observations.requests.push({ url, payload: clone(payload) })
                return {
                    success(callback) {
                        callback(requestResult)
                        return this
                    }
                }
            }
        },
        modal: {
            alert(message, title) {
                observations.alerts.push({ message, title })
            }
        }
    }
    return { observations, options }
}

const normalizeRows = (root, info, updateValues) => {
    const rows = Array.isArray(root) ? root : root.children
    return rows.map((row, index) => {
        const label = row.children[0]
        const control = row.children[1]
        const props = control.props || {}
        let updateField = null
        if (updateValues) {
            const before = clone(info)
            props['onUpdate:value'](`updated-${index}`)
            updateField = Object.keys(info).find(key => info[key] !== before[key])
            info[updateField] = before[updateField]
        }
        return {
            rowClass: row.props.class,
            labelKey: label.children,
            control: control.type,
            value: props.value,
            updateField,
            placeholderKey: props.placeholder,
            options: props.options ? clone(props.options) : null,
            width: props.width || null,
            language: props.language || null,
            style: props.style ? clone(props.style) : null
        }
    })
}

const normalizeComponent = (component, translationState) => {
    const info = clone(SAMPLE_INFO)
    const render = component.setup({ info })
    const rows = normalizeRows(render({}, []), info, true)
    translationState.prefix = 'rerendered:'
    const rerenderedRows = normalizeRows(render({}, []), info, false)

    return {
        name: component.__name || component.name,
        defaultInfo: clone(component.props.info.default),
        rows,
        rerenderedTranslations: rerenderedRows.map(row => ({
            labelKey: row.labelKey,
            placeholderKey: row.placeholderKey,
            options: row.options
        }))
    }
}

const normalizeService = (factory, requestResult) => {
    const { observations, options } = createOptions(requestResult)
    const plugin = factory(options)
    const datasource = plugin.datasources[0]
    const extension = observations.extensionAttributes[0]
    const attributesBeforeInjection = extension.provider()

    datasource.service.injectResources(type => {
        observations.resourceLookups.push(type)
        return [{
            type,
            children: [
                { key: 'chat', name: 'Chat model' },
                { key: 'embedding', name: 'Embedding model' }
            ]
        }]
    })
    const extensionAttributes = extension.provider()
    datasource.service.doTest({ key: 'chat' })

    return {
        requestResult,
        dialogWidth: datasource.service.dialogWidth,
        extensionType: extension.type,
        attributesBeforeInjection: clone(attributesBeforeInjection),
        extensionAttributes: clone(extensionAttributes),
        resourceLookups: observations.resourceLookups,
        loadedClasses: observations.loadedClasses,
        requests: observations.requests,
        alerts: observations.alerts
    }
}

export const normalizePluginArtifact = (source, filename) => {
    const domObservations = { bodyNodes: [], headNodes: [], opened: [] }
    const translationState = { prefix: '' }
    const context = {
        Vue: createVueRuntime(translationState),
        document: createDocument(domObservations),
        window: {
            open(...args) {
                domObservations.opened.push(args)
            }
        }
    }
    vm.createContext(context)
    vm.runInContext(source, context, { filename })
    const factory = context.MagicLangchain4j
    if (typeof factory !== 'function') {
        throw new Error('MagicLangchain4j global factory was not installed')
    }

    const { observations, options } = createOptions('ok')
    const plugin = factory(options)
    const datasource = plugin.datasources[0]
    const statusBarItems = (plugin.statusBarItems || []).map(item => {
        item.onClick()
        return {
            icon: item.icon,
            title: item.title,
            displayKey: item.displayKey,
            order: item.order,
            opened: clone(domObservations.opened.pop())
        }
    })
    const icon = domObservations.bodyNodes[0]
    const styles = domObservations.headNodes
        .filter(node => node.tagName === 'style')
        .map(node => normalizeCss(node.innerHTML))

    return clone({
        artifact: {
            filename,
            bytes: Buffer.byteLength(source),
            sha256: sha256(source)
        },
        locales: observations.locales,
        registration: {
            datasourceCount: plugin.datasources.length,
            datasource: {
                type: datasource.type,
                icon: datasource.icon,
                title: datasource.title,
                name: datasource.name,
                componentName: datasource.component.__name || datasource.component.name
            }
        },
        component: normalizeComponent(datasource.component, translationState),
        services: {
            success: normalizeService(factory, 'ok'),
            failure: normalizeService(factory, 'connection refused')
        },
        styles,
        icon: {
            domId: icon.id,
            namespace: icon.namespace,
            attributes: icon.attributes,
            style: icon.style,
            symbolId: 'magic-langchain4j-langchain4j',
            symbolBytes: Buffer.byteLength(icon.innerHTML),
            symbolSha256: sha256(icon.innerHTML)
        },
        statusBarItems
    })
}
