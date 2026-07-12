import { h } from 'vue'
import MagicTableColumn from './magic-table-column.vue'
import './magic-table.css'
export default {
    props: {
        data: {
            type: Array,
            default: []
        },
        width: String,
        border: {
            type: Boolean,
            default: false
        },
        align: {
            type: String,
            default: 'center'
        },
    },
    emits: ['clickRow', 'contextmenu', 'loadNext'],
    render() {
        const slots = this.$slots.default();
        const _getTableColumnStyle = (prop) => {
            prop = prop || {}
            let styles = []
            if(prop.width) {
                let widthStyle = `width: ${prop.width}`
                if(!prop.width.endsWith('%') && !isNaN(prop.width)) {
                    widthStyle +='px'
                }
                styles.push(widthStyle)
                styles.push('flex:none')
            } else if (prop.flex) {
                styles.push(`flex: ${prop.flex}`)
            }
            if (prop.align || this.align) {
                styles.push(`justify-content: ${prop.align || this.align};text-align: ${prop.align || this.align}`)
            }
            
            return styles.join(';')
        }
        return h('div', {
            class: `magic-table` + (this.border ? ' magic-table__border' : '' )
        }, [
            h('div', { class: 'magic-table-header none-select' }, slots.map(item => h('div', { class: 'magic-table-column', title: item.props.title, style: _getTableColumnStyle(item.props) }, h('span', item.props.title)))),
            h('div',{ class: 'magic-table-body', onScroll: e => {
                if(e.target.scrollTop + e.target.offsetHeight + 40 >= e.target.scrollHeight){
                    this.$emit('loadNext')
                }
            } }, this.data.map((row, rowIndex) => h(MagicTableColumn, { row, onClick: () => { this.$emit('clickRow', rowIndex)} }, {
                default: () => slots.map(slot => h('div', { 
                    class: 'magic-table-column',
                    onContextmenu: e => { this.$emit('contextmenu', e, row);e.stopPropagation();e.preventDefault()}, 
                    style: _getTableColumnStyle(slot.props) 
                }, slot.children.default({row})) )
            })))
        ])
    }
}