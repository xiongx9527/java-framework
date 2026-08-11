<template>
	<section data-testid="task-center">
		<div class="kb-toolbar"><h3 class="kb-subheading">导入与索引任务</h3><button class="kb-button kb-icon-button" title="刷新任务" aria-label="刷新任务" @click="load">↻</button></div>
		<div class="kb-panel"><div v-if="loading" class="kb-empty">正在加载任务...</div><div v-else-if="!tasks.length" class="kb-empty">暂无任务</div><div v-else class="kb-table-wrap"><table class="kb-table" style="min-width:920px"><thead><tr><th style="width:180px">任务 ID</th><th style="width:180px">Document ID</th><th style="width:120px">状态</th><th style="width:170px">进度</th><th>错误 / 信息</th><th style="width:150px">操作</th></tr></thead><tbody><tr v-for="item in tasks" :key="item.taskId"><td>{{ item.taskId }}</td><td>{{ item.documentId }}</td><td><span :class="['kb-badge', item.status === 'FAILED' ? 'neutral' : '']">{{ item.status }}</span></td><td><div class="kb-progress"><span :style="{ width: `${progress(item)}%` }"></span></div><small>{{ progress(item) }}%</small></td><td>{{ item.message || '-' }}</td><td><div class="kb-actions"><button v-if="item.status === 'FAILED'" class="kb-button" data-action="retry" @click="retry(item)">↻ 重试</button><button v-if="['PENDING','RUNNING'].includes(item.status)" class="kb-button danger" data-action="cancel" @click="cancel(item)">× 取消</button></div></td></tr></tbody></table></div></div>
	</section>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue';
const props = defineProps({ api: { type: Object, required: true }, knowledge: { type: Object, required: true } }); const emit = defineEmits(['error']);
const tasks = ref([]); const loading = ref(false); let timer;
const progress = item => Math.max(0, Math.min(100, Number(item.metadata && item.metadata.progress !== undefined ? item.metadata.progress : item.status === 'COMPLETED' ? 100 : item.status === 'RUNNING' ? 50 : 0)));
const load = async () => { loading.value = true; try { const page = await props.api.listTasks(props.knowledge.knowledgeId); tasks.value = (page && page.items) || []; } catch (error) { emit('error', error); } finally { loading.value = false; } };
const retry = async item => { try { await props.api.retryTask(props.knowledge.knowledgeId, item.taskId); await load(); } catch (error) { emit('error', error); } };
const cancel = async item => { try { await props.api.cancelTask(props.knowledge.knowledgeId, item.taskId); await load(); } catch (error) { emit('error', error); } };
onMounted(() => { load(); timer = setInterval(load, 3000); }); onBeforeUnmount(() => clearInterval(timer));
</script>
