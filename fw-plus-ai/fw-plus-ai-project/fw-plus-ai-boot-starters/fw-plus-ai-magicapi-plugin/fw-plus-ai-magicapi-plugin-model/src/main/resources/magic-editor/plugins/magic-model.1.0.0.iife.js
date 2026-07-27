var MagicModel = function(vue) {
  "use strict";
  var localZhCN = {
    "model.datasource.title": "Model",
    "model.datasource.name": "\u6A21\u578B\u6570\u636E\u6E90",
    "model.datasource.form.name": "\u6570\u636E\u6E90\u540D\u79F0",
    "model.datasource.form.key": "\u6570\u636E\u6E90\u6807\u8BC6",
    "model.datasource.form.provider": "\u63D0\u4F9B\u5546",
    "model.datasource.form.modelType": "\u6A21\u578B\u7C7B\u578B",
    "model.datasource.form.enabled": "\u542F\u7528",
    "model.datasource.form.baseUrl": "\u670D\u52A1\u5730\u5740",
    "model.datasource.form.apiKey": "API Key",
    "model.datasource.form.modelName": "\u6A21\u578B\u540D\u79F0",
    "model.datasource.form.properties": "\u8FDE\u63A5\u53C2\u6570",
    "model.datasource.form.placeholder.name": "\u8BF7\u8F93\u5165\u6570\u636E\u6E90\u540D\u79F0",
    "model.datasource.form.placeholder.key": "\u5982 qwen",
    "model.datasource.form.placeholder.provider": "\u5982 open-ai \u6216 dashscope",
    "model.datasource.form.placeholder.baseUrl": "\u5982 https://api.example.com/v1",
    "model.datasource.form.placeholder.apiKey": "\u8BF7\u8F93\u5165 API Key",
    "model.datasource.form.placeholder.modelName": "\u5982 qwen-plus",
    "model.datasource.form.placeholder.properties": "\u8BF7\u8F93\u5165 JSON \u5BF9\u8C61"
  };
  var localEn = {
    "model.datasource.title": "Model",
    "model.datasource.name": "Model datasource",
    "model.datasource.form.name": "Name",
    "model.datasource.form.key": "Key",
    "model.datasource.form.provider": "Provider",
    "model.datasource.form.modelType": "Model type",
    "model.datasource.form.enabled": "Enabled",
    "model.datasource.form.baseUrl": "Base URL",
    "model.datasource.form.apiKey": "API key",
    "model.datasource.form.modelName": "Model name",
    "model.datasource.form.properties": "Properties",
    "model.datasource.form.placeholder.name": "Datasource name",
    "model.datasource.form.placeholder.key": "Example: qwen",
    "model.datasource.form.placeholder.provider": "Example: open-ai or dashscope",
    "model.datasource.form.placeholder.baseUrl": "Example: https://api.example.com/v1",
    "model.datasource.form.placeholder.apiKey": "API key",
    "model.datasource.form.placeholder.modelName": "Example: qwen-plus",
    "model.datasource.form.placeholder.properties": "JSON object"
  };
  const parseJsonObject = (value) => {
    if (!value) {
      return {};
    }
    if (typeof value === "object") {
      return value;
    }
    const parsed = JSON.parse(value);
    if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
      throw new Error("properties must be a JSON object");
    }
    return parsed;
  };
  const normalize = (info) => {
    const payload = Object.assign({}, info || {});
    payload.properties = parseJsonObject(payload.propertiesText || payload.properties);
    delete payload.propertiesText;
    return payload;
  };
  var MagicModelDataSource = (request, $i, modal) => ({
    dialogWidth: "960px",
    doTest: (info) => {
      let payload;
      try {
        payload = normalize(info);
      } catch (e) {
        modal.alert(e.message, $i("datasource.test"));
        return;
      }
      request.sendJson("/model/datasource/test", payload).success((res) => {
        if (res === "ok" || res && res.data === "ok") {
          modal.alert($i("datasource.connected"), $i("datasource.test"));
        } else {
          const message = res && (res.message || res.msg || res.data) ? res.message || res.msg || res.data : res;
          modal.alert($i("datasource.connectFailed", message), $i("datasource.test"));
        }
      });
    }
  });
  const _hoisted_1 = { class: "magic-form-row" };
  const _hoisted_2 = { class: "magic-form-row" };
  const _hoisted_3 = { class: "magic-form-row" };
  const _hoisted_4 = { class: "magic-form-row" };
  const _hoisted_5 = { class: "magic-form-row" };
  const _hoisted_6 = { class: "magic-form-row" };
  const _hoisted_7 = { class: "magic-form-row" };
  const _hoisted_8 = { class: "magic-form-row" };
  const _hoisted_9 = { class: "magic-form-row" };
  const _sfc_main = {
    __name: "magic-model-datasource-info",
    props: {
      info: {
        type: Object,
        default: () => ({ enabled: true, modelType: "CHAT", properties: {} })
      }
    },
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const props = __props;
      const modelTypeOptions = [
        { text: "CHAT", value: "CHAT" },
        { text: "STREAMING_CHAT", value: "STREAMING_CHAT" },
        { text: "EMBEDDING", value: "EMBEDDING" },
        { text: "IMAGE", value: "IMAGE" },
        { text: "LANGUAGE", value: "LANGUAGE" },
        { text: "STREAMING_LANGUAGE", value: "STREAMING_LANGUAGE" },
        { text: "MODERATION", value: "MODERATION" },
        { text: "SCORING", value: "SCORING" }
      ];
      const propertiesText = vue.ref("{}");
      const formatJson = (value) => {
        if (!value) {
          return "{}";
        }
        return typeof value === "string" ? value : JSON.stringify(value, null, 2);
      };
      vue.onMounted(() => {
        if (!props.info.propertiesText) {
          props.info.propertiesText = formatJson(props.info.properties);
        }
        if (props.info.enabled === void 0) {
          props.info.enabled = true;
        }
        if (!props.info.modelType) {
          props.info.modelType = "CHAT";
        }
        propertiesText.value = props.info.propertiesText;
      });
      vue.watch(propertiesText, (value) => {
        props.info.propertiesText = value;
      });
      return (_ctx, _cache) => {
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_select = vue.resolveComponent("magic-select");
        const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
        const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
        return vue.openBlock(), vue.createElementBlock("div", null, [
          vue.createElementVNode("div", _hoisted_1, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.name,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => __props.info.name = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.name"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_2, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.key")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.key,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => __props.info.key = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.key"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_3, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.provider")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.provider,
              "onUpdate:value": _cache[2] || (_cache[2] = ($event) => __props.info.provider = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.provider"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_4, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.modelType")), 1),
            vue.createVNode(_component_magic_select, {
              value: __props.info.modelType,
              "onUpdate:value": _cache[3] || (_cache[3] = ($event) => __props.info.modelType = $event),
              options: modelTypeOptions,
              width: "100%"
            }, null, 8, ["value"])
          ]),
          vue.createElementVNode("div", _hoisted_5, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.enabled")), 1),
            vue.createVNode(_component_magic_checkbox, {
              value: __props.info.enabled,
              "onUpdate:value": _cache[4] || (_cache[4] = ($event) => __props.info.enabled = $event)
            }, null, 8, ["value"])
          ]),
          vue.createElementVNode("div", _hoisted_6, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.baseUrl")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.baseUrl,
              "onUpdate:value": _cache[5] || (_cache[5] = ($event) => __props.info.baseUrl = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.baseUrl"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_7, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.apiKey")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.apiKey,
              "onUpdate:value": _cache[6] || (_cache[6] = ($event) => __props.info.apiKey = $event),
              type: "password",
              placeholder: vue.unref($i)("model.datasource.form.placeholder.apiKey"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_8, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.modelName")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.modelName,
              "onUpdate:value": _cache[7] || (_cache[7] = ($event) => __props.info.modelName = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.modelName"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_9, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("model.datasource.form.properties")), 1),
            vue.createVNode(_component_magic_monaco_editor, {
              value: propertiesText.value,
              "onUpdate:value": _cache[8] || (_cache[8] = ($event) => propertiesText.value = $event),
              placeholder: vue.unref($i)("model.datasource.form.placeholder.properties"),
              language: "json",
              style: { "height": "220px" }
            }, null, 8, ["value", "placeholder"])
          ])
        ]);
      };
    }
  };
  if (typeof window !== "undefined") {
    const svgBody = '<symbol viewBox="0 0 24 24" id="magic-model-model"><path d="M12 3a7 7 0 0 0-7 7v4a3 3 0 0 0 3 3h1v-6H7v-1a5 5 0 0 1 10 0v1h-2v6h1a3 3 0 0 0 3-3v-4a7 7 0 0 0-7-7Zm-3 16h6v2H9v-2Z"/></symbol>';
    const svgDom = document.getElementById("__svg__icons__dom__");
    if (svgDom) {
      svgDom.innerHTML += svgBody;
    } else {
      const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
      svg.style.position = "absolute";
      svg.style.width = "0";
      svg.style.height = "0";
      svg.id = "__svg__icons__dom__";
      svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
      svg.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
      svg.innerHTML = svgBody;
      document.body.insertBefore(svg, document.body.firstChild);
    }
  }
  var index = (opt) => {
    const i18n = opt.i18n;
    i18n.add("zh-cn", localZhCN);
    i18n.add("en", localEn);
    return {
      name: "model",
      statusBarItems: [{
        icon: "#magic-model-model",
        title: "Model",
        displayKey: "model"
      }],
      datasources: [{
        type: "model-datasource",
        icon: "#magic-model-model",
        title: i18n.format("model.datasource.title"),
        name: i18n.format("model.datasource.name"),
        service: MagicModelDataSource(opt.request, i18n.format, opt.modal),
        component: _sfc_main
      }]
    };
  };
  return index;
}(Vue);
