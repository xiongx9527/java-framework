import { ref, h, Teleport, computed  } from 'vue'
import MagicIcon from '../icon/magic-icon.vue'
import './magic-navbar.css'
export default {
    props: {
        direction: {
            type: String,
            default: ''
        },
        reverse: {
            type: Boolean,
            default: false
        },
        defaultSelect: {
            type: Number,
            default: 0
        },
        allowClose: {
            type: Boolean,
            default: true
        },
        tooltipDirection: {
            type: String,
            default: 'right'
        },
        spliter: Boolean,
        value: Array,
        to: HTMLElement
    },
    setup(props, context) {
        const selectIndex = ref(props.defaultSelect)
        return {
            slots: computed(() => context.slots.default()[0].children),
            navbars: computed(() => context.slots.default()[0].children.flatMap(it => it.props)),
            selectIndex
        }
    },
    methods: {
        select(index){
            this.selectIndex = index
        }
    },
    render() {
        const renderSvg = word => {
            const pathId = parseInt(Math.random() * 100000000)
            const height = word.length * 6
            return h('svg', {
                xmlns: 'http://www.w3.org/2000/svg',
                version: '1.1',
                width: 22,
                height
            }, [ 
                h('defs', h('path', { id: `path-text-${pathId}`, d: this.reverse ? `M6,0 L6,${height}` : `M14,${height} L14,0`})),
                h('text', { fill: 'var(--main-color)' }, h('textPath', { 'xlink:href': `#path-text-${pathId}`}, word))
            ])
        }
        const splitTitle = (title) => {
            if(this.spliter && this.direction === 'vertical'  && title.match(/\w/g)) {
                const elements = [];
                let last = ''
                let flag = 1
                let lastFlag
                const words = title.split('')
                for(let i=0, len = words.length; i< len;i++){
                    const word = words[i]
                    flag = word.match(/\w/) ? 1 : 2
                    if(flag == lastFlag){
                        last += word
                    } else {
                        last && elements.push(lastFlag === 1 ? renderSvg(last) : last)
                        last = word
                    }
                    lastFlag = flag
                }
                last && elements.push(lastFlag === 1 ? renderSvg(last) : last)
                return elements
            }
            return [title];
        }
        const childs = [h('ul',{ class: 'magic-navbar-header none-select' }, this.navbars.map((navbar, index) => {
            const elements = [h('div', { class: 'magic-navbar-title'}, [ ...splitTitle(navbar.title) ])]
            if(navbar.icon) {
                elements.push(h(MagicIcon, { icon: navbar.icon}))
            }
            const style = navbar.style || {}
            if(navbar.show === false){
                style['display'] = 'none'
            }
            return h('li', {
                class: this.selectIndex === index ? 'selected' : '',
                'data-title': navbar.title,
                'data-tooltip-direction': this.tooltipDirection,
                style,
                onClick: ()=> {
                    if(this.selectIndex === index && this.allowClose){
                        this.selectIndex = -1
                    }else {
                        this.selectIndex = index
                    }
                } 
            }, elements)
        }))]
        this.slots.forEach((slot, index) => {
            const style = index !== this.selectIndex || slot.props.show === false ? { display: 'none' } : {};
            if(this.to){
                childs.push(h(Teleport, {to: this.to}, h('div', { class:"magic-navbar-body", style}, slot)))
            }else {
                childs.push(h('div', { class: 'magic-navbar-body', style }, slot))
            }
            
        })
        return h('div', {
            class: `magic-navbar magic-navbar__${this.direction}` + (this.reverse ? ' reverse': '')
        }, childs)
    }
}