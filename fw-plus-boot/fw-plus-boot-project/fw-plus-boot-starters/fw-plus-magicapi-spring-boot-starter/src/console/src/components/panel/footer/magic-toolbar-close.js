const closingToolbars = new WeakSet();

export async function requestToolbarClose(toolbar, instance, remove) {
	if (!toolbar || typeof remove !== 'function' || closingToolbars.has(toolbar)) return false;
	closingToolbars.add(toolbar);
	try {
		if (typeof toolbar.beforeClose === 'function' && await toolbar.beforeClose(instance) === false) return false;
		remove();
		return true;
	} catch (error) {
		return false;
	} finally {
		closingToolbars.delete(toolbar);
	}
}
