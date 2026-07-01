/**
 * ROT13 函数：对给定字符串执行 ROT13 编码。
 * @param {string} str - 要编码的字符串。
 * @returns {string} 编码后的字符串。
 */
 export function rot13(str) {
    return str.replace(/[a-zA-Z]/g, function (char) {
      const base = char <= 'Z' ? 'A'.charCodeAt(0) : 'a'.charCodeAt(0);
      return String.fromCharCode(((char.charCodeAt(0) - base + 13) % 26) + base);
    });
  }
  
  /**
   * 加密流程：
   * 数据 -> JSON -> UTF8 Base64 编码 -> ROT13 替换
   */
  export function encrypt(data) {
    // 数据 -> JSON
    const json = JSON.stringify(data);
    // JSON -> UTF-8 byte string （兼容中文）
    const utf8Bytes = encodeURIComponent(json);
    // UTF-8 byte string -> Base64
    const base64 = btoa(utf8Bytes.replace(/%([0-9A-F]{2})/g, function(match, p1) {
        return String.fromCharCode('0x' + p1);
    }))
    // Base64 -> ROT13
    const encrypted = rot13(base64);
    return encrypted;
  }
    
  /**
   * 解密流程：
   * ROT13 -> Base64 -> UTF-8 JSON -> 数据对象
   */
  export function decrypt(encrypted) {
    // ROT13 -> Base64
    const base64 = rot13(encrypted);
    // Base64 -> UTF-8 byte string
    const utf8Bytes = atob(base64);
    // UTF-8 byte string -> JSON
    const json = decodeURIComponent(Array.prototype.map.call(utf8Bytes, function(c) {
        return '%' + c.charCodeAt(0).toString(16).padStart(2, '0');
    }).join(''));
    // JSON -> 数据对象
    const result = JSON.parse(json);
    return result
  }