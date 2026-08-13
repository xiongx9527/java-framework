let activeRuntime = {}

export const setMagicMqttDashboardRuntime = opt => {
    activeRuntime = {
        request: opt.request,
        modal: opt.modal,
        constants: opt.constants,
        dashboard: opt.dashboard || (typeof window === 'undefined' ? {} : window.MICA_MQTT_DASHBOARD_CONFIG || {}),
        format: opt.i18n.format
    }
}

export const getMagicMqttDashboardRuntime = () => activeRuntime
