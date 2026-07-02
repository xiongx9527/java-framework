const prefix = `magic-`
class Store {
    constructor() {
    }

    set(key, value) {
        if (Array.isArray(value) || typeof value == 'object') {
            value = JSON.stringify(value);
        }
        localStorage.setItem(`${prefix}${key}`, value);
    }

    remove(key) {
        localStorage.removeItem(`${prefix}${key}`)
    }

    get(key) {
        return localStorage.getItem(`${prefix}${key}`);
    }
}
export default new Store()