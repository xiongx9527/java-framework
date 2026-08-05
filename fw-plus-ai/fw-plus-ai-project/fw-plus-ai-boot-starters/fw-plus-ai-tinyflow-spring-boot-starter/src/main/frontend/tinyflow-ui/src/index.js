import { Tinyflow } from '@tinyflow-ai/ui';
import '@tinyflow-ai/ui/dist/index.css';
import { createTinyflowHost } from './tinyflow-host.js';

window.FwPlusTinyflowUi = {
	mount(options) {
		return createTinyflowHost({ Tinyflow, ...options });
	}
};
