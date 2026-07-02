<template>
  <svg xmlns="http://www.w3.org/2000/svg" version="1.1" :width="item.width" height="12" font-size="10" font-weight="bolder" font-family="inherit">
    <text :x="item.width / 2" y="8" :fill="item.color" style="dominant-baseline: middle;text-anchor:middle;" >{{ item.text }}</text>
  </svg>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({
    icon: String | Array
})
const colors = {
    GET: '#0cbb52',
    POST: '#FFB400',
    DELETE: ['DEL', '#EB2013'],
    PUT: '#097BED',
    'function': ['Fn', '#9012FE']
}
const item = computed(() => {
  let values = props.icon
  let isArray = Array.isArray(values)
  if(!isArray) {
    values = colors[props.icon]
    isArray = values !== undefined && Array.isArray(values)
  }
  const text = isArray ? values[0] : props.icon
  const width = text.length * 9
  const color = isArray ? values[1] : (values || 'var(--main-icon-color)')
  return { width, color, text}
})

</script>
