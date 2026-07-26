const symbol = typeof __MAGIC_SWAGGER_SYMBOL__ === 'undefined' ? '' : __MAGIC_SWAGGER_SYMBOL__

export const installIcon = () => {
    if (!symbol || typeof document === 'undefined' || document.getElementById('magic-swagger-knife4j')) return
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
