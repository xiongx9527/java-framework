<template>
	<section data-testid="api-access">
		<div class="kb-toolbar"><h3 class="kb-subheading">External Knowledge API</h3><button class="kb-button primary" data-action="create-key" @click="createKey">+ 创建 API Key</button></div>
		<pre class="kb-code">POST /external-knowledge/retrieval
Authorization: Bearer &lt;API_KEY&gt;
Content-Type: application/json

{"knowledge_id":"{{ knowledge.knowledgeId }}","query":"...","retrieval_setting":{"top_k":10,"score_threshold":0.2}}</pre>
		<div class="kb-panel" style="margin-top:16px"><div v-if="!keys.length" class="kb-empty">暂无 API Key</div><div v-else class="kb-table-wrap"><table class="kb-table"><thead><tr><th>Key ID</th><th>前缀</th><th>状态</th><th style="width:110px">操作</th></tr></thead><tbody><tr v-for="item in keys" :key="item.accessKeyId"><td>{{ item.accessKeyId }}</td><td>{{ item.prefix }}</td><td>{{ item.enabled ? '有效' : '已撤销' }}</td><td><button class="kb-button danger" @click="revoke(item)">撤销</button></td></tr></tbody></table></div></div>
		<div v-if="createdToken" class="kb-modal-backdrop" @click.self="closeToken">
			<section class="kb-modal" role="dialog" aria-modal="true" aria-label="API Key 创建成功" data-testid="api-key-once"><header class="kb-modal-header"><h3>API Key 已创建</h3></header><div class="kb-modal-body kb-form"><div class="kb-alert">明文仅显示这一次，关闭后无法再次查看。</div><pre class="kb-code">{{ createdToken }}</pre></div><footer class="kb-modal-footer"><span></span><button class="kb-button primary" @click="closeToken">我已妥善保存</button></footer></section>
		</div>
	</section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } }); const emit = defineEmits(['error']);
const keys = ref([]); const createdToken = ref('');
const load = async () => { try { keys.value = await props.api.listKeys(props.knowledge.knowledgeId) || []; } catch (error) { emit('error', error); } };
const createKey = async () => { try { const result = await props.api.createKey(props.knowledge.knowledgeId); createdToken.value = result.token; await load(); } catch (error) { emit('error', error); } };
const closeToken = () => { createdToken.value = ''; };
const revoke = async item => { try { await props.api.revokeKey(props.knowledge.knowledgeId, item.accessKeyId); await load(); } catch (error) { emit('error', error); } };
onMounted(load);
</script>
