<template>
    <div :class="`magic-resizer-${this.direction}`" :style="style">
        <slot></slot>
        <div class="magic-resizer-event" :class="{ reverse }" @mousedown="mousedown" ref="resizer"></div>
    </div>
</template>
<script>
import { ref } from 'vue'
export default {
    props: {
        max: Number,
        min: Number,
        value: Number,
        direction: String,
        reverse: {
            type: Boolean,
            default: false
        }
    },
    setup(props){
        const style = ref({
            [props.direction === 'x' ? 'width' : 'height']: (props.value || props.min) + 'px'
        })
        return {
            style
        }
    },
    mounted(){
        new ResizeObserver(this.updateCss).observe(this.$refs.resizer)
    },
    methods: {
        updateCss(){
            try {
                const element = this.$refs.resizer.parentElement
                this.$refs.resizer.style.setProperty('--width', element.offsetWidth + 'px')
                this.$refs.resizer.style.setProperty('--height', element.offsetHeight + 'px')
            } catch (error) {
                
            }
        },
        mousedown(event){
            const horizontal = this.direction === 'x';
            const start = horizontal ? event.clientX : event.clientY
            const rect = event.target.parentElement.getBoundingClientRect()
            document.onmousemove = e => {
                let delta = horizontal ?  rect.width : rect.height;
                if(horizontal){
                    if(this.reverse){
                        delta = start - e.clientX + rect.width
                    }else {
                        delta = e.clientX - start + rect.width
                    }
                }else {
                    if(this.reverse){
                        delta = e.clientY - start + rect.height
                    }else {
                        delta = start - e.clientY + rect.height
                    }
                }
                if (delta >= this.min && delta <= this.max) {
                    this.style[horizontal ? 'width' : 'height'] = `${delta}px`
                }
            }
            document.onmouseup = () => {
                document.onmousemove = document.onmouseup = null
            }
            event.stopPropagation()
            event.preventDefault()
        }
    }
}
</script>
<style scoped>
.magic-resizer-x, .magic-resizer-y{
    display: flex;
    position: relative;
    overflow: hidden;
    transition: none;
}
.magic-resizer-event{
    position: absolute;
    z-index: 20;
    --width: 100%;
    --height: 100%;
}
.magic-resizer-event::after{
    content: ' ';
    position: fixed;
    z-index: 9999;
}
.magic-resizer-x .magic-resizer-event{
    width: 15px;
    height: 100%;
    right: -15px;
    cursor: col-resize;
}
.magic-resizer-x .magic-resizer-event::after{
    width: 10px;
    height: var(--height);
}
.magic-resizer-x .magic-resizer-event.reverse{
    right: unset;
    left: -5px
}
.magic-resizer-x{
    height: 100%
}
.magic-resizer-y .magic-resizer-event{
    height: 10px;
    width: 100%;
    top: -5px;
    cursor: row-resize;
}
.magic-resizer-y .magic-resizer-event::after{
    height: 10px;
    width: var(--width);
}
</style>