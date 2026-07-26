const symbol = '<symbol viewBox="0 0 483.272 483.272" id="magic-druid-druid"><path fill="#2A98F2" d="M377.944 407.944c-32 26.76-57.184 36.632-94.376 36.632-37.832 0-71.344-12.216-98.672-36.632-36.064-31.984-55.16-73.848-55.16-122.704 0-44.76 17.208-84.912 49.792-116.304 29.048-28.496 63.776-43.04 103.344-43.04 37.792 0 71.064 12.8 87.064 39.552V37.72C329.944 13.976 288.888.168 241.328.168 108.032.168.304 108.2.304 241.496c0 133.288 108.184 240.664 241.48 240.664 49.168 0 95.048-16.664 133.184-40.664h2.976v-33.552z"/><path fill="#096AD8" d="M298.472 207.328c-43.6 0-77.328 35.472-77.328 77.344 0 43.592 33.728 76.776 80.84 76.776 39.52 0 72.672-32.584 72.672-75.616.008-44.792-33.144-78.504-76.192-78.504zM465.944 144.76V10.336l-96 19.016v8.368c40 25.632 72 62.872 96 107.04zM465.944 338.232c-24 42.776-49.448 79.264-88 103.264h88V338.232z"/></symbol>'

export const installIcon = () => {
    if (typeof document === 'undefined' || document.getElementById('magic-druid-druid')) return
    let sprite = document.getElementById('__svg__icons__dom__')
    if (!sprite) {
        sprite = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        sprite.id = '__svg__icons__dom__'
        sprite.style.position = 'absolute'
        sprite.style.width = '0'
        sprite.style.height = '0'
        document.body.insertBefore(sprite, document.body.firstChild)
    }
    sprite.insertAdjacentHTML('beforeend', symbol)
}
