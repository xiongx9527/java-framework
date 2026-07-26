var MagicSwagger = function() {
  "use strict";
  const symbol = '<symbol viewBox="0 0 24 24" id="magic-swagger-knife4j"><path d="M5 2h10l4 4v16H5V2zm9 1.5V7h3.5L14 3.5zM8 10v9h2v-3l1.2-1.5L14 19h2.4l-3.9-6 3.5-3h-2.7L10 13v-3H8z"/></symbol>';
  const installIcon = () => {
    if (typeof document === "undefined" || document.getElementById("magic-swagger-knife4j"))
      return;
    let sprite = document.getElementById("__svg__icons__dom__");
    if (!sprite) {
      sprite = document.createElementNS("http://www.w3.org/2000/svg", "svg");
      sprite.id = "__svg__icons__dom__";
      sprite.style.position = "absolute";
      sprite.style.width = "0";
      sprite.style.height = "0";
      document.body.insertBefore(sprite, document.body.firstChild);
    }
    sprite.insertAdjacentHTML("beforeend", symbol);
  };
  installIcon();
  const resolveBase = () => {
    var _a;
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace("/index.html", "");
    const baseURL = ((_a = { "BASE_URL": "./", "MODE": "production", "DEV": false, "PROD": true }) == null ? void 0 : _a.MAGIC_API_BASE_URL) || "./";
    if (baseURL.startsWith("http")) {
      link = new URL(baseURL).origin;
    } else if (baseURL.startsWith("./")) {
      link = `${window.location.protocol}//${window.location.host}`;
    }
    return link;
  };
  var index = () => ({
    name: "swagger",
    statusBarItems: [{
      icon: "#magic-swagger-knife4j",
      title: "Swagger Api Doc",
      displayKey: "swagger",
      order: 1200,
      onClick: () => window.open(resolveBase() + "/doc.html", "_blank")
    }]
  });
  return index;
}();
