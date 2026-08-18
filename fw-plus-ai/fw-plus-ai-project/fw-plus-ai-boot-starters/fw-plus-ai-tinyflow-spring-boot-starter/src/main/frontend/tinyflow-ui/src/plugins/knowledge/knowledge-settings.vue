<template>
	<section data-testid="knowledge-settings">
		<form class="kb-form" @submit.prevent="save">
			<h3 class="kb-subheading">知识库设置</h3>
			<div class="kb-form-grid"><label class="kb-field"><span>名称</span><input v-model.trim="draft.name" class="kb-input" required></label><label class="kb-field"><span>状态</span><select v-model="draft.enabled" class="kb-select"><option :value="true">启用</option><option :value="false">停用</option></select></label></div>
			<label class="kb-field"><span>说明</span><textarea v-model="draft.description" class="kb-textarea"></textarea></label>
			<div class="kb-form-grid">
				<label class="kb-field"><span>Embedding 模型（现有资源）</span><select v-model="draft.embeddingModelKey" class="kb-select"><option v-for="item in models" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
				<label class="kb-field"><span>向量存储（现有资源）</span><select v-model="draft.vectorStoreKey" class="kb-select"><option v-for="item in vectors" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
				<label class="kb-field"><span>对象存储（现有资源）</span><select v-model="draft.s3Key" class="kb-select"><option v-for="item in stores" :key="item.key" :value="item.key">{{ item.name || item.key }}</option></select></label>
			</div>
			<div v-if="message" class="kb-success">{{ message }}</div>
			<div class="kb-actions"><button class="kb-button primary" type="submit">保存设置</button></div>
		</form>
		<hr style="margin:24px 0;border:0;border-top:1px solid #e2e5e9">
		<div class="kb-toolbar"><div><h3 class="kb-subheading">删除知识库</h3><div class="kb-muted">删除会清理关联文档、切片和可定位的向量记录。</div></div><button class="kb-button danger" data-action="delete" @click="remove">删除</button></div>
	</section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } }); const emit = defineEmits(['updated', 'deleted', 'error']);
const models = ref([]); const vectors = ref([]); const stores = ref([]); const message = ref('');
const draft = reactive({ name: props.knowledge.name, description: props.knowledge.description || '', enabled: props.knowledge.enabled,
	embeddingModelKey: props.knowledge.metadata && props.knowledge.metadata.embeddingModelKey, vectorStoreKey: props.knowledge.metadata && props.knowledge.metadata.vectorStoreKey, s3Key: props.knowledge.metadata && props.knowledge.metadata.s3Key });
onMounted(async () => { try { [models.value, vectors.value, stores.value] = await Promise.all([props.api.models(), props.api.vectors(), props.api.s3()]); } catch (error) { emit('error', error); } });
const save = async () => { try { const updated = await props.api.updateKnowledge(props.knowledge.knowledgeId, { knowledgeId: props.knowledge.knowledgeId, name: draft.name, description: draft.description, enabled: draft.enabled, expectedVersion: props.knowledge.version,
	metadata: { embeddingModelKey: draft.embeddingModelKey, vectorStoreKey: draft.vectorStoreKey, s3Key: draft.s3Key } }); message.value = '设置已保存'; emit('updated', updated); } catch (error) { emit('error', error); } };
const remove = async () => { if (!window.confirm(`确认删除知识库“${props.knowledge.name}”？`)) return; try { await props.api.deleteKnowledge(props.knowledge.knowledgeId, props.knowledge.version); emit('deleted'); } catch (error) { emit('error', error); } };
</script>
