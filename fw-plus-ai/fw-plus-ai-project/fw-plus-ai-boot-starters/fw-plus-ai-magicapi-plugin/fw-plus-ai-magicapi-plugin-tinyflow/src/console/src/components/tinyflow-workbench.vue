<template>
	<section class="tinyflow-workbench">
		<TinyflowToolbar :resource="resource" :dirty="dirty" :loading="loading" @load="load" @save="save" @run="run" />
		<div v-if="error" class="tinyflow-error" role="alert">{{ error }}</div>
		<div v-if="loading" class="tinyflow-state">{{ $i('tinyflow.loading') }}</div>
		<div ref="canvas" class="tinyflow-canvas" :class="{ hidden: loading || error }"></div>
		<pre v-if="result" class="tinyflow-result">{{ result }}</pre>
	</section>
</template>

<script setup>
import { inject, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import TinyflowToolbar from './tinyflow-toolbar.vue';
import { loadTinyflowUi } from '../service/tinyflow-ui-loader.js';
import { createTinyflowService, createToolbarResourceBinding, createWorkbench, getTinyflowRuntime, tinyflowUiBaseUrl } from '../service/tinyflow-service.js';

const $i = inject('i18n.format', key => key);
const info = inject('info', ref(null));
const opened = inject('opened', ref(null));
const canvas = ref(null);
const loading = ref(false);
const error = ref('');
const resource = ref(null);
const result = ref('');
const dirty = ref(false);
const runtime = getTinyflowRuntime() || {};
const service = createTinyflowService(runtime.request);
const workbench = createWorkbench({
	load: id => service.get(id),
	save: async payload => { await service.validate(payload); return service.save(payload); },
	run: payload => service.run(payload),
	confirmDiscard: async () => window.confirm($i('tinyflow.unsaved'))
});
const refresh = () => { resource.value = workbench.getResource(); };
const failure = cause => { error.value = cause && cause.message ? cause.message : String(cause); };

const binding = createToolbarResourceBinding(info, workbench, opened);
const injectedValue = value => value && typeof value === 'object' && 'value' in value ? value.value : value;
const syncResource = async () => {
	loading.value = true;
	error.value = '';
	try {
		if (await binding.sync()) {
			refresh();
			dirty.value = workbench.isDirty();
		}
	} catch (cause) {
		failure(cause);
	} finally {
		loading.value = false;
	}
};

const load = async id => {
	loading.value = true;
	error.value = '';
	try {
		if (await workbench.load(id)) {
			refresh();
			dirty.value = workbench.isDirty();
		}
	} catch (cause) {
		failure(cause);
	} finally {
		loading.value = false;
	}
};

const save = async () => {
	loading.value = true;
	error.value = '';
	try {
		await workbench.save();
		refresh();
		dirty.value = false;
		result.value = $i('tinyflow.saveSuccess');
	} catch (cause) {
		failure(cause);
	} finally {
		loading.value = false;
	}
};

const run = async () => {
	error.value = '';
	try {
		result.value = JSON.stringify(await workbench.run(), null, 2);
	} catch (cause) {
		failure(cause);
	}
};

onMounted(async () => {
	loading.value = true;
	try {
		const ui = await loadTinyflowUi({ window, document, baseUrl: tinyflowUiBaseUrl(runtime) });
		workbench.attach(ui.mount({
			element: canvas.value,
			data: {},
			onDataChange: data => { workbench.onDataChange(data); dirty.value = true; }
		}));
	} catch (cause) {
		failure(cause);
	} finally {
		loading.value = false;
	}
});

watch(() => [injectedValue(opened), injectedValue(info)], syncResource, { immediate: true });
defineExpose({ beforeClose: () => workbench.close() });
onBeforeUnmount(() => { workbench.destroy(); });
</script>
