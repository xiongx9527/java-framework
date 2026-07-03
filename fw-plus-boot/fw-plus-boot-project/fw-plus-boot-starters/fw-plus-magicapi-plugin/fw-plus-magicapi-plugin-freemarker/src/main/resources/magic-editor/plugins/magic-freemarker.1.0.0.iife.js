var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-freemarker-info[data-v-a1e727cc]{\n   display: flex;\n   flex-direction: column;\n}\n.magic-freemarker-info form[data-v-a1e727cc]{\n  display: flex;\n  padding: 5px;\n}\n.magic-freemarker-info form label[data-v-a1e727cc]{\n  display: inline-block;\n  width: 75px;\n  height: var(--magic-input-height);\n  line-height: var(--magic-input-height);\n  font-weight: 400;\n  text-align: right;\n  padding: 0 5px;\n}\n.magic-freemarker-info + .magic-navbar[data-v-a1e727cc]{\n  flex-direction: column;\n  overflow: hidden;\n}\n.magic-freemarker-info + .magic-navbar[data-v-a1e727cc] .magic-navbar-header{\n  border-bottom: 1px solid var(--main-border-color);\n}\n.magic-navbar[data-v-a1e727cc] .magic-navbar-body,\n.magic-navbar[data-v-a1e727cc] .magic-navbar-item{\n  width: 100%;\n  height: 100%;\n}\n";
document.head.appendChild(__vite_style__);
var MagicFreemarker = function(vue) {
  "use strict";
  function MagicFreemarker2(bus, constants, $i, Message, request) {
    return {
      getIcon: (item) => ["FTL", "#02FAF1"],
      name: $i("freemarker.name"),
      language: "html",
      defaultScript: `<#--Hello Magic-APi-FreeMarker-->`,
      doTest: (opened) => {
        opened.running = true;
        const info = opened.item;
        const requestConfig = {
          baseURL: constants.SERVER_URL,
          url: "/freemarker/execute",
          method: "POST",
          responseType: "json",
          headers: {},
          withCredentials: true
        };
        bus.$emit(Message.SWITCH_TOOLBAR, "log");
        requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID;
        requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id;
        requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE;
        requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter((it) => it.options.linesDecorationsClassName === "breakpoints").map((it) => it.range.startLineNumber).join(",");
        const fullName = opened.path();
        bus.status(`\u5F00\u59CB\u6D4B\u8BD5freemarker\u300C${fullName}\u300D`);
        console.log("info.params:", info.params);
        request.sendPost("/freemarker/execute", { id: info.id, params: info.params }, requestConfig).success((res) => {
          opened.running = false;
          const newWindow = window.open("", "_blank");
          newWindow.document.write(res);
          newWindow.document.close();
          newWindow.document.title = `FreeMarker\u7ED3\u679C - ${fullName}`;
        }).end(() => {
          bus.status(`freemarker\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
          opened.running = false;
        });
      },
      runnable: true,
      requirePath: true,
      merge: (item) => item
    };
  }
  var localZhCN = {
    freemarker: {
      title: "Freemarker",
      result: "Freemarker\u8FD0\u884C\u7ED3\u679C",
      name: "Freemarker",
      form: {
        id: "Id",
        name: "\u6A21\u677F\u540D\u79F0",
        path: "\u6A21\u677F\u8DEF\u5F84",
        params: "\u6A21\u677F\u8FD0\u884C\u53C2\u6570",
        description: "\u6A21\u7248\u63CF\u8FF0",
        placeholder: {
          name: "\u8BF7\u8F93\u5165\u6A21\u677F\u540D\u79F0",
          path: "\u8BF7\u8F93\u5165\u6A21\u677F\u8DEF\u5F84",
          description: "\u8BF7\u8F93\u5165\u6A21\u7248\u63CF\u8FF0"
        }
      }
    }
  };
  var localEn = {
    freemarker: {
      title: "Freemarker Info",
      result: "Freemarker Result",
      name: "Freemarker",
      form: {
        id: "Id",
        name: "Freemarker Name",
        path: "Freemarker Path",
        params: "Freemarker Test Params",
        description: "Freemarker Description",
        placeholder: {
          name: "Please Enter Freemarker Name",
          path: "Please Enter Freemarker Path",
          description: "Please Enter Freemarker Description"
        }
      }
    }
  };
  var magicFreemarkerInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
  var _export_sfc = (sfc, props) => {
    const target = sfc.__vccOpts || sfc;
    for (const [key, val] of props) {
      target[key] = val;
    }
    return target;
  };
  const _hoisted_1 = {
    class: "magic-freemarker-info",
    style: { "height": "100vh" }
  };
  const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
  const _sfc_main = {
    __name: "magic-freemarker-info",
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const info = vue.inject("info");
      return (_ctx, _cache) => {
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_textarea = vue.resolveComponent("magic-textarea");
        return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
          vue.createElementVNode("form", null, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vue.form.id")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).id,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).id = $event),
              width: "200px",
              readonly: ""
            }, null, 8, ["value"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("freemarker.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).name,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).name = $event),
              placeholder: vue.unref($i)("freemarker.form.placeholder.name"),
              width: "200px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("freemarker.form.path")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).path,
              "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).path = $event),
              placeholder: vue.unref($i)("freemarker.form.placeholder.path"),
              width: "300px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("freemarker.form.description")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).description,
              "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).description = $event),
              placeholder: vue.unref($i)("freemarker.form.placeholder.description"),
              width: "auto",
              style: { "flex": "1" }
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_2, [
            vue.createVNode(_component_magic_textarea, {
              value: vue.unref(info).params,
              "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).params = $event),
              placeholder: vue.unref($i)("freemarker.form.params")
            }, null, 8, ["value", "placeholder"])
          ])
        ]);
      };
    }
  };
  var MagicFreemarkerInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-a1e727cc"]]);
  if (typeof window !== "undefined") {
    let loadSvg = function() {
      var body = document.body;
      var svgDom = document.getElementById("__svg__icons__dom__1762666696250__");
      if (!svgDom) {
        svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svgDom.style.position = "absolute";
        svgDom.style.width = "0";
        svgDom.style.height = "0";
        svgDom.id = "__svg__icons__dom__1762666696250__";
        svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
      }
      svgDom.innerHTML = '<symbol  viewBox="0 0 512 512" id="magic-freemarker-ftl"><path d="M425.782 45.045A31.999 31.999 0 0 0 400 32H96c-17.673 0-32 14.327-32 32v384a32 32 0 0 0 54.627 22.627L237.255 352H320a32 32 0 0 0 30.543-22.455l80-256a31.999 31.999 0 0 0-4.761-28.5zM336.474 160H224c-17.673 0-32 14.327-32 32s14.327 32 32 32h92.474l-20 64H224a31.996 31.996 0 0 0-22.627 9.373L128 370.745V96h228.474l-20 64z" /></symbol>';
      body.insertBefore(svgDom, body.firstChild);
    };
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", loadSvg);
    } else {
      loadSvg();
    }
  }
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  var conf$3 = {
    wordPattern: /(#?-?\d*\.\d\w*%?)|((::|[@#.!:])?[\w-?]+%?)|::|[@#.!:]/g,
    comments: {
      blockComment: ["/*", "*/"]
    },
    brackets: [
      ["{", "}"],
      ["[", "]"],
      ["(", ")"]
    ],
    autoClosingPairs: [
      { open: "{", close: "}", notIn: ["string", "comment"] },
      { open: "[", close: "]", notIn: ["string", "comment"] },
      { open: "(", close: ")", notIn: ["string", "comment"] },
      { open: '"', close: '"', notIn: ["string", "comment"] },
      { open: "'", close: "'", notIn: ["string", "comment"] }
    ],
    surroundingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"' },
      { open: "'", close: "'" }
    ],
    folding: {
      markers: {
        start: new RegExp("^\\s*\\/\\*\\s*#region\\b\\s*(.*?)\\s*\\*\\/"),
        end: new RegExp("^\\s*\\/\\*\\s*#endregion\\b.*\\*\\/")
      }
    }
  };
  var language$3 = {
    defaultToken: "",
    tokenPostfix: ".css",
    ws: "[ 	\n\r\f]*",
    identifier: "-?-?([a-zA-Z]|(\\\\(([0-9a-fA-F]{1,6}\\s?)|[^[0-9a-fA-F])))([\\w\\-]|(\\\\(([0-9a-fA-F]{1,6}\\s?)|[^[0-9a-fA-F])))*",
    brackets: [
      { open: "{", close: "}", token: "delimiter.bracket" },
      { open: "[", close: "]", token: "delimiter.bracket" },
      { open: "(", close: ")", token: "delimiter.parenthesis" },
      { open: "<", close: ">", token: "delimiter.angle" }
    ],
    tokenizer: {
      root: [{ include: "@selector" }],
      selector: [
        { include: "@comments" },
        { include: "@import" },
        { include: "@strings" },
        [
          "[@](keyframes|-webkit-keyframes|-moz-keyframes|-o-keyframes)",
          { token: "keyword", next: "@keyframedeclaration" }
        ],
        ["[@](page|content|font-face|-moz-document)", { token: "keyword" }],
        ["[@](charset|namespace)", { token: "keyword", next: "@declarationbody" }],
        [
          "(url-prefix)(\\()",
          ["attribute.value", { token: "delimiter.parenthesis", next: "@urldeclaration" }]
        ],
        [
          "(url)(\\()",
          ["attribute.value", { token: "delimiter.parenthesis", next: "@urldeclaration" }]
        ],
        { include: "@selectorname" },
        ["[\\*]", "tag"],
        ["[>\\+,]", "delimiter"],
        ["\\[", { token: "delimiter.bracket", next: "@selectorattribute" }],
        ["{", { token: "delimiter.bracket", next: "@selectorbody" }]
      ],
      selectorbody: [
        { include: "@comments" },
        ["[*_]?@identifier@ws:(?=(\\s|\\d|[^{;}]*[;}]))", "attribute.name", "@rulevalue"],
        ["}", { token: "delimiter.bracket", next: "@pop" }]
      ],
      selectorname: [
        ["(\\.|#(?=[^{])|%|(@identifier)|:)+", "tag"]
      ],
      selectorattribute: [{ include: "@term" }, ["]", { token: "delimiter.bracket", next: "@pop" }]],
      term: [
        { include: "@comments" },
        [
          "(url-prefix)(\\()",
          ["attribute.value", { token: "delimiter.parenthesis", next: "@urldeclaration" }]
        ],
        [
          "(url)(\\()",
          ["attribute.value", { token: "delimiter.parenthesis", next: "@urldeclaration" }]
        ],
        { include: "@functioninvocation" },
        { include: "@numbers" },
        { include: "@name" },
        { include: "@strings" },
        ["([<>=\\+\\-\\*\\/\\^\\|\\~,])", "delimiter"],
        [",", "delimiter"]
      ],
      rulevalue: [
        { include: "@comments" },
        { include: "@strings" },
        { include: "@term" },
        ["!important", "keyword"],
        [";", "delimiter", "@pop"],
        ["(?=})", { token: "", next: "@pop" }]
      ],
      warndebug: [["[@](warn|debug)", { token: "keyword", next: "@declarationbody" }]],
      import: [["[@](import)", { token: "keyword", next: "@declarationbody" }]],
      urldeclaration: [
        { include: "@strings" },
        ["[^)\r\n]+", "string"],
        ["\\)", { token: "delimiter.parenthesis", next: "@pop" }]
      ],
      parenthizedterm: [
        { include: "@term" },
        ["\\)", { token: "delimiter.parenthesis", next: "@pop" }]
      ],
      declarationbody: [
        { include: "@term" },
        [";", "delimiter", "@pop"],
        ["(?=})", { token: "", next: "@pop" }]
      ],
      comments: [
        ["\\/\\*", "comment", "@comment"],
        ["\\/\\/+.*", "comment"]
      ],
      comment: [
        ["\\*\\/", "comment", "@pop"],
        [/[^*/]+/, "comment"],
        [/./, "comment"]
      ],
      name: [["@identifier", "attribute.value"]],
      numbers: [
        ["-?(\\d*\\.)?\\d+([eE][\\-+]?\\d+)?", { token: "attribute.value.number", next: "@units" }],
        ["#[0-9a-fA-F_]+(?!\\w)", "attribute.value.hex"]
      ],
      units: [
        [
          "(em|ex|ch|rem|vmin|vmax|vw|vh|vm|cm|mm|in|px|pt|pc|deg|grad|rad|turn|s|ms|Hz|kHz|%)?",
          "attribute.value.unit",
          "@pop"
        ]
      ],
      keyframedeclaration: [
        ["@identifier", "attribute.value"],
        ["{", { token: "delimiter.bracket", switchTo: "@keyframebody" }]
      ],
      keyframebody: [
        { include: "@term" },
        ["{", { token: "delimiter.bracket", next: "@selectorbody" }],
        ["}", { token: "delimiter.bracket", next: "@pop" }]
      ],
      functioninvocation: [
        ["@identifier\\(", { token: "attribute.value", next: "@functionarguments" }]
      ],
      functionarguments: [
        ["\\$@identifier@ws:", "attribute.name"],
        ["[,]", "delimiter"],
        { include: "@term" },
        ["\\)", { token: "attribute.value", next: "@pop" }]
      ],
      strings: [
        ['~?"', { token: "string", next: "@stringenddoublequote" }],
        ["~?'", { token: "string", next: "@stringendquote" }]
      ],
      stringenddoublequote: [
        ["\\\\.", "string"],
        ['"', { token: "string", next: "@pop" }],
        [/[^\\"]+/, "string"],
        [".", "string"]
      ],
      stringendquote: [
        ["\\\\.", "string"],
        ["'", { token: "string", next: "@pop" }],
        [/[^\\']+/, "string"],
        [".", "string"]
      ]
    }
  };
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  function setCss(monaco) {
    monaco.languages.register({ id: "css" });
    monaco.languages.setMonarchTokensProvider("css", language$3);
    monaco.languages.setLanguageConfiguration("css", conf$3);
  }
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  var conf$2 = {
    wordPattern: /(-?\d*\.\d\w*)|([^\`\~\!\@\#\%\^\&\*\(\)\-\=\+\[\{\]\}\\\|\;\:\'\"\,\.\<\>\/\?\s]+)/g,
    comments: {
      lineComment: "//",
      blockComment: ["/*", "*/"]
    },
    brackets: [
      ["{", "}"],
      ["[", "]"],
      ["(", ")"]
    ],
    onEnterRules: [
      {
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        afterText: /^\s*\*\/$/,
        action: {
          appendText: " * "
        }
      },
      {
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        action: {
          appendText: " * "
        }
      },
      {
        beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
        action: {
          appendText: "* "
        }
      },
      {
        beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
        action: {
          removeText: 1
        }
      }
    ],
    autoClosingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"', notIn: ["string"] },
      { open: "'", close: "'", notIn: ["string", "comment"] },
      { open: "`", close: "`", notIn: ["string", "comment"] },
      { open: "/**", close: " */", notIn: ["string"] }
    ],
    folding: {
      markers: {
        start: new RegExp("^\\s*//\\s*#?region\\b"),
        end: new RegExp("^\\s*//\\s*#?endregion\\b")
      }
    }
  };
  var language$2 = {
    defaultToken: "invalid",
    tokenPostfix: ".ts",
    keywords: [
      "abstract",
      "any",
      "as",
      "asserts",
      "bigint",
      "boolean",
      "break",
      "case",
      "catch",
      "class",
      "continue",
      "const",
      "constructor",
      "debugger",
      "declare",
      "default",
      "delete",
      "do",
      "else",
      "enum",
      "export",
      "extends",
      "false",
      "finally",
      "for",
      "from",
      "function",
      "get",
      "if",
      "implements",
      "import",
      "in",
      "infer",
      "instanceof",
      "interface",
      "is",
      "keyof",
      "let",
      "module",
      "namespace",
      "never",
      "new",
      "null",
      "number",
      "object",
      "package",
      "private",
      "protected",
      "public",
      "override",
      "readonly",
      "require",
      "global",
      "return",
      "set",
      "static",
      "string",
      "super",
      "switch",
      "symbol",
      "this",
      "throw",
      "true",
      "try",
      "type",
      "typeof",
      "undefined",
      "unique",
      "unknown",
      "var",
      "void",
      "while",
      "with",
      "yield",
      "async",
      "await",
      "of"
    ],
    operators: [
      "<=",
      ">=",
      "==",
      "!=",
      "===",
      "!==",
      "=>",
      "+",
      "-",
      "**",
      "*",
      "/",
      "%",
      "++",
      "--",
      "<<",
      "</",
      ">>",
      ">>>",
      "&",
      "|",
      "^",
      "!",
      "~",
      "&&",
      "||",
      "??",
      "?",
      ":",
      "=",
      "+=",
      "-=",
      "*=",
      "**=",
      "/=",
      "%=",
      "<<=",
      ">>=",
      ">>>=",
      "&=",
      "|=",
      "^=",
      "@"
    ],
    symbols: /[=><!~?:&|+\-*\/\^%]+/,
    escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,
    digits: /\d+(_+\d+)*/,
    octaldigits: /[0-7]+(_+[0-7]+)*/,
    binarydigits: /[0-1]+(_+[0-1]+)*/,
    hexdigits: /[[0-9a-fA-F]+(_+[0-9a-fA-F]+)*/,
    regexpctl: /[(){}\[\]\$\^|\-*+?\.]/,
    regexpesc: /\\(?:[bBdDfnrstvwWn0\\\/]|@regexpctl|c[A-Z]|x[0-9a-fA-F]{2}|u[0-9a-fA-F]{4})/,
    tokenizer: {
      root: [[/[{}]/, "delimiter.bracket"], { include: "common" }],
      common: [
        [
          /[a-z_$][\w$]*/,
          {
            cases: {
              "@keywords": "keyword",
              "@default": "identifier"
            }
          }
        ],
        [/[A-Z][\w\$]*/, "type.identifier"],
        { include: "@whitespace" },
        [
          /\/(?=([^\\\/]|\\.)+\/([dgimsuy]*)(\s*)(\.|;|,|\)|\]|\}|$))/,
          { token: "regexp", bracket: "@open", next: "@regexp" }
        ],
        [/[()\[\]]/, "@brackets"],
        [/[<>](?!@symbols)/, "@brackets"],
        [/!(?=([^=]|$))/, "delimiter"],
        [
          /@symbols/,
          {
            cases: {
              "@operators": "delimiter",
              "@default": ""
            }
          }
        ],
        [/(@digits)[eE]([\-+]?(@digits))?/, "number.float"],
        [/(@digits)\.(@digits)([eE][\-+]?(@digits))?/, "number.float"],
        [/0[xX](@hexdigits)n?/, "number.hex"],
        [/0[oO]?(@octaldigits)n?/, "number.octal"],
        [/0[bB](@binarydigits)n?/, "number.binary"],
        [/(@digits)n?/, "number"],
        [/[;,.]/, "delimiter"],
        [/"([^"\\]|\\.)*$/, "string.invalid"],
        [/'([^'\\]|\\.)*$/, "string.invalid"],
        [/"/, "string", "@string_double"],
        [/'/, "string", "@string_single"],
        [/`/, "string", "@string_backtick"]
      ],
      whitespace: [
        [/[ \t\r\n]+/, ""],
        [/\/\*\*(?!\/)/, "comment.doc", "@jsdoc"],
        [/\/\*/, "comment", "@comment"],
        [/\/\/.*$/, "comment"]
      ],
      comment: [
        [/[^\/*]+/, "comment"],
        [/\*\//, "comment", "@pop"],
        [/[\/*]/, "comment"]
      ],
      jsdoc: [
        [/[^\/*]+/, "comment.doc"],
        [/\*\//, "comment.doc", "@pop"],
        [/[\/*]/, "comment.doc"]
      ],
      regexp: [
        [
          /(\{)(\d+(?:,\d*)?)(\})/,
          ["regexp.escape.control", "regexp.escape.control", "regexp.escape.control"]
        ],
        [
          /(\[)(\^?)(?=(?:[^\]\\\/]|\\.)+)/,
          ["regexp.escape.control", { token: "regexp.escape.control", next: "@regexrange" }]
        ],
        [/(\()(\?:|\?=|\?!)/, ["regexp.escape.control", "regexp.escape.control"]],
        [/[()]/, "regexp.escape.control"],
        [/@regexpctl/, "regexp.escape.control"],
        [/[^\\\/]/, "regexp"],
        [/@regexpesc/, "regexp.escape"],
        [/\\\./, "regexp.invalid"],
        [/(\/)([dgimsuy]*)/, [{ token: "regexp", bracket: "@close", next: "@pop" }, "keyword.other"]]
      ],
      regexrange: [
        [/-/, "regexp.escape.control"],
        [/\^/, "regexp.invalid"],
        [/@regexpesc/, "regexp.escape"],
        [/[^\]]/, "regexp"],
        [
          /\]/,
          {
            token: "regexp.escape.control",
            next: "@pop",
            bracket: "@close"
          }
        ]
      ],
      string_double: [
        [/[^\\"]+/, "string"],
        [/@escapes/, "string.escape"],
        [/\\./, "string.escape.invalid"],
        [/"/, "string", "@pop"]
      ],
      string_single: [
        [/[^\\']+/, "string"],
        [/@escapes/, "string.escape"],
        [/\\./, "string.escape.invalid"],
        [/'/, "string", "@pop"]
      ],
      string_backtick: [
        [/\$\{/, { token: "delimiter.bracket", next: "@bracketCounting" }],
        [/[^\\`$]+/, "string"],
        [/@escapes/, "string.escape"],
        [/\\./, "string.escape.invalid"],
        [/`/, "string", "@pop"]
      ],
      bracketCounting: [
        [/\{/, "delimiter.bracket", "@bracketCounting"],
        [/\}/, "delimiter.bracket", "@pop"],
        { include: "common" }
      ]
    }
  };
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  var conf$1 = conf$2;
  var language$1 = {
    defaultToken: "invalid",
    tokenPostfix: ".js",
    keywords: [
      "break",
      "case",
      "catch",
      "class",
      "continue",
      "const",
      "constructor",
      "debugger",
      "default",
      "delete",
      "do",
      "else",
      "export",
      "extends",
      "false",
      "finally",
      "for",
      "from",
      "function",
      "get",
      "if",
      "import",
      "in",
      "instanceof",
      "let",
      "new",
      "null",
      "return",
      "set",
      "super",
      "switch",
      "symbol",
      "this",
      "throw",
      "true",
      "try",
      "typeof",
      "undefined",
      "var",
      "void",
      "while",
      "with",
      "yield",
      "async",
      "await",
      "of"
    ],
    typeKeywords: [],
    operators: language$2.operators,
    symbols: language$2.symbols,
    escapes: language$2.escapes,
    digits: language$2.digits,
    octaldigits: language$2.octaldigits,
    binarydigits: language$2.binarydigits,
    hexdigits: language$2.hexdigits,
    regexpctl: language$2.regexpctl,
    regexpesc: language$2.regexpesc,
    tokenizer: language$2.tokenizer
  };
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  function setJs(monaco) {
    monaco.languages.register({ id: "javascript" });
    monaco.languages.setMonarchTokensProvider("javascript", language$1);
    monaco.languages.setLanguageConfiguration("javascript", conf$1);
  }
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  var EMPTY_ELEMENTS = [
    "area",
    "base",
    "br",
    "col",
    "embed",
    "hr",
    "img",
    "input",
    "keygen",
    "link",
    "menuitem",
    "meta",
    "param",
    "source",
    "track",
    "wbr"
  ];
  var conf = {
    wordPattern: /(-?\d*\.\d\w*)|([^\`\~\!\@\$\^\&\*\(\)\=\+\[\{\]\}\\\|\;\:\'\"\,\.\<\>\/\s]+)/g,
    comments: {
      blockComment: ["<#--", "-->"]
    },
    brackets: [
      ["<#--", "-->"],
      ["<!--", "-->"],
      ["<", ">"],
      ["{", "}"],
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
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: "<", close: ">" }
    ],
    onEnterRules: [
      {
        beforeText: new RegExp(`<(?!(?:${EMPTY_ELEMENTS.join("|")}))([_:\\w][_:\\w-.\\d]*)([^/>]*(?!/)>)[^<]*$`, "i"),
        afterText: /^<\/([_:\w][_:\w-.\d]*)\s*>$/i,
        action: {}
      },
      {
        beforeText: new RegExp(`<(?!(?:${EMPTY_ELEMENTS.join("|")}))(\\w[\\w\\d]*)([^/>]*(?!/)>)[^<]*$`, "i")
      }
    ],
    folding: {
      markers: {
        start: new RegExp("^\\s*<!--\\s*#region\\b.*-->"),
        end: new RegExp("^\\s*<!--\\s*#endregion\\b.*-->")
      }
    }
  };
  var language = {
    defaultToken: "",
    tokenPostfix: ".html",
    ignoreCase: true,
    tokenizer: {
      root: [
        [/<!DOCTYPE/, "metatag", "@doctype"],
        [/<!--/, "comment", "@comment"],
        [/(<)((?:[\w\-]+:)?[\w\-]+)(\s*)(\/>)/, ["delimiter", "tag", "", "delimiter"]],
        [/(<)(script)/, ["delimiter", { token: "tag", next: "@script" }]],
        [/(<)(style)/, ["delimiter", { token: "tag", next: "@style" }]],
        [/(<)((?:[\w\-]+:)?[\w\-]+)/, ["delimiter", { token: "tag", next: "@otherTag" }]],
        [/(<\/)((?:[\w\-]+:)?[\w\-]+)/, ["delimiter", { token: "tag", next: "@otherTag" }]],
        [/</, "delimiter"],
        [/[^<]+/]
      ],
      doctype: [
        [/[^>]+/, "metatag.content"],
        [/>/, "metatag", "@pop"]
      ],
      comment: [
        [/-->/, "comment", "@pop"],
        [/[^-]+/, "comment.content"],
        [/./, "comment.content"]
      ],
      otherTag: [
        [/\/?>/, "delimiter", "@pop"],
        [/"([^"]*)"/, "attribute.value"],
        [/'([^']*)'/, "attribute.value"],
        [/[\w\-]+/, "attribute.name"],
        [/=/, "delimiter"],
        [/[ \t\r\n]+/]
      ],
      script: [
        [/type/, "attribute.name", "@scriptAfterType"],
        [/"([^"]*)"/, "attribute.value"],
        [/'([^']*)'/, "attribute.value"],
        [/[\w\-]+/, "attribute.name"],
        [/=/, "delimiter"],
        [
          />/,
          {
            token: "delimiter",
            next: "@scriptEmbedded",
            nextEmbedded: "javascript"
          }
        ],
        [/[ \t\r\n]+/],
        [/(<\/)(script\s*)(>)/, ["delimiter", "tag", { token: "delimiter", next: "@pop" }]]
      ],
      scriptAfterType: [
        [/=/, "delimiter", "@scriptAfterTypeEquals"],
        [
          />/,
          {
            token: "delimiter",
            next: "@scriptEmbedded",
            nextEmbedded: "javascript"
          }
        ],
        [/[ \t\r\n]+/],
        [/<\/script\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      scriptAfterTypeEquals: [
        [
          /"([^"]*)"/,
          {
            token: "attribute.value",
            switchTo: "@scriptWithCustomType.$1"
          }
        ],
        [
          /'([^']*)'/,
          {
            token: "attribute.value",
            switchTo: "@scriptWithCustomType.$1"
          }
        ],
        [
          />/,
          {
            token: "delimiter",
            next: "@scriptEmbedded",
            nextEmbedded: "javascript"
          }
        ],
        [/[ \t\r\n]+/],
        [/<\/script\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      scriptWithCustomType: [
        [
          />/,
          {
            token: "delimiter",
            next: "@scriptEmbedded.$S2",
            nextEmbedded: "$S2"
          }
        ],
        [/"([^"]*)"/, "attribute.value"],
        [/'([^']*)'/, "attribute.value"],
        [/[\w\-]+/, "attribute.name"],
        [/=/, "delimiter"],
        [/[ \t\r\n]+/],
        [/<\/script\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      scriptEmbedded: [
        [/<\/script/, { token: "@rematch", next: "@pop", nextEmbedded: "@pop" }],
        [/[^<]+/, ""]
      ],
      style: [
        [/type/, "attribute.name", "@styleAfterType"],
        [/"([^"]*)"/, "attribute.value"],
        [/'([^']*)'/, "attribute.value"],
        [/[\w\-]+/, "attribute.name"],
        [/=/, "delimiter"],
        [
          />/,
          {
            token: "delimiter",
            next: "@styleEmbedded",
            nextEmbedded: "css"
          }
        ],
        [/[ \t\r\n]+/],
        [/(<\/)(style\s*)(>)/, ["delimiter", "tag", { token: "delimiter", next: "@pop" }]]
      ],
      styleAfterType: [
        [/=/, "delimiter", "@styleAfterTypeEquals"],
        [
          />/,
          {
            token: "delimiter",
            next: "@styleEmbedded",
            nextEmbedded: "css"
          }
        ],
        [/[ \t\r\n]+/],
        [/<\/style\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      styleAfterTypeEquals: [
        [
          /"([^"]*)"/,
          {
            token: "attribute.value",
            switchTo: "@styleWithCustomType.$1"
          }
        ],
        [
          /'([^']*)'/,
          {
            token: "attribute.value",
            switchTo: "@styleWithCustomType.$1"
          }
        ],
        [
          />/,
          {
            token: "delimiter",
            next: "@styleEmbedded",
            nextEmbedded: "css"
          }
        ],
        [/[ \t\r\n]+/],
        [/<\/style\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      styleWithCustomType: [
        [
          />/,
          {
            token: "delimiter",
            next: "@styleEmbedded.$S2",
            nextEmbedded: "$S2"
          }
        ],
        [/"([^"]*)"/, "attribute.value"],
        [/'([^']*)'/, "attribute.value"],
        [/[\w\-]+/, "attribute.name"],
        [/=/, "delimiter"],
        [/[ \t\r\n]+/],
        [/<\/style\s*>/, { token: "@rematch", next: "@pop" }]
      ],
      styleEmbedded: [
        [/<\/style/, { token: "@rematch", next: "@pop", nextEmbedded: "@pop" }],
        [/[^<]+/, ""]
      ]
    }
  };
  /*!-----------------------------------------------------------------------------
   * Copyright (c) Microsoft Corporation. All rights reserved.
   * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
   * Released under the MIT license
   * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
   *-----------------------------------------------------------------------------*/
  function setHtml(monaco) {
    monaco.languages.register({ id: "freemarker" });
    monaco.languages.setMonarchTokensProvider("freemarker", language);
    monaco.languages.setLanguageConfiguration("freemarker", conf);
  }
  var index = (opt) => {
    const i18n = opt.i18n;
    i18n.add("zh-cn", localZhCN);
    i18n.add("en", localEn);
    setCss(opt.monaco);
    setJs(opt.monaco);
    setHtml(opt.monaco);
    return {
      resource: [{
        type: "freemarker",
        icon: "#magic-freemarker-ftl",
        title: "freemarker.name",
        service: MagicFreemarker2(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
      }],
      toolbars: [{
        type: "freemarker",
        title: "freemarker.title",
        icon: "parameter",
        component: MagicFreemarkerInfo
      }]
    };
  };
  return index;
}(Vue);
