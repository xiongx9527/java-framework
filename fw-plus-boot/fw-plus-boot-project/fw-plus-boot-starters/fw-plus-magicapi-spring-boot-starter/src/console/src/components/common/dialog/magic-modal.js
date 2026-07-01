import { createApp } from 'vue'
import MagicAlert from './magic-alert.vue'
import MagicConfirm from './magic-confirm.vue'
let registerComponent = null;
const mount = (component, data) => {
    const element = document.createElement('div')
    document.querySelector('.magic-editor .magic-mounts').appendChild(element)
    const instance = createApp(component, {...data, onClose: () => element.remove()})
    registerComponent(instance)
    instance.mount(element)
}

const exportObject = {
    alert: (message, title, ok) => mount(MagicAlert, { message, title, ok }),
    confirm: (title, message, success) => mount(MagicConfirm, { title, message, success })
}

export const install = (app, _registerComponent)=>{
    registerComponent = _registerComponent
    Object.keys(exportObject).forEach(key => app.config.globalProperties[`$${key}`] = exportObject[key])
}

export default exportObject