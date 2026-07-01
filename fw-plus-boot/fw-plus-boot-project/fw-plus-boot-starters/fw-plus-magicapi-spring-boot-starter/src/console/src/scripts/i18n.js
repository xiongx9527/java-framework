import store from './store.js'
import en from './i18n/en.js'
import zhCN from './i18n/zh-cn.js'
const localeName = store.get('locale')
let locale = zhCN
if(localeName === 'en'){
    locale = en
}

export default function doLocale(key, ...args){
    try{
        const msg = key.split('.').reduce((val, k) => val[k], locale);
        if(msg && args.length > 0){
            return msg.replace(/\{(\d+)\}/g, (match, index) => args[index])
        }
        return msg || key;
    } catch(e){
        return key
    }
}

export function add(addLocalName, value){
    let addLocale = zhCN
    if(addLocalName === 'en'){
        addLocale = en
    }
    const appendValue = (target, object) => {
        Object.entries(object).forEach(([key, value]) => {
            if(typeof value === 'string'){
                target[key] = value
            } else {
                if(!target[key]){
                    target[key] = {}
                }
                appendValue(target[key], value)
            }
        })
    }
    appendValue(addLocale, value)
}

export function translateCode(code, message){
    if(zhCN === locale){
        return message
    }
    const src = zhCN.code[`${code}`]
    const target = locale.code[`${code}`]
    if(src && target){
        const regex = src.replace(/([()\[\]\.])/g, '\\$1').replace(/{\d+}/g, '(.*?)')
        if(regex){
            const result = []
            const exp = new RegExp(regex, 'g')
            let str
            while((str = exp.exec(message)) != null){
                result.push(str)
            }
            return doLocale(`code.${code}`, result.splice(0, 1))
        }
    }
    return message
}
