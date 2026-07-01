<template>
    <div class="magic-event">
        <magic-panel-toolbar :toolbars="toolbars"/>
        <div>
            <magic-table :data="data" border align="left">
                <magic-table-column :title="$i('message.date')" width="180" #default="{ row }">
                    {{ row.timestamp }}
                </magic-table-column>
                <magic-table-column :title="$i('event.message')" #default="{ row }">
                    <div v-html="row.content"></div>
                </magic-table-column>
            </magic-table>
        </div>
    </div>
</template>
<script setup>
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
const data = bus.getStatusLog()
const toolbars = [{
    title: $i('message.clear'),
    icon: 'clear',
    onClick: () => {
        bus.clearStatusLog()
    }
}]
</script>
<style scoped>
.magic-event,
.magic-event :deep(.magic-table){
    display: flex;
    flex: 1;
}
.magic-event > div{
	background-color: var(--navbar-body-background-color);
	flex: 1;
	overflow: hidden;
	position: relative;
	height: 100%;
}
.magic-event :deep(.magic-table){
    height: 100%;
	position: absolute;
	width: 100%;
}
.magic-event :deep(.magic-table .magic-table-header){
    display: none;
}
.magic-event :deep(.magic-table .magic-table-row){
    border-right: 1px solid var(--main-border-color);
}
.magic-event :deep(.magic-table .magic-table-column){
    padding-left : 5px
}
</style>