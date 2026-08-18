<template>
	<main class="kb-shell" data-testid="knowledge-app">
		<header class="kb-appbar">
			<div>
				<h1>知识库</h1>
				<p>管理来源、索引与检索接入</p>
			</div>
			<div class="kb-segmented" aria-label="知识库 Provider">
				<button :class="{ active: mode === 'fw-plus-local' }" @click="switchMode('fw-plus-local')">本地知识库</button>
				<button :class="{ active: mode === 'dify' }" @click="switchMode('dify')">Dify 连接</button>
			</div>
		</header>
		<div v-if="fatalError" class="kb-alert" role="alert">{{ fatalError }}</div>
		<DifyConnection v-if="mode === 'dify'" :api="api" read-only @error="showError" />
		<KnowledgeDetail v-else-if="selected" :api="api" :knowledge="selected" @back="selected = null" @updated="selected = $event" @deleted="selected = null" @error="showError" />
		<KnowledgeList v-else :api="api" @select="selected = $event" @error="showError" />
	</main>
</template>

<script setup>
import { ref } from 'vue';
import { createKnowledgeApi } from './knowledge-api.js';
import DifyConnection from './dify-connection.vue';
import KnowledgeDetail from './knowledge-detail.vue';
import KnowledgeList from './knowledge-list.vue';

const props = defineProps({ options: { type: Object, default: () => ({}) } });
const initialProvider = props.options.provider && props.options.provider.key === 'dify' ? 'dify' : 'fw-plus-local';
const mode = ref(initialProvider);
const selected = ref(null);
const fatalError = ref('');
const api = createKnowledgeApi(props.options);
const switchMode = value => { mode.value = value; selected.value = null; fatalError.value = ''; };
const showError = error => { fatalError.value = error && error.message ? error.message : String(error || '操作失败'); };
</script>

