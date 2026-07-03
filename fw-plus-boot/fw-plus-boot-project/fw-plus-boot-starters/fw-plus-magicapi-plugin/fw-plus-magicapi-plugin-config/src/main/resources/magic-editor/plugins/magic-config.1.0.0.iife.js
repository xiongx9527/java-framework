var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-config-info[data-v-3838d4a8]{\n  display: flex;\n  flex-direction: column;\n  flex: 1;\n  padding: 5px;\n}\n.magic-config-info form[data-v-3838d4a8]{\n  display: flex;\n}\n.magic-config-info form label[data-v-3838d4a8]{\n  display: inline-block;\n  width: 75px;\n  height: 22px;\n  line-height: 22px;\n  font-weight: 400;\n  text-align: right;\n  padding: 0 5px;\n}\n.magic-config-info form[data-v-3838d4a8] .magic-checkbox{\n  width: 22px;\n  height: 22px;\n}\n.magic-config-info form[data-v-3838d4a8] .magic-textarea{\n  margin: 5px;\n}\n";
document.head.appendChild(__vite_style__);
var MagicConfig = function(vue) {
  "use strict";
  function MagicTask(bus, constants, $i, Message, request) {
    return {
      getIcon: (item) => ["CONF", "#00BFFF"],
      name: $i("config.name"),
      language: "yaml",
      defaultScript: `# Hello Magic-Config`,
      runnable: false,
      requirePath: true,
      merge: (item) => item
    };
  }
  var localZhCN = {
    config: {
      title: "\u914D\u7F6E\u6587\u4EF6\u7BA1\u7406",
      name: "\u914D\u7F6E\u6587\u4EF6",
      form: {
        enabled: "\u542F\u7528",
        name: "\u914D\u7F6E\u540D\u79F0",
        appId: "\u5E94\u7528Id",
        namespace: "\u547D\u540D\u7A7A\u95F4",
        env: "\u73AF\u5883",
        externalFlag: "\u5916\u90E8\u914D\u7F6E",
        commonFlag: "\u901A\u7528\u914D\u7F6E",
        placeholder: {
          name: "\u8BF7\u8F93\u5165\u914D\u7F6E\u6587\u4EF6\u540D\u79F0",
          appId: "\u8BF7\u8F93\u5165\u5E94\u7528Id",
          namespace: "\u8BF7\u8F93\u5165\u547D\u540D\u7A7A\u95F4",
          env: "\u8BF7\u8F93\u5165\u73AF\u5883",
          description: "\u8BF7\u8F93\u5165\u914D\u7F6E\u6587\u4EF6\u63CF\u8FF0"
        }
      }
    }
  };
  var localEn = {
    config: {
      title: "Configuration Info",
      name: "Configuration",
      form: {
        enabled: "Enabled",
        name: "Name",
        appId: "AppId",
        namespace: "Namespace",
        env: "Env",
        externalFlag: "External Config",
        commonFlag: "Common Config",
        path: "Path",
        placeholder: {
          name: "Please Enter Conf Name",
          appId: "Please Enter AppId",
          namespace: "Please Enter Namespace",
          env: "Please Enter Env",
          description: "Please Enter Configuration Description"
        }
      }
    }
  };
  var magicConfigInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
  var _export_sfc = (sfc, props) => {
    const target = sfc.__vccOpts || sfc;
    for (const [key, val] of props) {
      target[key] = val;
    }
    return target;
  };
  const _hoisted_1 = { class: "magic-config-info" };
  const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
  const _sfc_main = {
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const info = vue.inject("info");
      return (_ctx, _cache) => {
        const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_textarea = vue.resolveComponent("magic-textarea");
        return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
          vue.createElementVNode("form", null, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.enabled")), 1),
            vue.createVNode(_component_magic_checkbox, {
              value: vue.unref(info).enabled,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).enabled = $event)
            }, null, 8, ["value"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.env")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).env,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).env = $event),
              placeholder: vue.unref($i)("config.form.placeholder.env"),
              width: "180px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.appId")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).appId,
              "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).appId = $event),
              placeholder: vue.unref($i)("config.form.placeholder.appId"),
              width: "180px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.namespace")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).namespace,
              "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).namespace = $event),
              placeholder: vue.unref($i)("config.form.placeholder.namespace"),
              width: "180px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.externalFlag")), 1),
            vue.createVNode(_component_magic_checkbox, {
              value: vue.unref(info).externalFlag,
              "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).externalFlag = $event)
            }, null, 8, ["value"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.commonFlag")), 1),
            vue.createVNode(_component_magic_checkbox, {
              value: vue.unref(info).commonFlag,
              "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).commonFlag = $event)
            }, null, 8, ["value"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("config.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).name,
              "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).name = $event),
              placeholder: vue.unref($i)("config.form.placeholder.name"),
              width: "auto",
              style: { "flex": "1" }
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_2, [
            vue.createVNode(_component_magic_textarea, {
              value: vue.unref(info).description,
              "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).description = $event),
              placeholder: vue.unref($i)("config.form.placeholder.description")
            }, null, 8, ["value", "placeholder"])
          ])
        ]);
      };
    }
  };
  var MagicConfigInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-3838d4a8"]]);
  function setYaml(monaco) {
    monaco.languages.register({ id: "yaml" });
    monaco.languages.setMonarchTokensProvider("yaml", {
      tokenPostfix: ".yaml",
      brackets: [
        { token: "delimiter.bracket", open: "{", close: "}" },
        { token: "delimiter.square", open: "[", close: "]" }
      ],
      keywords: ["true", "True", "TRUE", "false", "False", "FALSE", "null", "Null", "Null", "~"],
      numberInteger: /(?:0|[+-]?[0-9]+)/,
      numberFloat: /(?:0|[+-]?[0-9]+)(?:\.[0-9]+)?(?:e[-+][1-9][0-9]*)?/,
      numberOctal: /0o[0-7]+/,
      numberHex: /0x[0-9a-fA-F]+/,
      numberInfinity: /[+-]?\.(?:inf|Inf|INF)/,
      numberNaN: /\.(?:nan|Nan|NAN)/,
      numberDate: /\d{4}-\d\d-\d\d([Tt ]\d\d:\d\d:\d\d(\.\d+)?(( ?[+-]\d\d?(:\d\d)?)|Z)?)?/,
      escapes: /\\(?:[btnfr\\"']|[0-7][0-7]?|[0-3][0-7]{2})/,
      tokenizer: {
        root: [
          { include: "@whitespace" },
          { include: "@comment" },
          [/%[^ ]+.*$/, "meta.directive"],
          [/---/, "operators.directivesEnd"],
          [/\.{3}/, "operators.documentEnd"],
          [/[-?:](?= )/, "operators"],
          { include: "@anchor" },
          { include: "@tagHandle" },
          { include: "@flowCollections" },
          { include: "@blockStyle" },
          [/@numberInteger(?![ \t]*\S+)/, "number"],
          [/@numberFloat(?![ \t]*\S+)/, "number.float"],
          [/@numberOctal(?![ \t]*\S+)/, "number.octal"],
          [/@numberHex(?![ \t]*\S+)/, "number.hex"],
          [/@numberInfinity(?![ \t]*\S+)/, "number.infinity"],
          [/@numberNaN(?![ \t]*\S+)/, "number.nan"],
          [/@numberDate(?![ \t]*\S+)/, "number.date"],
          [/(".*?"|'.*?'|.*?)([ \t]*)(:)( |$)/, ["type", "white", "operators", "white"]],
          { include: "@flowScalars" },
          [
            /[^#]+/,
            {
              cases: {
                "@keywords": "keyword",
                "@default": "string"
              }
            }
          ]
        ],
        object: [
          { include: "@whitespace" },
          { include: "@comment" },
          [/\}/, "@brackets", "@pop"],
          [/,/, "delimiter.comma"],
          [/:(?= )/, "operators"],
          [/(?:".*?"|'.*?'|[^,\{\[]+?)(?=: )/, "type"],
          { include: "@flowCollections" },
          { include: "@flowScalars" },
          { include: "@tagHandle" },
          { include: "@anchor" },
          { include: "@flowNumber" },
          [
            /[^\},]+/,
            {
              cases: {
                "@keywords": "keyword",
                "@default": "string"
              }
            }
          ]
        ],
        array: [
          { include: "@whitespace" },
          { include: "@comment" },
          [/\]/, "@brackets", "@pop"],
          [/,/, "delimiter.comma"],
          { include: "@flowCollections" },
          { include: "@flowScalars" },
          { include: "@tagHandle" },
          { include: "@anchor" },
          { include: "@flowNumber" },
          [
            /[^\],]+/,
            {
              cases: {
                "@keywords": "keyword",
                "@default": "string"
              }
            }
          ]
        ],
        multiString: [[/^( +).+$/, "string", "@multiStringContinued.$1"]],
        multiStringContinued: [
          [
            /^( *).+$/,
            {
              cases: {
                "$1==$S2": "string",
                "@default": { token: "@rematch", next: "@popall" }
              }
            }
          ]
        ],
        whitespace: [[/[ \t\r\n]+/, "white"]],
        comment: [[/#.*$/, "comment"]],
        flowCollections: [
          [/\[/, "@brackets", "@array"],
          [/\{/, "@brackets", "@object"]
        ],
        flowScalars: [
          [/"([^"\\]|\\.)*$/, "string.invalid"],
          [/'([^'\\]|\\.)*$/, "string.invalid"],
          [/'[^']*'/, "string"],
          [/"/, "string", "@doubleQuotedString"]
        ],
        doubleQuotedString: [
          [/[^\\"]+/, "string"],
          [/@escapes/, "string.escape"],
          [/\\./, "string.escape.invalid"],
          [/"/, "string", "@pop"]
        ],
        blockStyle: [[/[>|][0-9]*[+-]?$/, "operators", "@multiString"]],
        flowNumber: [
          [/@numberInteger(?=[ \t]*[,\]\}])/, "number"],
          [/@numberFloat(?=[ \t]*[,\]\}])/, "number.float"],
          [/@numberOctal(?=[ \t]*[,\]\}])/, "number.octal"],
          [/@numberHex(?=[ \t]*[,\]\}])/, "number.hex"],
          [/@numberInfinity(?=[ \t]*[,\]\}])/, "number.infinity"],
          [/@numberNaN(?=[ \t]*[,\]\}])/, "number.nan"],
          [/@numberDate(?=[ \t]*[,\]\}])/, "number.date"]
        ],
        tagHandle: [[/\![^ ]*/, "tag"]],
        anchor: [[/[&*][^ ]+/, "namespace"]]
      }
    });
    monaco.languages.setLanguageConfiguration("yaml", {
      comments: {
        lineComment: "#"
      },
      brackets: [
        ["{", "}"],
        ["[", "]"],
        ["(", ")"]
      ],
      autoClosingPairs: [
        { open: "{", close: "}" },
        { open: "[", close: "]" },
        { open: "(", close: ")" },
        { open: '"', close: '"' },
        { open: "'", close: "'" }
      ],
      surroundingPairs: [
        { open: "{", close: "}" },
        { open: "[", close: "]" },
        { open: "(", close: ")" },
        { open: '"', close: '"' },
        { open: "'", close: "'" }
      ],
      folding: {
        offSide: true
      }
    });
  }
  if (typeof window !== "undefined") {
    let loadSvg = function() {
      var body = document.body;
      var svgDom = document.getElementById("__svg__icons__dom__1757055600399__");
      if (!svgDom) {
        svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svgDom.style.position = "absolute";
        svgDom.style.width = "0";
        svgDom.style.height = "0";
        svgDom.id = "__svg__icons__dom__1757055600399__";
        svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
      }
      svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 1084 1024"  id="magic-config-config"><defs><style>@font-face{font-family:rbicon;src:url(chrome-extension://dipiagiiohfljcicegpgffpbnjmgjcnf/fonts/rbicon.woff2) format("woff2");font-weight:400;font-style:normal}</style></defs><path d="M1080.096 434.5c-4.216-23.731-26.924-47.945-50.596-53.185l-17.648-4.096a175.94 175.94 0 0 1-101.613-80.832 177.807 177.807 0 0 1-18.732-129.802l5.541-16.685c7.108-23.13-2.108-54.992-20.6-70.833 0 0-16.624-14.095-63.244-41.2-46.8-26.984-66.858-34.513-66.858-34.513-22.768-8.373-54.632-.362-71.256 17.407l-12.287 13.251a173.47 173.47 0 0 1-120.466 48.066 174.133 174.133 0 0 1-121.008-48.487l-11.745-12.83C393.14 2.992 361.096-4.899 338.268 3.354c0 0-20.359 7.529-67.1 34.513-46.8 27.346-63.244 41.44-63.244 41.44-18.431 15.661-27.647 47.223-20.54 70.593l5.06 16.866a178.048 178.048 0 0 1-18.672 129.62A174.916 174.916 0 0 1 71.496 377.46l-17.045 3.855c-23.31 5.421-46.26 29.334-50.596 53.186 0 0-3.855 21.382-3.855 75.712s3.855 75.713 3.855 75.713C8.07 609.9 30.779 633.872 54.45 639.112l16.624 3.855A174.254 174.254 0 0 1 173.47 724.28c23.31 40.838 28.911 87.338 18.732 129.802l-4.818 16.444c-7.108 23.129 2.108 54.992 20.6 70.833 0 0 16.623 14.095 63.244 41.2 46.8 27.105 66.918 34.513 66.918 34.513 22.708 8.373 54.632.362 71.256-17.407l11.625-12.589a175.097 175.097 0 0 1 242.257.12l11.624 12.65c16.384 17.708 48.428 25.599 71.256 17.347 0 0 20.359-7.53 67.16-34.514 46.74-27.105 63.124-41.2 63.124-41.2 18.491-15.6 27.707-47.463 20.6-70.833l-5.06-17.106A176.723 176.723 0 0 1 910.66 724.4a176.06 176.06 0 0 1 102.396-81.314l16.684-3.855c23.31-5.42 46.26-29.333 50.596-53.185 0 0 3.855-21.383 3.855-75.713-.241-54.33-4.096-75.833-4.096-75.833zm-537.82 293.335c-119.26 0-216.175-97.336-216.175-217.622a216.658 216.658 0 0 1 216.236-217.32c119.2 0 216.115 97.276 216.115 217.561-.24 120.045-96.974 217.32-216.175 217.32z" fill="#bfbfbf" /></symbol>';
      body.insertBefore(svgDom, body.firstChild);
    };
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", loadSvg);
    } else {
      loadSvg();
    }
  }
  var index = (opt) => {
    const i18n = opt.i18n;
    i18n.add("zh-cn", localZhCN);
    i18n.add("en", localEn);
    setYaml(opt.monaco);
    return {
      resource: [{
        type: "config",
        icon: "#magic-config-config",
        title: "config.name",
        service: MagicTask(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
      }],
      toolbars: [{
        type: "config",
        title: "config.title",
        icon: "parameter",
        component: MagicConfigInfo
      }]
    };
  };
  return index;
}(Vue);
