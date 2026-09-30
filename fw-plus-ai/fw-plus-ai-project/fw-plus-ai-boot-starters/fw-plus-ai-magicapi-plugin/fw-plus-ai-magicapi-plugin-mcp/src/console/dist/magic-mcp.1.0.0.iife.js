var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = '\n.magic-mcp-info[data-v-2bc4b11b]{\n  display: flex;\n  flex-direction: column;\n}\n.magic-mcp-info form[data-v-2bc4b11b]{\n  display: flex;\n  align-items: center;\n  padding: 5px;\n}\n.magic-mcp-info form label[data-v-2bc4b11b]{\n  display: inline-block;\n  width: auto;\n  height: var(--magic-input-height);\n  line-height: var(--magic-input-height);\n  font-weight: 400;\n  text-align: right;\n  padding: 0 5px;\n}\n\n.magic-mcp-info[data-v-facf203a]{\n  display: flex;\n  flex-direction: column;\n  height: 100%;\n}\n.magic-mcp-info form[data-v-facf203a]{\n  display: flex;\n  align-items: center;\n  padding: 5px;\n}\n.magic-mcp-info form label[data-v-facf203a]{\n  display: inline-block;\n  width: auto;\n  height: var(--magic-input-height);\n  line-height: var(--magic-input-height);\n  font-weight: 400;\n  text-align: right;\n  padding: 0 5px;\n}\n.prompt-json-tabs[data-v-facf203a]{\n  display: flex;\n  flex-direction: column;\n  padding: 5px;\n  flex: 1;\n  min-height: 0;\n}\n.prompt-json-tabs .tab-header[data-v-facf203a]{\n  display: flex;\n  border-bottom: 1px solid var(--magic-border-color, #ddd);\n}\n.prompt-json-tabs .tab-header button[data-v-facf203a]{\n  height: var(--magic-input-height);\n  padding: 0 12px;\n  border: none;\n  border-bottom: 2px solid transparent;\n  background: transparent;\n  cursor: pointer;\n}\n.prompt-json-tabs .tab-header button.active[data-v-facf203a]{\n  color: var(--magic-primary-color, #409eff);\n  border-bottom-color: var(--magic-primary-color, #409eff);\n}\n.prompt-json-tabs .tab-panel[data-v-facf203a]{\n  position: relative;\n  flex: 1;\n  min-height: 0;\n  padding-top: 8px;\n}\n.prompt-json-tabs .tab-panel[style*="display: none"][data-v-facf203a]{\n  flex: none;\n}\n.prompt-json-tabs[data-v-facf203a] .json-editor.magic-monaco-editor{\n  position: absolute;\n  left: 0;\n  right: 0;\n  top: 8px;\n  bottom: 0;\n}\n';
document.head.appendChild(__vite_style__);
var MagicMcp = function(vue) {
  "use strict";
  function MagicMcpResource(bus, constants, $i, Message, request) {
    return {
      getIcon: (item) => ["RES", "#3fb950"],
      name: $i("mcp.resource.name"),
      language: "json",
      defaultScript: `{
  "text": "Hello MCP resource"
}`,
      runnable: true,
      execute: (item) => request.post("/mcp/diagnostics/execute", {
        jsonrpc: "2.0",
        id: Date.now(),
        method: "resources/read",
        params: { uri: item.uri }
      }),
      requirePath: true,
      merge: (item) => Object.assign({
        uri: "",
        name: "",
        mimeType: "application/json",
        description: "",
        template: false
      }, item)
    };
  }
  function MagicMcpPrompt(bus, constants, $i, Message, request) {
    return {
      getIcon: (item) => ["PRM", "#d29922"],
      name: $i("mcp.prompt.name"),
      language: "json",
      defaultScript: JSON.stringify({
        arguments: [{
          name: "content",
          description: "Prompt input content",
          required: true
        }],
        messages: [{
          role: "user",
          content: {
            type: "text",
            text: "{{content}}"
          }
        }]
      }, null, 2),
      runnable: true,
      execute: (item) => request.post("/mcp/diagnostics/execute", {
        jsonrpc: "2.0",
        id: Date.now(),
        method: "prompts/get",
        params: {
          name: item.name,
          arguments: item.previewArguments || {}
        }
      }),
      requirePath: true,
      merge: (item) => Object.assign({
        name: "",
        description: "",
        arguments: [],
        messages: []
      }, item)
    };
  }
  function MagicMcpServer(bus, constants, $i, Message, request) {
    return {
      getIcon: (item) => ["SRV", "#3fb950"],
      name: $i("mcp.server.name"),
      language: "json",
      defaultScript: `{
  "key": "report",
  "endpoint": "/mcp/report",
  "transport": "http-sse"
}`,
      requirePath: true,
      merge: (item) => Object.assign({
        key: "",
        name: "",
        transport: "http-sse",
        endpoint: "",
        enabled: true,
        tools: null
      }, item),
      runnable: false
    };
  }
  var localZhCN = {
    mcp: {
      resource: {
        title: "MCP \u8D44\u6E90",
        name: "MCP \u8D44\u6E90",
        form: {
          uri: "URI",
          name: "\u540D\u79F0",
          mimeType: "MIME",
          description: "\u63CF\u8FF0",
          template: "\u6A21\u677F",
          placeholder: {
            uri: "\u4F8B\u5982 mcp://docs/readme",
            name: "\u8BF7\u8F93\u5165\u8D44\u6E90\u540D\u79F0",
            mimeType: "\u4F8B\u5982 text/plain \u6216 application/json",
            description: "\u8BF7\u8F93\u5165\u8D44\u6E90\u63CF\u8FF0"
          }
        }
      },
      prompt: {
        title: "MCP \u63D0\u793A\u8BCD",
        name: "MCP \u63D0\u793A\u8BCD",
        form: {
          name: "\u540D\u79F0",
          description: "\u63CF\u8FF0",
          arguments: "\u53C2\u6570",
          messages: "\u6D88\u606F",
          placeholder: {
            name: "\u8BF7\u8F93\u5165\u63D0\u793A\u8BCD\u540D\u79F0",
            description: "\u8BF7\u8F93\u5165\u63D0\u793A\u8BCD\u63CF\u8FF0",
            arguments: 'JSON \u53C2\u6570\u6570\u7EC4\uFF0C\u4F8B\u5982 [{"name":"content","description":"\u5185\u5BB9","required":true}]',
            messages: 'JSON \u6D88\u606F\u6570\u7EC4\uFF0C\u4F8B\u5982 [{"role":"user","content":{"type":"text","text":"{{content}}"}}]'
          }
        }
      },
      server: {
        title: "MCP \u670D\u52A1\u5B9E\u4F8B",
        name: "MCP \u670D\u52A1\u5B9E\u4F8B"
      }
    }
  };
  var localEn = {
    mcp: {
      resource: {
        title: "MCP Resource",
        name: "MCP Resource",
        form: {
          uri: "URI",
          name: "Name",
          mimeType: "MIME",
          description: "Description",
          template: "Template",
          placeholder: {
            uri: "For example mcp://docs/readme",
            name: "Please enter resource name",
            mimeType: "For example text/plain or application/json",
            description: "Please enter resource description"
          }
        }
      },
      prompt: {
        title: "MCP Prompt",
        name: "MCP Prompt",
        form: {
          name: "Name",
          description: "Description",
          arguments: "Arguments",
          messages: "Messages",
          placeholder: {
            name: "Please enter prompt name",
            description: "Please enter prompt description",
            arguments: 'JSON argument array, for example [{"name":"content","description":"Content","required":true}]',
            messages: 'JSON message array, for example [{"role":"user","content":{"type":"text","text":"{{content}}"}}]'
          }
        }
      },
      server: {
        title: "MCP Server Instance",
        name: "MCP Server Instance"
      }
    }
  };
  var magicMcpResourceInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
  var _export_sfc = (sfc, props) => {
    const target = sfc.__vccOpts || sfc;
    for (const [key, val] of props) {
      target[key] = val;
    }
    return target;
  };
  const _hoisted_1$1 = { class: "magic-mcp-info" };
  const _sfc_main$1 = {
    __name: "magic-mcp-resource-info",
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const info = vue.inject("info");
      return (_ctx, _cache) => {
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
        return vue.openBlock(), vue.createElementBlock("div", _hoisted_1$1, [
          vue.createElementVNode("form", null, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.resource.form.uri")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).uri,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).uri = $event),
              placeholder: vue.unref($i)("mcp.resource.form.placeholder.uri"),
              width: "260px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.resource.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).name,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).name = $event),
              placeholder: vue.unref($i)("mcp.resource.form.placeholder.name"),
              width: "180px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.resource.form.mimeType")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).mimeType,
              "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).mimeType = $event),
              placeholder: vue.unref($i)("mcp.resource.form.placeholder.mimeType"),
              width: "220px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.resource.form.template")), 1),
            vue.createVNode(_component_magic_checkbox, {
              value: vue.unref(info).template,
              "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).template = $event)
            }, null, 8, ["value"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.resource.form.description")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).description,
              "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).description = $event),
              placeholder: vue.unref($i)("mcp.resource.form.placeholder.description"),
              width: "auto",
              style: { "flex": "1" }
            }, null, 8, ["value", "placeholder"])
          ])
        ]);
      };
    }
  };
  var MagicMcpResourceInfo = /* @__PURE__ */ _export_sfc(_sfc_main$1, [["__scopeId", "data-v-2bc4b11b"]]);
  var magicMcpPromptInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
  const _hoisted_1 = { class: "magic-mcp-info" };
  const _hoisted_2 = { class: "prompt-json-tabs" };
  const _hoisted_3 = { class: "tab-header" };
  const _hoisted_4 = { class: "tab-panel" };
  const _hoisted_5 = { class: "tab-panel" };
  const _sfc_main = {
    __name: "magic-mcp-prompt-info",
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const info = vue.inject("info");
      const activeTab = vue.ref("arguments");
      const stringify = (value) => JSON.stringify(value || [], null, 2);
      const parse = (value) => {
        try {
          return JSON.parse(value || "[]");
        } catch (e) {
          return null;
        }
      };
      const argumentsText = vue.ref(stringify(info.arguments));
      const messagesText = vue.ref(stringify(info.messages));
      vue.watch(() => info.arguments, (value) => {
        const text = stringify(value);
        if (text !== argumentsText.value) {
          argumentsText.value = text;
        }
      }, { deep: true });
      vue.watch(() => info.messages, (value) => {
        const text = stringify(value);
        if (text !== messagesText.value) {
          messagesText.value = text;
        }
      }, { deep: true });
      vue.watch(argumentsText, (value) => {
        const parsed = parse(value);
        if (parsed) {
          info.arguments = parsed;
        }
      });
      vue.watch(messagesText, (value) => {
        const parsed = parse(value);
        if (parsed) {
          info.messages = parsed;
        }
      });
      return (_ctx, _cache) => {
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
        return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
          vue.createElementVNode("form", null, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.prompt.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).name,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
              placeholder: vue.unref($i)("mcp.prompt.form.placeholder.name"),
              width: "220px"
            }, null, 8, ["value", "placeholder"]),
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mcp.prompt.form.description")), 1),
            vue.createVNode(_component_magic_input, {
              value: vue.unref(info).description,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).description = $event),
              placeholder: vue.unref($i)("mcp.prompt.form.placeholder.description"),
              width: "auto",
              style: { "flex": "1" }
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_2, [
            vue.createElementVNode("div", _hoisted_3, [
              vue.createElementVNode("button", {
                type: "button",
                class: vue.normalizeClass({ active: activeTab.value === "arguments" }),
                onClick: _cache[2] || (_cache[2] = ($event) => activeTab.value = "arguments")
              }, vue.toDisplayString(vue.unref($i)("mcp.prompt.form.arguments")), 3),
              vue.createElementVNode("button", {
                type: "button",
                class: vue.normalizeClass({ active: activeTab.value === "messages" }),
                onClick: _cache[3] || (_cache[3] = ($event) => activeTab.value = "messages")
              }, vue.toDisplayString(vue.unref($i)("mcp.prompt.form.messages")), 3)
            ]),
            vue.withDirectives(vue.createElementVNode("div", _hoisted_4, [
              vue.createVNode(_component_magic_monaco_editor, {
                value: argumentsText.value,
                "onUpdate:value": _cache[4] || (_cache[4] = ($event) => argumentsText.value = $event),
                placeholder: vue.unref($i)("mcp.prompt.form.placeholder.arguments"),
                language: "json",
                class: "json-editor"
              }, null, 8, ["value", "placeholder"])
            ], 512), [
              [vue.vShow, activeTab.value === "arguments"]
            ]),
            vue.withDirectives(vue.createElementVNode("div", _hoisted_5, [
              vue.createVNode(_component_magic_monaco_editor, {
                value: messagesText.value,
                "onUpdate:value": _cache[5] || (_cache[5] = ($event) => messagesText.value = $event),
                placeholder: vue.unref($i)("mcp.prompt.form.placeholder.messages"),
                language: "json",
                class: "json-editor"
              }, null, 8, ["value", "placeholder"])
            ], 512), [
              [vue.vShow, activeTab.value === "messages"]
            ])
          ])
        ]);
      };
    }
  };
  var MagicMcpPromptInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-facf203a"]]);
  if (typeof window !== "undefined") {
    let loadSvg = function() {
      var body = document.body;
      var svgDom = document.getElementById("__svg__icons__dom__1790696383468__");
      if (!svgDom) {
        svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svgDom.style.position = "absolute";
        svgDom.style.width = "0";
        svgDom.style.height = "0";
        svgDom.id = "__svg__icons__dom__1790696383468__";
        svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
      }
      svgDom.innerHTML = '<symbol viewBox="0 0 1024 1024"  id="magic-mcp-prompt"><path d="M160 160h704c35.3 0 64 28.7 64 64v448c0 35.3-28.7 64-64 64H393.4L256 864V736h-96c-35.3 0-64-28.7-64-64V224c0-35.3 28.7-64 64-64zm128 192v64h448v-64H288zm0 128v64h320v-64H288z" /></symbol><symbol viewBox="0 0 1024 1024"  id="magic-mcp-resource"><path d="M192 128h640c35.3 0 64 28.7 64 64v640c0 35.3-28.7 64-64 64H192c-35.3 0-64-28.7-64-64V192c0-35.3 28.7-64 64-64zm64 192v96h512v-96H256zm0 160v96h512v-96H256zm0 160v96h320v-96H256z" /></symbol><symbol viewBox="0 0 1024 1024"  id="magic-mcp-server"><path d="M192 128h640c35.3 0 64 28.7 64 64v192c0 35.3-28.7 64-64 64H192c-35.3 0-64-28.7-64-64V192c0-35.3 28.7-64 64-64zm0 320h640c35.3 0 64 28.7 64 64v320c0 35.3-28.7 64-64 64H192c-35.3 0-64-28.7-64-64V512c0-35.3 28.7-64 64-64zm128 96v64h384v-64H320zm0 192v64h384v-64H320z" /></symbol>';
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
    return {
      resource: [{
        type: "mcp-resource",
        icon: "#magic-mcp-resource",
        title: "mcp.resource.name",
        service: MagicMcpResource(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
      }, {
        type: "mcp-prompt",
        icon: "#magic-mcp-prompt",
        title: "mcp.prompt.name",
        service: MagicMcpPrompt(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
      }, {
        type: "mcp-server",
        icon: "#magic-mcp-server",
        title: "mcp.server.name",
        service: MagicMcpServer(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
      }],
      toolbars: [{
        type: "mcp-resource",
        title: "mcp.resource.title",
        icon: "#magic-mcp-resource",
        order: 500,
        component: MagicMcpResourceInfo
      }, {
        type: "mcp-prompt",
        title: "mcp.prompt.title",
        icon: "#magic-mcp-prompt",
        order: 510,
        component: MagicMcpPromptInfo
      }]
    };
  };
  return index;
}(Vue);
