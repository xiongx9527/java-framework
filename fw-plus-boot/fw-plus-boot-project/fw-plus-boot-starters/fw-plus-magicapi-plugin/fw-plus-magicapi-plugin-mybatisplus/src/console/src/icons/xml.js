export default function installMybatisPlusXmlIcon() {
    if (typeof document === 'undefined' || document.getElementById('magic-mybatisplus-xml-symbols')) {
        return
    }
    if (!document.createElementNS || !document.body) {
        return
    }
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    svg.setAttribute('id', 'magic-mybatisplus-xml-symbols')
    svg.setAttribute('style', 'display:none')
    svg.innerHTML = '<symbol id="magic-mybatisplus-xml" viewBox="0 0 1024 1024"><path fill="#2f80ed" d="M160 96h512l192 192v640H160z"/><path fill="#fff" d="M640 128v192h192zM272 448h96l64 96 64-96h96L480 608l120 176h-96l-72-112-72 112h-96l120-176z"/></symbol>'
    document.body.appendChild(svg)
}
