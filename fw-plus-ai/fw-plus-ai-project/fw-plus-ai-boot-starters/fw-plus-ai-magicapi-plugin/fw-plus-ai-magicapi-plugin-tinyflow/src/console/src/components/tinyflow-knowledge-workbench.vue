<template>
	<section class="tinyflow-knowledge-empty">
		<h3>{{ $i('tinyflow.knowledge.title') }}</h3>
		<p>{{ $i('tinyflow.emptyKnowledge') }}</p>
	</section>
</template>

<script setup>
import { inject } from 'vue';
const $i = inject('i18n.format', key => key);
</script>
