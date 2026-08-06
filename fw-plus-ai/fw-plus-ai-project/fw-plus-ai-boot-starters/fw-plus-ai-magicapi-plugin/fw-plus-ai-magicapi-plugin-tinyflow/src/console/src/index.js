import localZhCN from './i18n/zh-cn.js';
import localEn from './i18n/en.js';
import TinyflowWorkbench from './components/tinyflow-workbench.vue';
import TinyflowKnowledgeWorkbench from './components/tinyflow-knowledge-workbench.vue';
import MagicTinyflowResource from './service/magic-tinyflow-resource.js';
import { setTinyflowRuntime } from './service/tinyflow-service.js';

if (typeof window !== 'undefined') {
	const svgBody = '<symbol viewBox="0 0 24 24" id="magic-tinyflow-flow"><path d="M5 4h5v4H5V4Zm9 12h5v4h-5v-4ZM9 6h4a3 3 0 0 1 3 3v3h-2V9a1 1 0 0 0-1-1H9V6Zm-2 8h3v4H5v-4h2Zm3 2h4v2h-4v-2Z"/></symbol><symbol viewBox="0 0 24 24" id="magic-tinyflow-knowledge"><path d="M4 5a3 3 0 0 1 3-3h11v18H7a3 3 0 0 0-3 3V5Zm3-1a1 1 0 0 0-1 1v14.17c.31-.11.64-.17 1-.17h9V4H7Z"/></symbol>';
	const svgDom = document.getElementById('__svg__icons__dom__');
	if (svgDom) svgDom.innerHTML += svgBody;
	else {
		const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
		svg.style.position = 'absolute';
		svg.style.width = '0';
		svg.style.height = '0';
		svg.id = '__svg__icons__dom__';
		svg.innerHTML = svgBody;
		document.body.insertBefore(svg, document.body.firstChild);
	}
}

export default opt => {
	const i18n = opt.i18n;
	i18n.add('zh-cn', localZhCN);
	i18n.add('en', localEn);
	setTinyflowRuntime(opt);
	return {
		name: 'tinyflow',
		resource: [{
			type: 'tinyflow',
			title: 'tinyflow.resource.name',
			icon: '#magic-tinyflow-flow',
			service: MagicTinyflowResource(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
		}],
		toolbars: [
			{ type: 'tinyflow', title: 'tinyflow.workflow.title', icon: '#magic-tinyflow-flow', order: 600, component: TinyflowWorkbench, allowClose: true,
				beforeClose: component => !component || typeof component.beforeClose !== 'function' || component.beforeClose() },
			{ id: 'tinyflow-knowledge', title: 'tinyflow.knowledge.title', icon: '#magic-tinyflow-knowledge',
				order: 610, component: TinyflowKnowledgeWorkbench, adminOnly: true, allowClose: true, fullscreen: true }
		]
	};
};
