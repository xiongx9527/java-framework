const symbol = '<symbol viewBox="0 0 135.297 120.367" id="magic-admin-admin"><path fill="#42d3a5" d="m133.605 66.313-27.691 47.963a12.22 12.22 0 0 1-10.582 6.109H39.949a12.22 12.22 0 0 1-10.582-6.109L1.677 66.313a12.22 12.22 0 0 1 0-12.22L29.367 6.132A12.22 12.22 0 0 1 39.949.021h55.383a12.22 12.22 0 0 1 10.582 6.111l27.691 47.961a12.22 12.22 0 0 1 0 12.22"/><path fill="#fff" d="M61.533 19.968a3.414 3.414 0 0 0-3.141 2.262L40.557 71.925l-5.856-9.76a3.414 3.414 0 0 0-2.929-1.66H.051a12.19 12.19 0 0 0 2.22 6.833h27.566l8.57 14.283a3.414 3.414 0 0 0 6.145-.585l17.258-48.095 18.953 46.411a3.414 3.414 0 0 0 6.06.481l15.15-24.28 8.844 11.564a3.414 3.414 0 0 0 2.714 1.342h18.824a12.22 12.22 0 0 0 2.799-6.834H115.23l-10.816-14.145a3.414 3.414 0 0 0-5.613.266L84.564 70.566 64.767 22.089a3.414 3.414 0 0 0-3.234-2.121z"/></symbol>'

export const installIcon = () => {
    if (typeof document === 'undefined' || document.getElementById('magic-admin-admin')) return
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
