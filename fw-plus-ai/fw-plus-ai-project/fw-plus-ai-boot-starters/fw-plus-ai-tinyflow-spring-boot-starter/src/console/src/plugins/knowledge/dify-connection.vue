<template>
	<section class="kb-page" data-testid="dify-connection">
		<div class="kb-toolbar"><div><h2>Dify 连接</h2><div class="kb-muted">只管理远程 Dataset 引用，不提供上传、重建或删除文档。</div></div><span class="kb-badge neutral">只读 Provider</span></div>
		<div class="kb-panel kb-tab-body">
			<form class="kb-form" @submit.prevent="save">
				<div class="kb-form-grid"><label class="kb-field"><span>知识库 ID</span><input v-model.trim="draft.knowledgeId" class="kb-input" required @change="load"></label><label class="kb-field"><span>连接 ID</span><input v-model.trim="draft.connectionId" class="kb-input" placeholder="dify-main" required></label></div>
				<div class="kb-form-grid"><label class="kb-field"><span>Base URL</span><input v-model.trim="draft.baseUrl" class="kb-input" type="url" placeholder="https://api.dify.ai/v1" required></label><label class="kb-field"><span>Credential Ref</span><input v-model.trim="draft.credentialRef" class="kb-input" placeholder="vault://dify/main" required></label></div>
				<label class="kb-field"><span>Dataset ID</span><input v-model.trim="draft.datasetId" class="kb-input" required></label>
				<div v-if="message" class="kb-success" role="status">{{ message }}</div>
				<div class="kb-actions"><button class="kb-button" data-action="test-connection" type="button" :disabled="!draft.connectionId" @click="testConnection">▶ 测试连通性</button><button class="kb-button primary" data-action="save-connection" type="submit">保存连接</button></div>
			</form>
		</div>
		<div v-if="connections.length" class="kb-panel" style="margin-top:16px"><div class="kb-table-wrap"><table class="kb-table"><thead><tr><th>连接 ID</th><th>Base URL</th><th>Credential Ref</th><th>Dataset ID</th><th style="width:100px">操作</th></tr></thead><tbody><tr v-for="item in connections" :key="item.connectionId"><td>{{ item.connectionId }}</td><td>{{ item.baseUrl }}</td><td>{{ item.credentialRef }}</td><td>{{ item.datasetId }}</td><td><button class="kb-button" @click="edit(item)">编辑</button></td></tr></tbody></table></div></div>
	</section>
</template>

<script setup>
import { reactive, ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, readOnly: Boolean }); const emit = defineEmits(['error']);
const connections = ref([]); const message = ref(''); const draft = reactive({ knowledgeId: '', connectionId: '', baseUrl: '', credentialRef: '', datasetId: '' });
const load = async () => { if (!draft.knowledgeId) return; try { connections.value = await props.api.listDify(draft.knowledgeId) || []; } catch (error) { emit('error', error); } };
const edit = item => Object.assign(draft, item);
const save = async () => { try { await props.api.saveDify({ ...draft }); message.value = 'Dify 连接已保存'; await load(); } catch (error) { emit('error', error); } };
const testConnection = async () => { try { await props.api.testDify(draft.connectionId); message.value = '连通性测试通过'; } catch (error) { emit('error', error); } };
</script>
