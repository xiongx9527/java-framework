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
import { createTinyflowService, createToolbarResourceBinding, createWorkbench, createWorkbenchMountLifecycle, getTinyflowRuntime, tinyflowUiBaseUrl } from '../service/tinyflow-service.js';

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
const lifecycle = createWorkbenchMountLifecycle(workbench);
let disposed = false;
let pendingOperations = 0;
const refresh = () => { resource.value = workbench.getResource(); };
const failure = cause => { error.value = cause && cause.message ? cause.message : String(cause); };
const beginOperation = () => {
	pendingOperations += 1;
	if (!disposed) {
		loading.value = true;
		error.value = '';
	}
};
const finishOperation = () => {
	pendingOperations = Math.max(0, pendingOperations - 1);
	if (!disposed) loading.value = pendingOperations > 0;
};

const binding = createToolbarResourceBinding(info, workbench, opened);
const injectedValue = value => value && typeof value === 'object' && 'value' in value ? value.value : value;
const syncResource = async () => {
	beginOperation();
	try {
		if (!disposed && await binding.sync()) {
			refresh();
			dirty.value = workbench.isDirty();
		}
	} catch (cause) {
		if (!disposed) failure(cause);
	} finally {
		finishOperation();
	}
};

const load = async id => {
	beginOperation();
	try {
		if (!disposed && await workbench.load(id)) {
			refresh();
			dirty.value = workbench.isDirty();
		}
	} catch (cause) {
		if (!disposed) failure(cause);
	} finally {
		finishOperation();
	}
};

const save = async () => {
	beginOperation();
	try {
		if (!disposed) {
			await workbench.save();
			refresh();
			dirty.value = false;
			result.value = $i('tinyflow.saveSuccess');
		}
	} catch (cause) {
		if (!disposed) failure(cause);
	} finally {
		finishOperation();
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
	beginOperation();
	try {
		await lifecycle.mount(
			() => loadTinyflowUi({ window, document, baseUrl: tinyflowUiBaseUrl(runtime) }),
			() => ({
				element: canvas.value,
				data: {},
				onDataChange: data => {
					if (disposed) return;
					workbench.onDataChange(data);
					dirty.value = true;
				}
			})
		);
	} catch (cause) {
		if (!disposed) failure(cause);
	} finally {
		finishOperation();
	}
});

watch(() => [injectedValue(opened), injectedValue(info)], syncResource, { immediate: true });
defineExpose({ beforeClose: () => workbench.close() });
onBeforeUnmount(() => {
	disposed = true;
	lifecycle.dispose();
});
</script>
