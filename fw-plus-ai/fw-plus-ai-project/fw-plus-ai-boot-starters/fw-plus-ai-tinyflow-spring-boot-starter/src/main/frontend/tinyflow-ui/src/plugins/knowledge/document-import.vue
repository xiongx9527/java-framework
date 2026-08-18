<template>
	<section data-testid="document-import">
		<div class="kb-toolbar"><h3 class="kb-subheading">导入文档</h3><span class="kb-muted">支持 TXT、PDF、DOCX、HTML、CSV、XLSX</span></div>
		<div class="kb-segmented" aria-label="导入方式">
			<button v-for="item in modes" :key="item.id" :class="{ active: mode === item.id }" @click="mode = item.id">{{ item.label }}</button>
		</div>
		<div class="kb-form" style="margin-top:16px">
			<div class="kb-form-grid">
				<label class="kb-field"><span>来源 ID</span><input v-model.trim="sourceId" class="kb-input" placeholder="source-001"></label>
				<label class="kb-field"><span>显示名称</span><input v-model.trim="name" class="kb-input" placeholder="产品资料"></label>
			</div>
			<label v-if="mode === 'file'" class="kb-field"><span>文件（可多选批量导入）</span><input class="kb-input" type="file" multiple accept=".txt,.pdf,.docx,.html,.htm,.csv,.xlsx" @change="files = Array.from($event.target.files || [])"></label>
			<label v-else-if="mode === 'text'" class="kb-field"><span>文本内容</span><textarea v-model="content" class="kb-textarea" placeholder="粘贴需要索引的文本"></textarea></label>
			<label v-else class="kb-field"><span>URL</span><input v-model.trim="url" class="kb-input" type="url" placeholder="https://example.com/docs"></label>
			<div class="kb-form-grid">
				<label class="kb-field"><span>解析器</span><select v-model="parser" class="kb-select"><option value="auto">自动识别</option><option value="text">纯文本 / HTML</option><option value="pdf">PDF</option><option value="docx">DOCX</option><option value="tabular">CSV / XLSX</option></select></label>
				<label class="kb-field"><span>切片策略</span><select v-model="strategy" class="kb-select"><option value="normal">普通切片</option><option value="parent-child">父子切片</option><option value="qa">问答切片</option></select></label>
				<label class="kb-field"><span>切片 Token</span><input v-model.number="maxTokens" class="kb-input" type="number" min="64" max="4096"></label>
				<label class="kb-field"><span>重叠 Token</span><input v-model.number="overlap" class="kb-input" type="number" min="0" :max="maxTokens - 1"></label>
				<label v-if="strategy === 'parent-child'" class="kb-field"><span>父块 Token</span><input v-model.number="parentTokens" class="kb-input" type="number" min="128" max="8192"></label>
				<label class="kb-field"><span>Metadata（JSON）</span><input v-model="metadataText" class="kb-input" placeholder='{"department":"研发"}'></label>
			</div>
			<div v-if="status" class="kb-success" role="status">{{ status }}</div>
			<div v-if="error" class="kb-alert" role="alert">{{ error }}</div>
			<div class="kb-actions"><button class="kb-button primary" data-action="upload" :disabled="submitting" @click="submit">↑ {{ submitting ? '正在提交...' : mode === 'file' && files.length > 1 ? `批量导入 ${files.length} 个文件` : '开始导入' }}</button></div>
		</div>
	</section>
</template>

<script setup>
import { ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } });
const emit = defineEmits(['queued', 'error']);
const modes = [{ id: 'file', label: '文件 / 批量' }, { id: 'text', label: '文本' }, { id: 'url', label: 'URL' }];
const mode = ref('file'); const files = ref([]); const content = ref(''); const url = ref(''); const sourceId = ref('source-001'); const name = ref('');
const parser = ref('auto'); const strategy = ref('normal'); const maxTokens = ref(512); const overlap = ref(64); const parentTokens = ref(1024); const metadataText = ref('{}');
const submitting = ref(false); const status = ref(''); const error = ref('');
const bytesToBase64 = buffer => {
	const bytes = new Uint8Array(buffer); let binary = '';
	for (let offset = 0; offset < bytes.length; offset += 8192) binary += String.fromCharCode(...bytes.subarray(offset, offset + 8192));
	return btoa(binary);
};
const options = () => ({ parser: parser.value, chunkStrategy: strategy.value, chunkSize: maxTokens.value, overlap: overlap.value,
	parentSize: parentTokens.value, childSize: maxTokens.value, metadata: JSON.parse(metadataText.value || '{}') });
const submit = async () => {
	status.value = ''; error.value = ''; submitting.value = true;
	try {
		const common = { knowledgeId: props.knowledge.knowledgeId, sourceId: sourceId.value, name: name.value, options: options() };
		let tasks = [];
		if (mode.value === 'file') {
			if (!files.value.length) throw new Error('请选择至少一个文件');
			for (let index = 0; index < files.value.length; index += 1) {
				const file = files.value[index];
				tasks.push(await props.api.importFile({ ...common, sourceId: files.value.length > 1 ? `${sourceId.value}-${String(index + 1).padStart(3, '0')}` : sourceId.value,
					name: name.value || file.name, filename: file.name, mediaType: file.type || 'application/octet-stream', content: bytesToBase64(await file.arrayBuffer()) }));
			}
		} else if (mode.value === 'text') {
			if (!content.value.trim()) throw new Error('请输入文本内容');
			tasks = [await props.api.importText({ ...common, name: name.value || '文本导入', content: content.value })];
		} else {
			if (!url.value) throw new Error('请输入 URL');
			tasks = [await props.api.importUrl({ ...common, name: name.value || url.value, url: url.value })];
		}
		status.value = `已创建 ${tasks.length} 个导入任务`;
		emit('queued', tasks);
	} catch (cause) { error.value = cause.message; emit('error', cause); } finally { submitting.value = false; }
};
</script>
