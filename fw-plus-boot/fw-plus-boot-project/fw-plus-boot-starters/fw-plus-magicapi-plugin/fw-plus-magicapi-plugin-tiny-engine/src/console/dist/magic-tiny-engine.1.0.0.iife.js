var MagicTinyEngine = function() {
  "use strict";
  const DEFAULT_PATH = "/tiny-engine/";
  const normalizePath = (path) => {
    if (!path) {
      return DEFAULT_PATH;
    }
    const prefixed = path.startsWith("/") ? path : `/${path}`;
    return prefixed.endsWith("/") ? prefixed : `${prefixed}/`;
  };
  const replaceURL = (url) => url.replace(/:?\/+/g, (value) => value.indexOf(":") > -1 ? value : "/");
  const normalizeURL = (url) => {
    if (!url) {
      return null;
    }
    return url.endsWith("/") ? url : `${url}/`;
  };
  const resolveBase = () => {
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace("/index.html", "");
    const baseURL = "./";
    if (baseURL.startsWith("http")) {
      link = new URL(baseURL).origin;
    } else if (baseURL.startsWith("./")) {
      link = `${window.location.protocol}//${window.location.host}`;
    }
    return link;
  };
  var index = ({ pluginData = {} } = {}) => ({
    name: "tiny-engine",
    statusBarItems: [{
      icon: "app",
      title: "TinyEngine",
      displayKey: "tiny-engine",
      order: 1600,
      onClick: () => {
        var _a, _b;
        const url = normalizeURL((_a = pluginData.attributes) == null ? void 0 : _a.url);
        if (url) {
          window.open(url, "_blank");
          return;
        }
        const link = resolveBase();
        const path = normalizePath((_b = pluginData.attributes) == null ? void 0 : _b.path);
        window.open(replaceURL(link + path), "_blank");
      }
    }]
  });
  return index;
}();
