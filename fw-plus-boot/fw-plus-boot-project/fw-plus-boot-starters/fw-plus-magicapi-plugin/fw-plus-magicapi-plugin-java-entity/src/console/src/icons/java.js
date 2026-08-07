export default function installJavaEntityIcon() {
    if (typeof document === 'undefined' || document.getElementById('magic-java-entity-symbols')) {
        return
    }
    if (!document.createElementNS || !document.body) {
        return
    }
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    svg.setAttribute('id', 'magic-java-entity-symbols')
    svg.setAttribute('style', 'display:none')
    svg.innerHTML = '<symbol id="magic-java-entity-java" viewBox="0 0 1024 1024"><path fill="#d73a49" d="M512 96c229.75 0 416 186.25 416 416S741.75 928 512 928 96 741.75 96 512 282.25 96 512 96z"/><path fill="#fff" d="M337.92 700.16c-42.24 0-75.52-12.16-101.12-36.48l42.24-49.92c16.64 15.36 33.28 23.04 53.76 23.04 24.32 0 38.4-14.08 38.4-47.36V324.48h75.52v268.8c0 35.84-10.24 62.72-28.16 80.64-18.56 17.92-46.08 26.24-80.64 26.24zm312.32-4.48-34.56-84.48H460.8l-34.56 84.48h-76.8l159.36-373.76h69.12l159.36 373.76h-87.04zm-111.36-272-49.28 120.32h98.56l-49.28-120.32z"/></symbol>'
    document.body.appendChild(svg)
}
