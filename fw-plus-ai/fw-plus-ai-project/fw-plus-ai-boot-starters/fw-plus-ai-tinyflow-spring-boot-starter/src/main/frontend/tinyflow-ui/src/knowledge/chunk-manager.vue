<template>
	<section data-testid="chunk-manager">
		<div class="kb-toolbar"><h3 class="kb-subheading">切片管理</h3><div class="kb-actions"><input v-model.trim="documentId" class="kb-input" placeholder="输入 Document ID"><button class="kb-button" @click="load">查询</button><button class="kb-button" data-action="rebuild" title="通过相同来源重新导入可安全重建" @click="hint = '请在文档页使用相同 Source ID 重新导入；新版本完成后才会替换旧索引。'">↻ 重建提示</button></div></div>
		<div v-if="hint" class="kb-success">{{ hint }}</div>
		<div class="kb-panel" style="margin-top:12px">
			<div v-if="loading" class="kb-empty">正在加载切片...</div>
			<div v-else-if="!chunks.length" class="kb-empty">输入 Document ID 后查看切片</div>
			<div v-else class="kb-table-wrap"><table class="kb-table" style="min-width:1120px">
				<thead><tr><th style="width:150px">来源</th><th style="width:190px">Document / Chunk</th><th style="width:34%">原文与切片</th><th style="width:180px">定位 / 父子</th><th>Metadata</th><th style="width:90px">状态</th></tr></thead>
				<tbody><tr v-for="item in chunks" :key="item.chunkId">
					<td><strong>{{ item.sourceId }}</strong><br><span class="kb-badge neutral">{{ item.sourceType }}</span></td>
					<td>{{ item.documentId }}<br><span class="kb-muted">{{ item.chunkId }}</span></td>
					<td><textarea v-model="item.content" class="kb-textarea" :aria-label="`切片 ${item.chunkId}`"></textarea><div class="kb-muted">原文位置：{{ item.location || item.position || '-' }}</div></td>
					<td>{{ item.location || '-' }}<br><span class="kb-muted">父块：{{ item.parentChunkId || '-' }}</span><br><span class="kb-muted">Token：{{ item.tokenCount || '-' }}</span></td>
					<td><code>{{ JSON.stringify(item.metadata || {}) }}</code></td>
					<td><label><input v-model="item.enabled" type="checkbox"> 启用</label><br><button class="kb-button" style="margin-top:8px" @click="save(item)">保存</button></td>
				</tr></tbody>
			</table></div>
		</div>
	</section>
</template>

<script setup>
import { ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } });
const emit = defineEmits(['error']);
const documentId = ref(''); const chunks = ref([]); const loading = ref(false); const hint = ref('');
const load = async () => { if (!documentId.value) return; loading.value = true; try { const page = await props.api.listChunks(props.knowledge.knowledgeId, documentId.value); chunks.value = (page && page.items) || []; } catch (error) { emit('error', error); } finally { loading.value = false; } };
const save = async item => { try { await props.api.updateChunk({ knowledgeId: props.knowledge.knowledgeId, chunkId: item.chunkId, content: item.content, enabled: item.enabled, metadata: item.metadata }); hint.value = `切片 ${item.chunkId} 已保存`; } catch (error) { emit('error', error); } };
</script>
