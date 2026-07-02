<template>
    <ul class="magic-context-menu none-select" :style="style" ref="element">
        <li v-for="(menu, key) in menus" :key="key" :class="{ divided: menu.divided }" @click.stop="() => {onMouseup(); menu.onClick && menu.onClick()}">
            <span v-if="menus.some(it => it.icon)" class="magic-context-menu-icon"><magic-icon :icon="menu.icon" v-if="menu.icon" size="12px"/></span>
            <label>{{ menu.label }}</label>
            <template  v-if="menu.children">
                <span class="magic-context-menu-icon right"><magic-icon icon="right" size="12px"/></span>
                <ul class="magic-context-menu none-select">
                    <li v-for="(item, key) in menu.children" :key="key" :class="{ divided: item.divided }" @click.stop="() => {onMouseup(); item.onClick && item.onClick()}">
                        <span v-if="menu.children.some(it => it.icon)" class="magic-context-menu-icon"><magic-icon :icon="item.icon" v-if="item.icon" size="12px"/></span>
                        <label>{{ item.label }}</label>
                    </li>
                </ul>
            </template>
        </li>
    </ul>
</template>
<script>
import {ref} from 'vue'

export default {
    props: {
        menus: Array,
        onDestory: Function,
        position: Object
    },
    data() {
        return {
            style: {
                left: this.position.x + 'px',
                top: this.position.y + 'px',
				element: ref(null)
            }
        }
    },
    mounted(){
        document.addEventListener('click', this.onMouseup)
		const rootRect = document.querySelector('.magic-editor').getBoundingClientRect()
        const xMax = rootRect.x + rootRect.width
		const yMax = rootRect.y + rootRect.height
        const rect = this.$el.getBoundingClientRect()
        if(rect.x + rect.width > xMax){
            this.style.left = `${xMax - rect.width}px`
        }
		if(rect.y + rect.height > yMax){
			this.style.top = `${yMax - rect.height}px`
		}

    },
    unmounted(){
        document.removeEventListener('click', this.onMouseup)
    },
    methods: {
        onMouseup(){
            this.onDestory && this.onDestory()
        }
    }
}
</script>
<style scoped>
.magic-context-menu{
    position: absolute;
    z-index: 999999;
    background-color: var(--context-menu-background);
    border: 1px solid var(--main-border-color);
    padding: var(--context-menu-padding)
}
.magic-context-menu li{
    height: 24px;
    line-height: 24px;
    padding:0 10px;
    cursor: pointer;
    display: flex;
    border-radius: var(--context-menu-item-border-radius);
}
.magic-context-menu li > span,
.magic-context-menu li > label {
    cursor: pointer;
	white-space: pre;
}
.magic-context-menu li.divided{
    margin-bottom: 5px;
    position: relative;
}
.magic-context-menu li.divided::after{
    content: '';
    height: 2px;
    width: 100%;
    border-bottom: 1px solid var(--main-border-color);
    margin-bottom: 2px;
    position: absolute;
    left: 0;
    top: 100%;
}
.magic-context-menu .magic-context-menu-icon{
    display: inline-block;
    text-align: center;
    margin-right: 8px;
    width: 13px;
}
.magic-context-menu li:hover{
    background-color: var(--select-option-hover-background-color);
}
.magic-context-menu li:hover > span,
.magic-context-menu li:hover > label {
    color: var(--select-option-hover-color);
}
.magic-context-menu li:hover > span :deep(.magic-icon){
    fill: var(--select-option-hover-color);
}
.magic-context-menu li:hover .magic-context-menu{
    display: block;
}
.magic-context-menu li .magic-context-menu{
    left: 100%;
    margin-top: -1px;
    display: none;
}
.magic-context-menu li .magic-context-menu-icon.right{
    float: right;
    margin-right: 0px;
    margin-left: 5px;
}
</style>
