const UI_VERSION = '1.3.6';
const UI_PATH = `/fw-plus/tinyflow-ui/${UI_VERSION}`;

const trimTrailingSlash = value => (value || '').replace(/\/+$/, '');
const resourceUrl = (baseUrl, file) => `${trimTrailingSlash(baseUrl)}${UI_PATH}/${file}`;

const verifyUi = ui => {
	if (!ui || typeof ui.mount !== 'function') {
		throw new Error('Tinyflow UI 加载完成但缺少 FwPlusTinyflowUi.mount 构造器');
	}
	if (ui.version && ui.version !== UI_VERSION) {
		throw new Error(`Tinyflow UI 版本不匹配：需要 ${UI_VERSION}，实际 ${ui.version}`);
	}
	if (typeof ui.mountKnowledge !== 'function') {
		throw new Error('Tinyflow UI 加载完成但缺少 FwPlusTinyflowUi.mountKnowledge 构造器');
	}
	return ui;
};

const appendStylesheet = (document, url) => new Promise((resolve, reject) => {
	const link = document.createElement('link');
	link.rel = 'stylesheet';
	link.href = url;
	link.onload = resolve;
	link.onerror = () => reject(new Error(`加载 Tinyflow UI 样式失败：${url}`));
	document.head.appendChild(link);
});

const appendScript = (document, url) => new Promise((resolve, reject) => {
	const script = document.createElement('script');
	script.async = true;
	script.src = url;
	script.onload = resolve;
	script.onerror = () => reject(new Error(`加载 Tinyflow UI 失败：${url}`));
	document.head.appendChild(script);
});

export async function loadTinyflowUi(options = {}) {
	const browserWindow = options.window || (typeof window === 'undefined' ? undefined : window);
	const document = options.document || (browserWindow && browserWindow.document);
	if (!browserWindow) {
		throw new Error('Tinyflow UI 加载失败：浏览器 window 不可用');
	}
	if (browserWindow.FwPlusTinyflowUi) {
		return verifyUi(browserWindow.FwPlusTinyflowUi);
	}
	if (!document || !document.head || typeof document.createElement !== 'function') {
		throw new Error('Tinyflow UI 加载失败：缺少 FwPlusTinyflowUi 构造器且无法加载静态资源');
	}
	await appendStylesheet(document, resourceUrl(options.baseUrl, 'tinyflow-ui.css'));
	await appendScript(document, resourceUrl(options.baseUrl, 'tinyflow-ui.js'));
	return verifyUi(browserWindow.FwPlusTinyflowUi);
}

export { UI_PATH, UI_VERSION };
