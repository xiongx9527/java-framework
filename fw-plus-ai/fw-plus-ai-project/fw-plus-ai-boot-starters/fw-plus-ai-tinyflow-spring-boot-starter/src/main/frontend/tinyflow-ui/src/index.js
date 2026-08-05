import { Tinyflow } from '@tinyflow-ai/ui';
import '@tinyflow-ai/ui/dist/index.css';
import { createTinyflowMount } from './tinyflow-host.js';

window.FwPlusTinyflowUi = {
	mount: createTinyflowMount(Tinyflow)
};
