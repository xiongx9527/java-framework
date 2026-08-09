<template>
	<section class="tinyflow-knowledge-workbench">
		<div v-if="loading" class="tinyflow-knowledge-state">{{ $i('tinyflow.knowledge.loading') }}</div>
		<div v-if="error" class="tinyflow-knowledge-error" role="alert">{{ error }}</div>
		<div ref="hostElement" class="tinyflow-knowledge-host" :class="{ hidden: loading || error }"></div>
	</section>
</template>

<script setup>
import { inject, onBeforeUnmount, onMounted, ref } from 'vue';
import { loadTinyflowUi } from '../service/tinyflow-ui-loader.js';
import { getTinyflowRuntime, tinyflowUiBaseUrl } from '../service/tinyflow-service.js';

const $i = inject('i18n.format', key => key);
const hostElement = ref(null);
const loading = ref(true);
const error = ref('');
const runtime = getTinyflowRuntime() || {};
let host = null;
let disposed = false;

onMounted(async () => {
	try {
		const ui = await loadTinyflowUi({ window, document, baseUrl: tinyflowUiBaseUrl(runtime) });
		if (disposed) return;
		host = ui.mountKnowledge({
			element: hostElement.value,
			request: runtime.request,
			baseUrl: tinyflowUiBaseUrl(runtime),
			provider: { key: 'fw-plus-local', readOnly: false }
		});
	} catch (cause) {
		if (!disposed) error.value = cause && cause.message ? cause.message : String(cause);
	} finally {
		if (!disposed) loading.value = false;
	}
});

const destroy = () => {
	if (host && typeof host.destroy === 'function') host.destroy();
	host = null;
};
defineExpose({ beforeClose: () => { destroy(); return true; } });
onBeforeUnmount(() => { disposed = true; destroy(); });
</script>

<style scoped>
.tinyflow-knowledge-workbench { position: relative; width: 100%; height: 100%; min-height: 520px; overflow: auto; background: #f7f8fa; }
.tinyflow-knowledge-host { min-width: 0; min-height: 100%; }
.tinyflow-knowledge-host.hidden { visibility: hidden; }
.tinyflow-knowledge-state, .tinyflow-knowledge-error { position: absolute; inset: 0; z-index: 1; display: grid; place-items: center; padding: 24px; }
.tinyflow-knowledge-error { color: #8c2626; background: #fff0f0; }
@media (max-width: 720px) { .tinyflow-knowledge-workbench { min-height: 640px; } }
</style>
