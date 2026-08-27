const closingToolbars = new WeakSet();

export function removeToolbarByIdentity(toolbars, toolbar) {
	const index = toolbars.indexOf(toolbar);
	if (index < 0) return false;
	toolbars.splice(index, 1);
	return true;
}

export async function requestToolbarClose(toolbar, instance, remove) {
	if (!toolbar || typeof remove !== 'function' || closingToolbars.has(toolbar)) return false;
	closingToolbars.add(toolbar);
	try {
		if (typeof toolbar.beforeClose === 'function' && await toolbar.beforeClose(instance) === false) return false;
		return remove() !== false;
	} catch (error) {
		return false;
	} finally {
		closingToolbars.delete(toolbar);
	}
}
