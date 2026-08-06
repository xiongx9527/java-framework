<template>
	<header class="tinyflow-toolbar">
		<label><span>{{ $i('tinyflow.resourceId') }}</span><input v-model.trim="resourceId" @keyup.enter="$emit('load', resourceId)" /></label>
		<button type="button" :disabled="loading || !resourceId" @click="$emit('load', resourceId)">{{ $i('tinyflow.load') }}</button>
		<button type="button" :disabled="loading" @click="$emit('save')">{{ $i('tinyflow.save') }}</button>
		<button type="button" :disabled="loading || dirty" @click="$emit('run')">{{ $i('tinyflow.run') }}</button>
	</header>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({ resource: { type: Object, default: null }, dirty: Boolean, loading: Boolean });
defineEmits(['load', 'save', 'run']);
const resourceId = computed({
	get: () => props.resource && props.resource.id ? props.resource.id : '',
	set: value => {
		if (props.resource) props.resource.id = value;
	}
});
</script>
