<template>
	<section class="kb-page" data-testid="knowledge-list">
		<div class="kb-toolbar">
			<h2>知识库列表</h2>
			<div class="kb-actions">
				<button class="kb-button kb-icon-button" title="刷新列表" aria-label="刷新列表" @click="load">↻</button>
				<button class="kb-button primary" data-action="create" @click="openCreate">+ 新建知识库</button>
			</div>
		</div>
		<div class="kb-panel">
			<div v-if="loading" class="kb-empty">正在加载...</div>
			<div v-else-if="!items.length" class="kb-empty">暂无知识库</div>
			<div v-else class="kb-table-wrap">
				<table class="kb-table">
					<thead><tr><th style="width:26%">名称</th><th style="width:22%">ID</th><th style="width:16%">Provider</th><th>说明</th><th style="width:90px">操作</th></tr></thead>
					<tbody>
						<tr v-for="item in items" :key="item.knowledgeId">
							<td><strong>{{ item.name }}</strong><br><span :class="['kb-badge', item.enabled ? '' : 'neutral']">{{ item.enabled ? '已启用' : '已停用' }}</span></td>
							<td>{{ item.knowledgeId }}</td><td>{{ item.providerKey }}</td><td>{{ item.description || '-' }}</td>
							<td><button class="kb-button" @click="$emit('select', item)">管理</button></td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>
		<div v-if="creating" class="kb-modal-backdrop" @click.self="creating = false">
			<section class="kb-modal" role="dialog" aria-modal="true" aria-label="新建知识库" data-testid="knowledge-create-dialog">
				<header class="kb-modal-header"><h3>新建本地知识库</h3><button class="kb-button kb-icon-button" title="关闭" aria-label="关闭" @click="creating = false">×</button></header>
				<div class="kb-modal-body kb-form">
					<div class="kb-form-grid">
						<label class="kb-field"><span>名称</span><input v-model.trim="draft.name" class="kb-input" required></label>
						<label class="kb-field"><span>知识库 ID</span><input v-model.trim="draft.knowledgeId" class="kb-input" placeholder="knowledge-001" required></label>
					</div>
					<label class="kb-field"><span>说明</span><textarea v-model="draft.description" class="kb-textarea"></textarea></label>
					<div class="kb-form-grid">
						<label class="kb-field"><span>Embedding 模型</span><select v-model="draft.embeddingModelKey" class="kb-select"><option v-for="item in models" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
						<label class="kb-field"><span>向量存储</span><select v-model="draft.vectorStoreKey" class="kb-select"><option v-for="item in vectors" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
						<label class="kb-field"><span>对象存储</span><select v-model="draft.s3Key" class="kb-select"><option v-for="item in stores" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
						<label class="kb-field"><span>状态</span><select v-model="draft.enabled" class="kb-select"><option :value="true">启用</option><option :value="false">停用</option></select></label>
					</div>
					<div v-if="formError" class="kb-alert" role="alert">{{ formError }}</div>
				</div>
				<footer class="kb-modal-footer"><button class="kb-button" @click="creating = false">取消</button><button class="kb-button primary" :disabled="saving" @click="create">{{ saving ? '创建中...' : '创建' }}</button></footer>
			</section>
		</div>
	</section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true } });
const emit = defineEmits(['select', 'error']);
const items = ref([]); const loading = ref(false); const creating = ref(false); const saving = ref(false); const formError = ref('');
const models = ref([]); const vectors = ref([]); const stores = ref([]);
const draft = reactive({ name: '', knowledgeId: '', description: '', embeddingModelKey: '', vectorStoreKey: '', s3Key: '', enabled: true });
const load = async () => { loading.value = true; try { const page = await props.api.listKnowledge(); items.value = (page && page.items) || []; } catch (error) { emit('error', error); } finally { loading.value = false; } };
const openCreate = async () => {
	creating.value = true; formError.value = '';
	try {
		[models.value, vectors.value, stores.value] = await Promise.all([props.api.models(), props.api.vectors(), props.api.s3()]);
		draft.embeddingModelKey = models.value[0] && models.value[0].key;
		draft.vectorStoreKey = vectors.value[0] && vectors.value[0].key;
		draft.s3Key = stores.value[0] && stores.value[0].key;
	} catch (error) { formError.value = error.message; }
};
const create = async () => {
	if (!draft.name || !draft.knowledgeId || !draft.embeddingModelKey || !draft.vectorStoreKey || !draft.s3Key) { formError.value = '请完整填写名称、ID 与资源选择'; return; }
	saving.value = true; formError.value = '';
	try {
		const created = await props.api.createKnowledge({ knowledgeId: draft.knowledgeId, name: draft.name, description: draft.description, enabled: draft.enabled,
			metadata: { embeddingModelKey: draft.embeddingModelKey, vectorStoreKey: draft.vectorStoreKey, s3Key: draft.s3Key } });
		creating.value = false; await load(); emit('select', created);
	} catch (error) { formError.value = error.message; } finally { saving.value = false; }
};
onMounted(load);
</script>
