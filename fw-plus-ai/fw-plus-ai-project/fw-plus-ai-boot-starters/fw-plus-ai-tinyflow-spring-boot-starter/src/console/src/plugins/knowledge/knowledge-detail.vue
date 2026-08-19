<template>
	<section class="kb-page" data-testid="knowledge-detail">
		<div class="kb-toolbar">
			<div class="kb-detail-header">
				<button class="kb-button kb-icon-button" title="返回列表" aria-label="返回列表" @click="$emit('back')">←</button>
				<div><h2>{{ knowledge.name }}</h2><div class="kb-muted">{{ knowledge.knowledgeId }} · {{ knowledge.providerKey }}</div></div>
			</div>
			<span :class="['kb-badge', knowledge.enabled ? '' : 'neutral']">{{ knowledge.enabled ? '运行中' : '已停用' }}</span>
		</div>
		<div class="kb-panel">
			<nav class="kb-tabs" aria-label="知识库详情">
				<button v-for="item in tabs" :key="item.id" :class="['kb-tab', { active: tab === item.id }]" :data-tab="item.id" @click="tab = item.id">{{ item.label }}</button>
			</nav>
			<div class="kb-tab-body">
				<div v-if="tab === 'overview'">
					<dl class="kb-definition">
						<dt>名称</dt><dd>{{ knowledge.name }}</dd><dt>知识库 ID</dt><dd>{{ knowledge.knowledgeId }}</dd>
						<dt>说明</dt><dd>{{ knowledge.description || '-' }}</dd><dt>版本</dt><dd>{{ knowledge.version }}</dd>
						<dt>Embedding</dt><dd>{{ knowledge.metadata && knowledge.metadata.embeddingModelKey }}</dd>
						<dt>向量存储</dt><dd>{{ knowledge.metadata && knowledge.metadata.vectorStoreKey }}</dd>
						<dt>对象存储</dt><dd>{{ knowledge.metadata && knowledge.metadata.s3Key }}</dd>
					</dl>
				</div>
				<DocumentImport v-else-if="tab === 'documents'" :api="api" :knowledge="knowledge" @queued="tab = 'tasks'" @error="$emit('error', $event)" />
				<ChunkManager v-else-if="tab === 'chunks'" :api="api" :knowledge="knowledge" @error="$emit('error', $event)" />
				<RetrievalTest v-else-if="tab === 'retrieval'" :api="api" :knowledge="knowledge" @error="$emit('error', $event)" />
				<ApiAccess v-else-if="tab === 'api'" :api="api" :knowledge="knowledge" @error="$emit('error', $event)" />
				<TaskCenter v-else-if="tab === 'tasks'" :api="api" :knowledge="knowledge" @error="$emit('error', $event)" />
				<KnowledgeSettings v-else :api="api" :knowledge="knowledge" @updated="$emit('updated', $event)" @deleted="$emit('deleted')" @error="$emit('error', $event)" />
			</div>
		</div>
	</section>
</template>

<script setup>
import { ref } from 'vue';
import ApiAccess from './api-access.vue';
import ChunkManager from './chunk-manager.vue';
import DocumentImport from './document-import.vue';
import KnowledgeSettings from './knowledge-settings.vue';
import RetrievalTest from './retrieval-test.vue';
import TaskCenter from './task-center.vue';

defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } });
defineEmits(['back', 'updated', 'deleted', 'error']);
const tab = ref('overview');
const tabs = [
	{ id: 'overview', label: '概览' }, { id: 'documents', label: '文档' }, { id: 'chunks', label: '切片' },
	{ id: 'retrieval', label: '召回测试' }, { id: 'api', label: 'API 接入' }, { id: 'tasks', label: '任务' }, { id: 'settings', label: '设置' }
];
</script>
