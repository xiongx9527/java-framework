<template>
	<div class="magic-log-wrapper">
		<magic-panel-toolbar :toolbars="toolbars"/>
		<magic-empty v-if="!logs ||logs.length === 0" :text="$i('message.empty', $i('message.log'))"/>
		<div v-show="logs && logs.length > 0" class="magic-log" ref="element"  @contextmenu.prevent="e=> onContextMenu(e)">
			<div v-for="(item, key) in logs" :class="{ multiple: item.multiple, more: item.showMore }" :key="'run_log_' + key">
				<pre v-html="item.html"></pre>
				<span v-if="item.multiple" class="multiple" @click="item.showMore = !item.showMore">
					{{ item.showMore ? $i('log.hide') : $i('log.show', item.lines)}}
				</span>
			</div>
		</div>
	</div>
</template>
<script setup>
import { getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import Socket from '../../../scripts/constants/socket.js'
import constants from '../../../scripts/constants.js'
const logs = ref([])
const keepScroll = ref(false)
const toolbars = ref([{
	icon: 'expand-all',
	title: $i('resource.header.expand'),
	onClick: ()=> logs.value.forEach(it => it.showMore = true)
}, {
	icon: 'collapse-all',
	title: $i('resource.header.collapse'),
	onClick: ()=> logs.value.forEach(it => it.showMore = false)
}, {
	icon: 'delete',
	title: $i('message.clear'),
	onClick: ()=> logs.value.splice(0)
}, {
	icon: 'scroll-down',
	title: $i('log.scrollEnd'),
	selectable: true,
	onSelect: (val)=> {
		keepScroll.value = val
		updateScroll()
	}
}])
const element = ref(null)
const { proxy } = getCurrentInstance()
const updateScroll = () => {
	if(keepScroll.value){
		nextTick(() => {
			if(element.value){
				element.value.scrollTop = element.value.scrollHeight
			}
		})
	}
}
const onContextMenu = event => {
	const selection = window.getSelection()
	const selectedText = selection.toString()
	proxy.$contextmenu({
		event,
		menus: [{
			icon: 'copy',
			label: $i('message.copy'),
			onClick: () => {
				if (selectedText) {
					navigator.clipboard.writeText(selectedText).then(() => {
						proxy.$message.success($i('message.copySuccess', $i('message.log')))
					}).catch(() => {
						proxy.$message.error($i('message.copyFailed', $i('message.log')))
					})
				}
			},
			disabled: !selectedText
		}, {
			icon: 'delete',
			label: $i('message.clear'),
			onClick: ()=> logs.value.splice(0)
		}, {
			icon: 'expand-all',
			label: $i('resource.header.expand'),
			onClick: ()=> logs.value.forEach(it => it.showMore = true)
		}, {
			icon: 'collapse-all',
			label: $i('resource.header.collapse'),
			onClick: ()=> logs.value.forEach(it => it.showMore = false)
		}]
	})
}
const _pushLog = items => {
	const messages = items.map(text => {
		// escape
		let html = text.replace(/[&<>]/gm, function (str) {
			return str === "&" ? "&amp;" : str === "<" ? "&lt;" : str === ">" ? "&gt;" : "";
		});
		html = html.replace(/(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}.\d{3}\s+)([^\s]+)( --- \[)(.{15})(] )(.{40})/gm,'$1 <span class="log-$2">$2</span>$3$4$5<span class="log-cyan">$6</span>')
		// 替换链接
		html = html.replace(/(https?:\/\/[^\s]+)/gm, '<a class="log-link" href="$1" target="blank">$1</a>')
		// 处理异常里的 at
		html = html.replace(/(\tat .*\()(.*?:\d+)(\).*?[\r\n])/g,'$1<span style="color:#808080;text-decoration: underline;">$2</span>$3')
		let lines = text.split('\n').length;
		return {
			html,
			multiple: lines > 3,
			lines: lines - 3,
			showMore: false
		}
	})
	if(messages){
		logs.value.push(...messages)
		if(constants.LOG_MAX_ROWS !== Infinity && logs.value.length > constants.LOG_MAX_ROWS){
			logs.value.splice(0, logs.value.length - constants.LOG_MAX_ROWS)
		}
		updateScroll()
	}
}
bus.$event(Socket.LOGS, ([ rows ]) => _pushLog(rows))
bus.$event(Socket.LOG, ([ row ]) => _pushLog(row))
const observer = new IntersectionObserver(() => {
	updateScroll()
})
onMounted(() => {
	observer.observe(element.value)
})

</script>
<style>
.magic-log-wrapper{
	display: flex;
	flex: 1;
	height: 100%;
	overflow: hidden;
	position: relative;
}
.magic-log {
	font-size: 13.5px;
	padding: 5px;
	flex: 1;
	overflow: auto;
	position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: var(--magic-panel-toolbar-width);
}
.magic-log > div{
	width: 100%;
}
.magic-log > div pre{
	line-height: 20px;
	width: 100%;
}
.magic-log > div.multiple pre{
	max-height: 60px;
	overflow: hidden;
}
.magic-log > div.multiple.more pre{
	max-height: none;
}
.magic-log span.multiple{
	opacity: 0.5;
	font-size: 13px;
	text-decoration: underline;
	cursor: pointer
}

.magic-log pre span.log-INFO{
		color: var(--log-color-info);
}
.magic-log pre span.log-DEBUG{
		color: var(--log-color-debug);
}
.magic-log pre span.log-ERROR{
		color: var(--log-color-error);
}
.magic-log pre span.log-WARN{
		color: var(--log-color-warn);
}
.magic-log pre span.log-TRACE{
		color: var(--log-color-trace);
}
.magic-log pre span.log-cyan{
		color: var(--log-color-cyan);
}
.magic-log pre a.log-link{
		color: var(--log-color-link);
}
</style>