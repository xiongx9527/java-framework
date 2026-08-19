import { Tinyflow } from '@tinyflow-ai/ui';
import '@tinyflow-ai/ui/dist/index.css';
import { createApp } from 'vue';
import { createTinyflowMount } from './components/tinyflow-host.js';
import KnowledgeApp from './plugins/knowledge/knowledge-app.vue';
import { createKnowledgeMount } from './plugins/knowledge/knowledge-host.js';

window.FwPlusTinyflowUi = {
	version: '1.3.6',
	mount: createTinyflowMount(Tinyflow),
	mountKnowledge: createKnowledgeMount(createApp, KnowledgeApp)
};
