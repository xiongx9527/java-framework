let activeRuntime = {}

export const setPulsarRuntime = opt => {
    activeRuntime = {
        request: opt.request,
        modal: opt.modal,
        bus: opt.bus,
        Message: opt.Message,
        format: opt.i18n.format
    }
}

export const getPulsarRuntime = () => activeRuntime
