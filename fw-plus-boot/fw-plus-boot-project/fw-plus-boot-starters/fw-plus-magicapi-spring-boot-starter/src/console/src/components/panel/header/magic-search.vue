<template>
	<magic-dialog :title="$i('message.search')" v-model:value="show" :shade="false" padding="0" width="700px" top="60px">
		<magic-input v-model:value="keyword" :placeholder="$i('message.searchText')" />
		<template v-if="results.length >0">
			<div class="magic-search-result">
				<div v-for="(item, key) in results" :key="key" class="magic-search-result-item" :class="{ selected: selectedItem === item}" @click="onClick(item)" @dblclick="onClick(item, true)">
					<div class="label" v-html="item.text"></div>
					<div class="name"><magic-text-icon :icon="item.icon" />{{ item.name }}</div>
					<div class="line" v-text="item.line"></div>
				</div>
			</div>
			<div class="display-text"><magic-text-icon :icon="selectedItem.icon" />{{displayText}}</div>
			<magic-monaco-editor readonly :value="selectedItem.script" :language="selectedItem.language" style="width: 100%; height:300px" :matches="keyword"></magic-monaco-editor>
		</template>
	</magic-dialog>
</template>
<script setup>
import { computed, inject, ref, watch } from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import Message from '../../../scripts/constants/message.js'
import request from '../../../scripts/request.js'
import { TokenizationRegistry } from 'monaco-editor/esm/vs/editor/common/modes.js'
import { tokenizeToString } from 'monaco-editor/esm/vs/editor/common/modes/textToHtmlTokenizer.js'
const keyword = ref('')
const show = ref(false)
const findResource = inject('findResource')
const services = inject('service')
const results = ref([])
const selectedItem = ref({})
const displayText = computed(() => selectedItem.value.name + (selectedItem.value.path ? `(${selectedItem.value.path})` : ''))
const fetchScript = item => {
	if(!item.script){
		request.sendGet(`/resource/file/${item.id}`).success(data => {
			item.script = data.script   
		})
	}
}
const onClick = (item, open) => {
	selectedItem.value = item
	fetchScript(item)
	if(open) {
		bus.$emit(Message.OPEN_WITH_ID, item.id)
		show.value = false
		results.value = []
		keyword.value = ''
	}
}
bus.$on(Message.DO_SEARCH, () => {
	results.value = []
	keyword.value = ''
	show.value = !show.value
})
let timer = null
const getTextNodeList = (dom) => {
	const nodeList = [...dom.childNodes]
	const textNodes = []
	while (nodeList.length) {
		const node = nodeList.shift()
		if (node.nodeType === node.TEXT_NODE) {
			textNodes.push(node)
		} else {
			nodeList.unshift(...node.childNodes)
		}
	}
	return textNodes
}

const getTextInfoList = (textNodes) => {
	let length = 0
	return textNodes.map(node => {
		let startIdx = length, endIdx = length + node.wholeText.length
		length = endIdx
		return {
			text: node.wholeText,
			startIdx,
			endIdx
		}
	})
}
const getMatchList = (content, keyword) => {
	const characters = [...'[]()?.+*^${}:'].reduce((r, c) => (r[c] = true, r), {})
	keyword = keyword.split('').map(s => characters[s] ? `\\${s}` : s).join('[\\s\\n]*')
	const reg = new RegExp(keyword, 'gmi')
	return [...content.matchAll(reg)] // matchAll结果是个迭代器，用扩展符展开得到数组
}
const replaceMatchResult = (textNodes, textList, matchList) => {
	// 对于每一个匹配结果，可能分散在多个标签中，找出这些标签，截取匹配片段并用font标签替换出
	for (let i = matchList.length - 1; i >= 0; i--) {
		const match = matchList[i]
		const matchStart = match.index, matchEnd = matchStart + match[0].length // 匹配结果在拼接字符串中的起止索引
		// 遍历文本信息列表，查找匹配的文本节点
		for (let textIdx = 0; textIdx < textList.length; textIdx++) {
			const { text, startIdx, endIdx } = textList[textIdx] // 文本内容、文本在拼接串中开始、结束索引
			if (endIdx < matchStart) continue // 匹配的文本节点还在后面
			if (startIdx >= matchEnd) break // 匹配文本节点已经处理完了
			let textNode = textNodes[textIdx] // 这个节点中的部分或全部内容匹配到了关键词，将匹配部分截取出来进行替换
			const nodeMatchStartIdx = Math.max(0, matchStart - startIdx) // 匹配内容在文本节点内容中的开始索引
			const nodeMatchLength = Math.min(endIdx, matchEnd) - startIdx - nodeMatchStartIdx // 文本节点内容匹配关键词的长度
			if (nodeMatchStartIdx > 0) textNode = textNode.splitText(nodeMatchStartIdx) // textNode取后半部分
			if (nodeMatchLength < textNode.wholeText.length) textNode.splitText(nodeMatchLength)
			const span = document.createElement('span')
			span.innerText = text.substr(nodeMatchStartIdx, nodeMatchLength)
			span.className = 'keyword'
			textNode.parentNode.replaceChild(span, textNode)
		}
	}
}
const replaceKeywords = (htmlString, keyword) => {
	if (!keyword) return htmlString
	const div = document.createElement('div')
	div.innerHTML = htmlString
	const textNodes = getTextNodeList(div)
	const textList = getTextInfoList(textNodes)
	const content = textList.map(({ text }) => text).join('')
	const matchList = getMatchList(content, keyword)
	replaceMatchResult(textNodes, textList, matchList)
	return div.innerHTML
}
watch(keyword, (val) => {
	const text = val.trim()
	clearTimeout(timer)
	if(text){
		timer = setTimeout(()=>{
			request.send('/search', { keyword: text} ,{ method: 'POST'}).success(async data => {
				const list = []
				for (let index = 0; index < data.length; index++) {
					const item = data[index];
					const find = findResource(item.id)
					const config = services[find.type]
					const language = config.language || 'magicscript'
					const tokenizer = await TokenizationRegistry.getPromise(language)
					list.push({
						...item,
						icon: config.getIcon(find.item),
						text: replaceKeywords(await tokenizeToString(item.text, tokenizer), text),
						name: find && find.name || 'unknown',
						script: '',
						language
					})
				}
				if(list.length > 0){
					selectedItem.value = list[0]
					fetchScript(selectedItem.value)
				}
				results.value = list
			})
		}, 600)
	}
})
</script>
<style scoped>
.magic-search-result {
  overflow: auto;
  max-height: 200px;
  background-color: var(--navbar-body-background-color);
}
.magic-search-result .magic-search-result-item {
  display: flex;
  padding: 0 5px;
  line-height: 20px;
}
.magic-search-result .magic-search-result-item:hover,
.magic-search-result .magic-search-result-item.selected {
  background-color: var(--tree-hover-background-color);
}
.magic-search-result .magic-search-result-item .label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.magic-search-result .magic-search-result-item .label :deep(.keyword) {
  background: #FFDE7B;
  color: #000000;
}
.magic-search-result-item .name, .magic-search-result-item .line{
  color: var(--resource-span-color)
}
.magic-search-result .magic-search-result-item .line {
  padding-left: 5px;
}

.display-text {
  height: 30px;
  line-height: 30px;
  border-top: 1px solid var(--main-border-color);
  border-bottom: 1px solid var(--main-border-color);
}
</style>