var MagicDruid = function() {
  "use strict";
  const symbol = '<symbol viewBox="0 0 483.272 483.272" id="magic-druid-druid">\n<path style="fill:#096AD8;" d="M298.472,207.328c-43.6,0-77.328,35.472-77.328,77.344c0,43.592,33.728,76.776,80.84,76.776\n	c39.52,0,72.672-32.584,72.672-75.616C374.664,241.04,341.512,207.328,298.472,207.328z"/>\n<path style="fill:#0D56BF;" d="M355.288,335.984c27.944-27.944,28.344-74.432-2.088-104.856\n	c-31.664-31.664-78.944-32.064-109.376-1.624"/>\n<path style="fill:#2A98F2;" d="M377.944,407.944c-32,26.76-57.184,36.632-94.376,36.632c-37.832,0-71.344-12.216-98.672-36.632\n	c-36.064-31.984-55.16-73.848-55.16-122.704c0-44.76,17.208-84.912,49.792-116.304c29.048-28.496,63.776-43.04,103.344-43.04\n	c37.792,0,71.064,12.8,87.064,39.552V37.72C329.944,13.976,288.888,0.168,241.328,0.168C108.032,0.168,0.304,108.2,0.304,241.496\n	c0,133.288,108.184,240.664,241.48,240.664c49.168,0,95.048-16.664,133.184-40.664h2.976V407.944z"/>\n<path style="fill:#1580EA;" d="M0.616,241.496c0,133.288,108.032,240.664,241.328,240.664c49.168,0,94.888-16.664,133.032-40.664\n	h2.968v-33.552c-32,26.76-57.184,36.632-94.376,36.632c-37.832,0-71.344-12.216-98.672-36.632\n	c-36.064-31.984-55.16-73.848-55.16-122.704c0-44.76,17.208-84.912,49.792-116.304c29.048-28.496,63.776-43.04,103.344-43.04\n	c37.792,0,71.064,12.8,87.064,39.552V37.72C329.944,13.976,288.888,0.168,241.328,0.168"/>\n<path style="fill:#096AD8;" d="M0,243.384c0,133.288,108.336,239.72,241.64,239.72c49.168,0,95.2-17.608,133.336-41.608h2.968\n	v-33.552c-32,26.76-57.184,36.632-94.376,36.632c-37.832,0-71.344-12.216-98.672-36.632c-36.064-31.984-55.16-73.848-55.16-122.704\n	c0-44.76,17.208-84.912,49.792-116.304c29.048-28.496,63.776-43.04,103.344-43.04c37.792,0,71.064,12.8,87.064,39.552V41.496"/>\n<g>\n	<path style="fill:#0D56BF;" d="M465.944,338.232c12.984-29.624,17.328-62.328,17.328-96.736s-4.344-67.112-17.328-96.736"/>\n	<path style="fill:#0D56BF;" d="M465.944,144.76V10.336l-96,19.016v8.368C409.944,63.352,441.944,100.592,465.944,144.76z"/>\n	<path style="fill:#0D56BF;" d="M465.944,338.232c-24,42.776-49.448,79.264-88,103.264h88V338.232z"/>\n</g>\n</symbol>';
  const installIcon = () => {
    if (typeof document === "undefined" || document.getElementById("magic-druid-druid"))
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
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace("/index.html", "");
    const baseURL = "./";
    if (baseURL.startsWith("http")) {
      link = new URL(baseURL).origin;
    } else if (baseURL.startsWith("./")) {
      link = `${window.location.protocol}//${window.location.host}`;
    }
    return link;
  };
  var index = () => ({
    name: "druid",
    statusBarItems: [{
      icon: "#magic-druid-druid",
      title: "Druid DataSource",
      displayKey: "druid",
      order: 1100,
      onClick: () => window.open(resolveBase() + "/druid/login.html", "_blank")
    }]
  });
  return index;
}();