<style>
.kb-shell { min-height: 100%; color: #20242b; background: #f7f8fa; font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; letter-spacing: 0; }
.kb-shell * { box-sizing: border-box; }
.kb-appbar { min-height: 72px; padding: 14px 20px; border-bottom: 1px solid #dfe3e8; background: #fff; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.kb-appbar h1 { margin: 0; font-size: 20px; line-height: 28px; font-weight: 650; letter-spacing: 0; }
.kb-appbar p { margin: 2px 0 0; color: #6b7280; font-size: 13px; }
.kb-segmented { display: inline-flex; border: 1px solid #cfd5dc; border-radius: 6px; background: #f5f6f7; padding: 2px; flex: none; }
.kb-segmented button { min-height: 32px; padding: 0 12px; border: 0; border-radius: 4px; background: transparent; color: #5f6670; cursor: pointer; }
.kb-segmented button.active { background: #fff; color: #176b4d; box-shadow: 0 1px 2px rgba(20, 30, 40, .12); font-weight: 600; }
.kb-page { max-width: 1440px; margin: 0 auto; padding: 20px; }
.kb-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.kb-toolbar h2 { margin: 0; font-size: 17px; line-height: 24px; letter-spacing: 0; }
.kb-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; }
.kb-button { min-height: 34px; padding: 0 12px; border: 1px solid #cbd1d8; border-radius: 6px; background: #fff; color: #2e343b; cursor: pointer; white-space: nowrap; }
.kb-button:hover { border-color: #7e8996; }
.kb-button.primary { border-color: #176b4d; background: #176b4d; color: #fff; }
.kb-button.danger { border-color: #c83d3d; color: #a52f2f; }
.kb-icon-button { width: 34px; height: 34px; padding: 0; display: inline-grid; place-items: center; font-size: 18px; }
.kb-button:disabled { cursor: not-allowed; opacity: .5; }
.kb-alert { margin: 12px 20px 0; padding: 9px 12px; border-left: 3px solid #c83d3d; background: #fff0f0; color: #8c2626; overflow-wrap: anywhere; }
.kb-success { padding: 9px 12px; border-left: 3px solid #27855f; background: #edf8f3; color: #176b4d; overflow-wrap: anywhere; }
.kb-panel { border: 1px solid #dfe3e8; border-radius: 6px; background: #fff; }
.kb-table-wrap { width: 100%; overflow-x: auto; }
.kb-table { width: 100%; min-width: 760px; border-collapse: collapse; table-layout: fixed; }
.kb-table th, .kb-table td { padding: 11px 12px; border-bottom: 1px solid #e7e9ec; text-align: left; vertical-align: top; overflow-wrap: anywhere; }
.kb-table th { background: #f5f6f7; color: #606873; font-size: 12px; font-weight: 600; }
.kb-table tr:last-child td { border-bottom: 0; }
.kb-empty { min-height: 180px; display: grid; place-items: center; padding: 32px; color: #737b85; text-align: center; }
.kb-badge { display: inline-flex; align-items: center; min-height: 24px; padding: 0 8px; border-radius: 999px; background: #edf8f3; color: #176b4d; font-size: 12px; white-space: nowrap; }
.kb-badge.neutral { background: #eef0f2; color: #5d6570; }
.kb-form { display: grid; gap: 14px; }
.kb-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.kb-field { min-width: 0; display: grid; gap: 6px; }
.kb-field > span { color: #4e5661; font-size: 12px; font-weight: 600; }
.kb-input, .kb-select, .kb-textarea { width: 100%; min-height: 36px; border: 1px solid #cbd1d8; border-radius: 5px; background: #fff; padding: 7px 9px; color: #20242b; font: inherit; }
.kb-textarea { min-height: 92px; resize: vertical; }
.kb-input:focus, .kb-select:focus, .kb-textarea:focus { outline: 2px solid rgba(23, 107, 77, .18); border-color: #27855f; }
.kb-modal-backdrop { position: fixed; inset: 0; z-index: 999; background: rgba(18, 22, 28, .46); display: grid; place-items: center; padding: 20px; }
.kb-modal { width: min(620px, 100%); max-height: calc(100vh - 40px); overflow: auto; border-radius: 7px; background: #fff; box-shadow: 0 18px 48px rgba(0,0,0,.24); }
.kb-modal-header, .kb-modal-footer { padding: 14px 18px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.kb-modal-header { border-bottom: 1px solid #e2e5e9; }
.kb-modal-header h3 { margin: 0; font-size: 16px; letter-spacing: 0; }
.kb-modal-body { padding: 18px; }
.kb-modal-footer { border-top: 1px solid #e2e5e9; }
.kb-detail-header { display: flex; align-items: flex-start; gap: 12px; }
.kb-detail-header h2 { margin: 0; font-size: 18px; letter-spacing: 0; overflow-wrap: anywhere; }
.kb-tabs { display: flex; gap: 2px; overflow-x: auto; border-bottom: 1px solid #dfe3e8; background: #fff; padding: 0 12px; }
.kb-tab { min-width: max-content; height: 42px; border: 0; border-bottom: 2px solid transparent; background: transparent; padding: 0 13px; color: #616975; cursor: pointer; }
.kb-tab.active { color: #176b4d; border-bottom-color: #176b4d; font-weight: 600; }
.kb-tab-body { padding: 18px; }
.kb-definition { display: grid; grid-template-columns: 140px minmax(0, 1fr); margin: 0; }
.kb-definition dt, .kb-definition dd { margin: 0; padding: 10px 0; border-bottom: 1px solid #eceef0; overflow-wrap: anywhere; }
.kb-definition dt { color: #68717c; }
.kb-code { margin: 0; padding: 12px; border-radius: 5px; background: #20242b; color: #eef1f4; overflow: auto; font: 12px/1.6 ui-monospace, SFMono-Regular, Menlo, monospace; white-space: pre-wrap; overflow-wrap: anywhere; }
.kb-progress { width: 120px; height: 7px; overflow: hidden; border-radius: 4px; background: #e3e6e9; }
.kb-progress > span { display: block; height: 100%; background: #27855f; }
.kb-split { display: grid; grid-template-columns: minmax(240px, 360px) minmax(0, 1fr); gap: 18px; }
.kb-subheading { margin: 0 0 12px; font-size: 15px; letter-spacing: 0; }
.kb-muted { color: #707984; }
@media (max-width: 720px) {
	.kb-appbar { align-items: flex-start; flex-direction: column; padding: 12px; }
	.kb-segmented { width: 100%; }
	.kb-segmented button { flex: 1; }
	.kb-page { padding: 12px; }
	.kb-toolbar { align-items: flex-start; }
	.kb-form-grid, .kb-split { grid-template-columns: minmax(0, 1fr); }
	.kb-tabs { padding: 0 6px; }
	.kb-tab { padding: 0 11px; }
	.kb-tab-body { padding: 12px; }
	.kb-definition { grid-template-columns: 110px minmax(0, 1fr); }
	.kb-modal-backdrop { padding: 10px; }
}
</style>
