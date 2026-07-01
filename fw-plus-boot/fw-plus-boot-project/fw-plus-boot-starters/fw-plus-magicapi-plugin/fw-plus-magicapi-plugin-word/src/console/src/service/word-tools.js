// 设计器工具栏按钮配置，visible 默认 true，可按需关闭
export const toolButtons = [
    { type: '', icon: 'paragraph', i18nKey: 'word.designer.paragraph', visible: true },
    { type: 'title', icon: 'title', i18nKey: 'word.designer.title', visible: true },
    { type: 'table', icon: 'table', i18nKey: 'word.designer.table', visible: true },
    { type: 'image', icon: 'image', i18nKey: 'word.designer.image', visible: true },
    { type: 'chart', icon: 'chart', i18nKey: 'word.designer.chart', visible: true },
    { type: 'barcode', icon: 'barcode', i18nKey: 'word.designer.barcode', visible: true },
    { type: 'qrcode', icon: 'qrcode', i18nKey: 'word.designer.qrcode', visible: true },
    { type: 'list', icon: 'list', i18nKey: 'word.designer.list', visible: true },
    { type: 'separator', icon: 'separator', i18nKey: 'word.designer.separator', visible: true },
    { type: 'hyperlink', icon: 'link', i18nKey: 'word.designer.hyperlink', visible: true },
    { type: 'pageBreak', icon: 'page-break', i18nKey: 'word.designer.pageBreak', visible: true },
    { type: 'watermark', icon: 'watermark', i18nKey: 'word.designer.watermark', visible: true },
    { type: 'pageNumber', icon: 'page-number', i18nKey: 'word.designer.pageNumber', visible: true },
    { type: 'sectionBreak', icon: 'section-break', i18nKey: 'word.designer.sectionBreak', visible: true },
    { type: 'toc', icon: 'toc', i18nKey: 'word.designer.toc', visible: true },
    { type: 'shape', icon: 'shape', i18nKey: 'word.designer.shape', visible: true },
    { type: 'codeBlock', icon: 'code', i18nKey: 'word.designer.codeBlock', visible: true },
    { type: 'footnote', icon: 'footnote', i18nKey: 'word.designer.footnote', visible: true },
]

export function visibleButtons() {
    return toolButtons.filter(b => b.visible !== false)
}
