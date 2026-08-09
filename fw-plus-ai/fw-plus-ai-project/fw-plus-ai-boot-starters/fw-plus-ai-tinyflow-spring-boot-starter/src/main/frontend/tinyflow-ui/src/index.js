import { Tinyflow } from '@tinyflow-ai/ui';
import '@tinyflow-ai/ui/dist/index.css';
import { createApp } from 'vue';
import { createTinyflowMount } from './tinyflow-host.js';
import KnowledgeApp from './knowledge/knowledge-app.vue';
import { createKnowledgeMount } from './knowledge/knowledge-host.js';

window.FwPlusTinyflowUi = {
	version: '1.3.6',
	mount: createTinyflowMount(Tinyflow),
	mountKnowledge: createKnowledgeMount(createApp, KnowledgeApp)
};
