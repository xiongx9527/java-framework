let activeRuntime = {}

export const setMagicMqttDashboardRuntime = opt => {
    activeRuntime = {
        request: opt.request,
        modal: opt.modal,
        constants: opt.constants,
        format: opt.i18n.format
    }
}

export const getMagicMqttDashboardRuntime = () => activeRuntime
