import { ref, h, nextTick } from 'vue'
import MagicIcon from '../icon/magic-icon.vue'
import './magic-tab.css'

export default {
  props: {
    tabs: Array,
    className: String,
    value: Object,
    allowClose: {
      type: Boolean
    }
  },
  emits: ['update:value', 'change', 'close', 'item-contextmenu', 'before-change'],
  setup(props) {
    // 拖拽中的对象
    const draggableItem = ref({})
    // 拖拽的目标对象
    const draggableTargetItem = ref({})
    // 拖拽事件
    function tabDraggable(item, event, type) {
      switch (type) {
        case 'dragstart':
          // 开始拖拽
          draggableItem.value = item
          event.stopPropagation()
          break
        case 'dragenter':
          // 拖拽到某个元素上
          draggableTargetItem.value = item
          event.stopPropagation()
          break
        case 'dragend':
          // 结束拖拽
          if (draggableItem.value !== draggableTargetItem.value) {
            const itemIndex = props.tabs.indexOf(draggableItem.value)
            const targetIndex = props.tabs.indexOf(draggableTargetItem.value)
            props.tabs.splice(itemIndex, 1)
            props.tabs.splice(targetIndex, 0, draggableItem.value)
          }
          draggableTargetItem.value = {}
          event.stopPropagation()
          break
        case 'dragover':
          // 拖拽元素上面
          if (draggableItem.value !== draggableTargetItem.value) {
            event.preventDefault()
          }
          break
      }
    }
    // 鼠标滚动事件
    const scrollbar = ref({})
    function scrollbarHandler(e) {
      let delta = e.wheelDelta || e.detail
      scrollbar.value.value.el.scrollLeft += delta > 0 ? -100 : 100
    }
    return {
      selectIndex: ref(0),
      draggableItem,
      draggableTargetItem,
      tabDraggable,
      scrollbar,
      scrollbarHandler,
      scrollId: '',
      scrollItem: ref(null)
    }
  },
  mounted() {
    nextTick(() => {
      this.scrollbar.value.el.addEventListener('DOMMouseScroll', this.scrollbarHandler, false)
      this.scrollbar.value.el.addEventListener('mousewheel', this.scrollbarHandler, false)
    })
  },
  methods: {
    scrollIntoView(itemOrId) {
      this.scrollId = 's' + new Date().getTime() + '' +parseInt(Math.random() * 10000000)
      this.scrollItem = itemOrId
      nextTick(() => {
        const element = document.querySelector('#' + this.scrollId)
        element && element.scrollIntoView(true)
      })
    }
  },
  destroyed() {
    nextTick(() => {
      this.scrollbar.value.el.removeEventListener('DOMMouseScroll', this.scrollbarHandler)
      this.scrollbar.value.el.removeEventListener('mousewheel', this.scrollbarHandler)
    })
  },
  render() {
    const childs =
      this.tabs &&
      h(
        'ul',
        this.tabs.map((tab, index) => {
          const slots = [this.$slots.default({ tab })]
          if (this.allowClose) {
            slots.push(
              h(MagicIcon, {
                icon: 'close',
                size: '12px',
                style: { marginLeft: '5px' },
                onClick: (e) => {
                  this.$emit('close', tab)
                  e.stopPropagation()
                  e.preventDefault()
                }
              })
            )
          }
          return h(
            'li',
            {
              class: [
                this.value === tab ? 'selected' : '',
                tab === this.draggableTargetItem ? 'draggable-target-item' : ''
              ],
              id: this.scrollItem === tab || this.scrollItem === tab.id ? this.scrollId : undefined,
              onClick: (e) => {
                if (this.value !== tab) {
                  this.$emit('before-change', this.value)
                  this.$emit('update:value', tab)
                  this.$emit('change', tab)
                }
                e.stopPropagation()
                e.preventDefault()
              },
              onMousedown: (e) => {
                if (e.which === 2 && this.allowClose) {
                  this.$emit('close', tab)
                }
              },
              onContextmenu: (e) => {
                this.$emit('item-contextmenu', e, tab, index)
                e.preventDefault()
              },
              // 拖拽相关
              draggable: true,
              onDragenter: (e) => this.tabDraggable(tab, e, 'dragenter'),
              onDragstart: (e) => this.tabDraggable(tab, e, 'dragstart'),
              onDragend: (e) => this.tabDraggable(tab, e, 'dragend'),
              onDragover: (e) => this.tabDraggable(tab, e, 'dragover')
            },
            slots
          )
        })
      )

    this.scrollbar.value = childs
    return h(
      'div',
      {
        class: `magic-tab none-select` + (this.className ? ' ' + this.className : '')
      },
      childs
    )
  }
}
