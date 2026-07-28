var MagicEureka = function() {
  "use strict";
  const symbol = '<symbol viewBox="0 0 24 24" id="magic-eureka-eureka"><path d="M13.138.053a12.018 12.018 0 0 0-9.646 3.481A12.014 12.014 0 0 0 .937 16.651a12.014 12.014 0 0 0 11.162 7.348 2.275 2.275 0 1 0-.037-4.549 7.438 7.438 0 0 1-6.932-4.562 7.438 7.438 0 0 1 1.587-8.145 7.437 7.437 0 0 1 12.378 3.014H12.05a2.275 2.275 0 1 0 0 4.55h9.674A2.275 2.275 0 0 0 24 12.04 12.014 12.014 0 0 0 16.597.914a11.962 11.962 0 0 0-3.459-.86z"/></symbol>';
  const installIcon = () => {
    if (typeof document === "undefined" || document.getElementById("magic-eureka-eureka"))
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
  const replaceURL = (url) => url.replace(/:?\/+/g, (value) => value.indexOf(":") > -1 ? value : "/");
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
    name: "eureka",
    statusBarItems: [{
      icon: "#magic-eureka-eureka",
      title: "Eureka Dashboard",
      displayKey: "eureka",
      order: 1600,
      onClick: () => {
        var _a;
        const link = resolveBase();
        const path = ((_a = pluginData.attributes) == null ? void 0 : _a.path) || "/";
        window.open(path ? replaceURL(link + "/" + path) : replaceURL(link), "_blank");
      }
    }]
  });
  return index;
}();
