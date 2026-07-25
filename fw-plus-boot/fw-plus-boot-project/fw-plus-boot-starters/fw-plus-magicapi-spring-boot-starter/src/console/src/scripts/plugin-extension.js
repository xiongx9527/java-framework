export const stableSortByOrder = items => items
    .map((item, index) => ({ item, index, order: Number.isFinite(item.order) ? item.order : null }))
    .sort((a, b) => {
        if (a.order === null && b.order === null) return a.index - b.index
        if (a.order === null) return 1
        if (b.order === null) return -1
        return a.order - b.order || a.index - b.index
    })
    .map(entry => entry.item)

export const isValidStatusBarItem = item => !!item
    && typeof item.icon === 'string' && item.icon.length > 0
    && typeof item.title === 'string' && item.title.length > 0
    && typeof item.displayKey === 'string' && item.displayKey.length > 0
    && typeof item.onClick === 'function'
