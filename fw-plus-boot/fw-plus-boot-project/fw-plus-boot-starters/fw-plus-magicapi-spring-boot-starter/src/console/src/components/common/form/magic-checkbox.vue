<template>
    <div class="magic-checkbox" @click.stop="e=>$emit('click',e)">
        <input :id="cboId" ref="checkbox" type="checkbox" @change="() => { $emit('update:value', $refs.checkbox.checked);$emit('change', $refs.checkbox.checked) } " :checked="value"/>
        <label :for="cboId" :class="{ checkedHalf: checkedHalf&&value }"/>
    </div>
</template>
<script setup>
const { value, checkedHalf } = defineProps({
    value: {
        type: [Number, Boolean],
        default: () => false
    },
    checkedHalf: {
        type: Boolean,
        default: false
    }
})
const cboId = new Date().getTime() + '' + Math.floor(Math.random() * 1000)
</script>
<style scoped>
.magic-checkbox {
    width: 100%;
    height: 100%;
    text-align: center;
    display: inline-block;
}

.magic-checkbox input {
    display: none;
}

.magic-checkbox input + label {
    position: relative;
    color: #c9c9c9;
    font-size: 12px;
    height: var(--magic-input-height);
    line-height: var(--magic-input-height);
    width: var(--magic-input-height);
    user-select: none;
    display: inline-block;
}

.magic-checkbox input + label::after {
    display: inline-block;
    background-color: var(--checkbox-background-color);
    border: 1px solid var(--checkbox-border-color);
    content: '';
    width: calc(var(--magic-input-height) - 8px);
    height: calc(var(--magic-input-height) - 8px);
    line-height: calc(var(--magic-input-height) - 8px);
    position: absolute;
    top: 4px;
    left: 4px;
    text-align: center;
    font-size: 12px;
    color: var(--checkbox-text-color);
}

.magic-checkbox input:checked + label::after {
    content: "\2714";
    background-color: var(--checkbox-selected-background-color);
    border-color: var(--checkbox-selected-border-color);
}
.magic-checkbox input+ label.checkedHalf::after {
    content: "\2501";
}
</style>