import { h, reactive, watch, toRaw, nextTick, ref } from 'vue'
import './magic-tree.css'
import { sortArrayObject } from '../../../scripts/utils.js'

export default {
  props: {
    data: Array,
    // 排序是否倒序
    sort: Boolean,
    descending: Boolean,
    filter: String,
    filterText: Function,
    selected: Object,
    // 节点是否可拖拽
    draggable: {
      type: Boolean,
      default: false
    },
    onMove: {
      type: Function,
      defeault: () => new Promise(reslove => reslove(true))
    }
  },
  emits: ['drag', 'contextmenu','onMove', 'itemClick'],
  setup(props, context) {
    const emits = context.emit
    // 拖拽中的对象
    const draggableItem = reactive({
      item: {}, // 拖拽对象
      itemParent: {}, // 拖拽父级对象
      target: {}, // 目标对象
      targetParent: {} // 目标父级对象
    })
    // 拖拽事件
    function itemDraggable(parent, item, event, type) {
      // 判断是否能满足拖拽条件
      function judgeDraggable() {
        // 拖拽元素和目标元素相同
        if (draggableItem.item === draggableItem.target) {
          return false
        }
        // 拖拽父元素和目标元素相同
        if (draggableItem.itemParent === draggableItem.target) {
          return false
        }
        // 拖拽父元素和目标父元素相同
        if (!draggableItem.target.folder && draggableItem.itemParent === draggableItem.targetParent) {
          return false
        }
        return true
      }
      switch (type) {
        case 'dragstart':
          // 开始拖拽
          draggableItem.item = item
          draggableItem.itemParent = parent
          event.stopPropagation()
          break
        case 'dragenter':
          // 拖拽到某个元素上
          draggableItem.target = item
          draggableItem.targetParent = parent
          event.stopPropagation()
          break
        case 'dragover':
          // 拖拽元素上面
          if (judgeDraggable()) {
            event.preventDefault()
          }
          break
        case 'dragend':
          // 结束拖拽
          if (!judgeDraggable()) {
            break
          }
          props.onMove(item, draggableItem.target || draggableItem.targetParent).then(success => {
            if(success){
              // 删除旧的元素
              const itemParent = draggableItem.itemParent || { children: props.data }
              itemParent.children.splice(itemParent.children.indexOf(item), 1)
              if (draggableItem.target.folder) {
                draggableItem.target.children = draggableItem.target.children || []
                draggableItem.target.children.push(draggableItem.item)
              } else {
                draggableItem.targetParent.children = draggableItem.targetParent.children || []
                draggableItem.targetParent.children.push(draggableItem.item)
              }
              if(props.sort){
                sortArrayObject(draggableItem.target.folder ? draggableItem.target.children : draggableItem.targetParent.children, props.descending, 'name')
              }
              draggableItem.target = {}
              draggableItem.targetParent = {}
            }
          })
          event.stopPropagation()
          break
      }
      emits('drag', type, draggableItem, event)
    }
    if(props.sort){
      // 对数据进行深度排序
      sortArrayObject(props.data, props.descending, 'name', 'children')
      watch(
        () => props.data,
        (val) => {
          sortArrayObject(props.data, props.descending, 'name', 'children')
        }
      )
      watch(
        () => props.descending,
        (val) => {
          sortArrayObject(props.data, props.descending, 'name', 'children')
        }
      )
    }
    return {
      draggableItem,
      itemDraggable
    }
  },
  data() {
    return {
      scrollId: '',
      scrollItem: ref(null)
    }
  },
  methods: {
    scrollIntoView(itemOrId){
      this.scrollId = 's' + new Date().getTime() + '' +parseInt(Math.random() * 10000000)
      this.scrollItem = itemOrId
      const deepFind = array => {
        let find = false
        for (const i in array) {
          const it = array[i]
          if(find = (it === itemOrId)){
            if(it.opened !== undefined){
              it.opened = true
            }
            break;
          } else if (it.children && it.children.length > 0){
            if(find = deepFind(it.children)){
              it.opened = true
              break;
            }
          }
        }
        return find
      }
      if(deepFind(this.data)){
        nextTick(() => {
          const element = document.querySelector('#' + this.scrollId)
          element && element.scrollIntoView(true)
        })
      }
    }
  },
  render() {
    const _filter = (item) => {
      if(this.filter){
        return this.filterText(toRaw(item)).indexOf(this.filter) > -1 || (item.children && item.children.some(it => _filter(it)))
      }
      return true
    }
    const renderChild = (parent, item, level) => {
      const style = level > 0 ? { paddingLeft: `${level * 17}px` } : {}
      return _filter(item) && h(
        'li',
        {
          onDblclick: (e) => {
            if (item.folder) {
              item.opened = !item.opened
            }
            e.stopPropagation()
            e.preventDefault()
          },
          onClick: (e) => {
            this.$emit('itemClick', item)
            e.stopPropagation()
            e.preventDefault()
          },
          onContextmenu: (e) => {
            this.$emit('contextmenu', item, e)
            e.stopPropagation()
            e.preventDefault()
          },
          // 拖拽相关
          draggable: this.draggable,
          onDragenter: (e) => this.itemDraggable(parent, item, e, 'dragenter'),
          onDragstart: (e) => this.itemDraggable(parent, item, e, 'dragstart'),
          onDragend: (e) => this.itemDraggable(parent, item, e, 'dragend'),
          onDragover: (e) => this.itemDraggable(parent, item, e, 'dragover')
        },
        [
          h(
            'div',
            { class: ['magic-tree-item', item === this.draggableItem.target ? 'draggable-target-item' : '', item === this.selected ? 'selected': ''], style, id: this.scrollItem === item || this.scrollItem === item.id ? this.scrollId : undefined },
            item.folder ? this.$slots.folder({ item }) : this.$slots.file({ item })
          ),
          item.folder && item.opened &&
            h(
              'ul',
              { class: (item.opened && 'opened') || 'hide' },
              item.children && item.children.map((it) => renderChild(item, it, level + 1))
            )
        ]
      )
    }
    return h('div', {
      class: 'magic-tree none-select' 
    }, h(
      'ul', {},
      this.data && this.data.map((it) => renderChild(null, it, 0))
    ))
    
  }
}
