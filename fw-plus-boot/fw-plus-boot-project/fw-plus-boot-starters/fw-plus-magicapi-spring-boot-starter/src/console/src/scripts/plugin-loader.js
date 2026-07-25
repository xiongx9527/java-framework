export const installPlugin = ({ factory, plugin, constants, Message, bus, ...pluginContext }) => {
    const pluginInstance = factory({
        ...pluginContext,
        constants,
        Message,
        bus,
        pluginData: plugin
    })
    if (pluginInstance !== null && typeof pluginInstance === 'object') {
        constants.PLUGINS.push(pluginInstance)
        bus.$emit(Message.PLUGINS_CHANGED)
    }
    return pluginInstance
}
