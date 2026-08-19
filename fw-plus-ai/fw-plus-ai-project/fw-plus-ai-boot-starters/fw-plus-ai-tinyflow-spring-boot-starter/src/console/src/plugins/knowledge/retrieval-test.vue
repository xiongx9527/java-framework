<template>
	<section data-testid="retrieval-test">
		<div class="kb-split">
			<form class="kb-form" @submit.prevent="run">
				<h3 class="kb-subheading">召回参数</h3>
				<label class="kb-field"><span>查询文本</span><textarea v-model="query" class="kb-textarea" required></textarea></label>
				<label class="kb-field"><span>检索方式</span><select v-model="method" class="kb-select"><option value="vector">向量</option><option value="keyword">关键词</option><option value="hybrid">混合</option></select></label>
				<div class="kb-form-grid"><label class="kb-field"><span>Top K</span><input v-model.number="topK" class="kb-input" type="number" min="1" max="100"></label><label class="kb-field"><span>阈值</span><input v-model.number="threshold" class="kb-input" type="number" min="0" max="1" step="0.05"></label></div>
				<label><input v-model="rerank" type="checkbox"> 启用 Rerank</label><label><input v-model="parentExpansion" type="checkbox"> 展开父子切片</label>
				<label class="kb-field"><span>Metadata 过滤（JSON）</span><input v-model="metadataText" class="kb-input" placeholder='{"department":"研发"}'></label>
				<button class="kb-button primary" type="submit" :disabled="loading">▶ {{ loading ? '检索中...' : '执行召回' }}</button>
			</form>
			<div>
				<h3 class="kb-subheading">召回结果</h3>
				<div v-if="!matches.length" class="kb-panel kb-empty">暂无结果</div>
				<div v-else class="kb-panel kb-table-wrap"><table class="kb-table" style="min-width:820px"><thead><tr><th style="width:90px">分数</th><th style="width:190px">来源</th><th>内容</th><th style="width:230px">四级 ID</th></tr></thead><tbody><tr v-for="item in matches" :key="item.chunkId"><td>{{ Number(item.score).toFixed(4) }}</td><td>{{ item.sourceId }}<br><span class="kb-badge neutral">{{ item.sourceType }}</span></td><td>{{ item.content }}</td><td>{{ item.sourceId }}<br>{{ item.sourceType }}<br>{{ item.documentId }}<br>{{ item.chunkId }}</td></tr></tbody></table></div>
			</div>
		</div>
	</section>
</template>

<script setup>
import { ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } });
const emit = defineEmits(['error']);
const query = ref(''); const method = ref('hybrid'); const topK = ref(10); const threshold = ref(0.2); const rerank = ref(false); const parentExpansion = ref(true); const metadataText = ref('{}'); const loading = ref(false); const matches = ref([]);
const run = async () => { loading.value = true; try { const result = await props.api.recall({ knowledgeId: props.knowledge.knowledgeId, query: query.value, topK: topK.value, threshold: threshold.value,
	options: { method: method.value, rerank: rerank.value, parentExpansion: parentExpansion.value, metadata: JSON.parse(metadataText.value || '{}') } }); matches.value = (result && result.matches) || []; } catch (error) { emit('error', error); } finally { loading.value = false; } };
</script>
