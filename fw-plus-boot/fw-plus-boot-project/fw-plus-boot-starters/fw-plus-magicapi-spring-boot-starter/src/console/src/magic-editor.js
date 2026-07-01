import { createApp } from 'vue'
import MagicContextMenu from './components/common/magic-context-menu.vue'
import { install as installModal } from './components/common/dialog/magic-modal.js'
import constants from './scripts/constants.js'
const components = {}
Object.entries(import.meta.globEager('./components/**')).forEach(([key, value]) => components[key.substring(key.lastIndexOf('/') + 1, key.lastIndexOf('.'))] =  value.default)
const registerComponent = (app) => {
    let instance;
    Object.entries(components).forEach(([name, component]) => {
        app.component(name, component)
    })
    app.config.globalProperties.$contextmenu = options => {
        if(instance != null){
            instance.unmount()
        }
        instance = createApp(MagicContextMenu, {
            menus: options.menus,
            position: {
                x: options.event.clientX,
                y: options.event.clientY,
            },
            onDestory: ()=> {
                instance && instance.unmount()
                instance = null
            }
        });
        registerComponent(instance)
        instance.mount('.magic-editor .magic-mounts')
    }
    Object.keys(constants).forEach(key => app.config.globalProperties[`$${key}`] = constants[key])
    installModal(app, registerComponent)
}
export default registerComponent