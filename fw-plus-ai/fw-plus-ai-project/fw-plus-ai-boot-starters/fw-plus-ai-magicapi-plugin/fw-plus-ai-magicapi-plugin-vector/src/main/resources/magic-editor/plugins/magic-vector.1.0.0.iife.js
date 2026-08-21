var MagicVector = function(vue) {
  "use strict";
  var localZhCN = {
    "vector.datasource.title": "Vector",
    "vector.datasource.name": "\u5411\u91CF\u6570\u636E\u6E90",
    "vector.datasource.form.name": "\u6570\u636E\u6E90\u540D\u79F0",
    "vector.datasource.form.key": "\u6570\u636E\u6E90\u6807\u8BC6",
    "vector.datasource.form.provider": "\u5411\u91CF\u7C7B\u578B",
    "vector.datasource.form.dimension": "\u5411\u91CF\u7EF4\u5EA6",
    "vector.datasource.form.metadataKeys": "\u5143\u6570\u636E\u5B57\u6BB5",
    "vector.datasource.form.properties": "\u8FDE\u63A5\u53C2\u6570",
    "vector.datasource.form.placeholder.name": "\u8BF7\u8F93\u5165\u6570\u636E\u6E90\u540D\u79F0",
    "vector.datasource.form.placeholder.key": "\u5982 knowledge",
    "vector.datasource.form.placeholder.provider": "\u8BF7\u9009\u62E9\u6216\u8F93\u5165 provider",
    "vector.datasource.form.placeholder.dimension": "\u5982 768",
    "vector.datasource.form.placeholder.metadataKeys": "\u591A\u4E2A\u5B57\u6BB5\u7528\u82F1\u6587\u9017\u53F7\u5206\u9694",
    "vector.datasource.form.placeholder.properties": '\u8BF7\u8F93\u5165 JSON \u5BF9\u8C61\uFF0C\u5982 {"collectionName":"knowledge"}'
  };
  var localEn = {
    "vector.datasource.title": "Vector",
    "vector.datasource.name": "Vector datasource",
    "vector.datasource.form.name": "Name",
    "vector.datasource.form.key": "Key",
    "vector.datasource.form.provider": "Provider",
    "vector.datasource.form.dimension": "Dimension",
    "vector.datasource.form.metadataKeys": "Metadata keys",
    "vector.datasource.form.properties": "Properties",
    "vector.datasource.form.placeholder.name": "Datasource name",
    "vector.datasource.form.placeholder.key": "Example: knowledge",
    "vector.datasource.form.placeholder.provider": "Select or input provider",
    "vector.datasource.form.placeholder.dimension": "Example: 768",
    "vector.datasource.form.placeholder.metadataKeys": "Comma-separated keys",
    "vector.datasource.form.placeholder.properties": 'JSON object, for example {"collectionName":"knowledge"}'
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
    if (typeof payload.dimension === "string" && payload.dimension.trim().length > 0) {
      payload.dimension = Number(payload.dimension);
    }
    if (!payload.dimension && payload.dimension !== 0) {
      delete payload.dimension;
    }
    if (typeof payload.metadataKeysText === "string") {
      payload.metadataKeys = payload.metadataKeysText.split(",").map((item) => item.trim()).filter((item) => item.length > 0);
    }
    delete payload.metadataKeysText;
    return payload;
  };
  var MagicVectorDataSource = (request, $i, modal) => {
    return {
      dialogWidth: "960px",
      doTest: (info) => {
        let payload;
        try {
          payload = normalize(info);
        } catch (e) {
          modal.alert(e.message, $i("datasource.test"));
          return;
        }
        request.sendJson("/vector/datasource/test", payload).success((res) => {
          if (res === "ok" || res && res.data === "ok") {
            modal.alert($i("datasource.connected"), $i("datasource.test"));
          } else {
            const message = res && (res.message || res.msg || res.data) ? res.message || res.msg || res.data : res;
            modal.alert($i("datasource.connectFailed", message), $i("datasource.test"));
          }
        });
      }
    };
  };
  const _hoisted_1 = { class: "magic-form-row" };
  const _hoisted_2 = { class: "magic-form-row" };
  const _hoisted_3 = { class: "magic-form-row" };
  const _hoisted_4 = { class: "magic-form-row" };
  const _hoisted_5 = { class: "magic-form-row" };
  const _hoisted_6 = { class: "magic-form-row" };
  const _sfc_main = {
    __name: "magic-vector-datasource-info",
    props: {
      info: {
        type: Object,
        default: () => ({ properties: {} })
      }
    },
    setup(__props) {
      const $i = vue.inject("i18n.format");
      const props = __props;
      const providerOptions = [
        { text: "in-memory", value: "in-memory" },
        { text: "milvus", value: "milvus" },
        { text: "qdrant", value: "qdrant" },
        { text: "weaviate", value: "weaviate" },
        { text: "redis", value: "redis" },
        { text: "elasticsearch", value: "elasticsearch" },
        { text: "azure-ai-search", value: "azure-ai-search" },
        { text: "azure-cosmos-mongo-vcore", value: "azure-cosmos-mongo-vcore" },
        { text: "azure-cosmos-nosql", value: "azure-cosmos-nosql" },
        { text: "cassandra", value: "cassandra" },
        { text: "astra-db", value: "astra-db" },
        { text: "chroma", value: "chroma" },
        { text: "couchbase", value: "couchbase" },
        { text: "mongodb-atlas", value: "mongodb-atlas" },
        { text: "opensearch", value: "opensearch" },
        { text: "oracle", value: "oracle" },
        { text: "pgvector", value: "pgvector" },
        { text: "pinecone", value: "pinecone" },
        { text: "tablestore", value: "tablestore" },
        { text: "vearch", value: "vearch" },
        { text: "vespa", value: "vespa" },
        { text: "infinispan", value: "infinispan" },
        { text: "neo4j", value: "neo4j" }
      ];
      const propertiesText = vue.ref("{}");
      const metadataKeysText = vue.ref("");
      const formatJson = (value) => {
        if (!value) {
          return "{}";
        }
        if (typeof value === "string") {
          return value;
        }
        return JSON.stringify(value, null, 2);
      };
      const syncFromInfo = () => {
        if (!props.info.propertiesText) {
          props.info.propertiesText = formatJson(props.info.properties);
        }
        propertiesText.value = props.info.propertiesText;
        if (!props.info.metadataKeysText) {
          props.info.metadataKeysText = Array.isArray(props.info.metadataKeys) ? props.info.metadataKeys.join(",") : "";
        }
        metadataKeysText.value = props.info.metadataKeysText;
      };
      vue.onMounted(syncFromInfo);
      vue.watch(propertiesText, (value) => {
        props.info.propertiesText = value;
      });
      vue.watch(metadataKeysText, (value) => {
        props.info.metadataKeysText = value;
      });
      return (_ctx, _cache) => {
        const _component_magic_input = vue.resolveComponent("magic-input");
        const _component_magic_select = vue.resolveComponent("magic-select");
        const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
        return vue.openBlock(), vue.createElementBlock("div", null, [
          vue.createElementVNode("div", _hoisted_1, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.name")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.name,
              "onUpdate:value": _cache[0] || (_cache[0] = ($event) => __props.info.name = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.name"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_2, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.key")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.key,
              "onUpdate:value": _cache[1] || (_cache[1] = ($event) => __props.info.key = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.key"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_3, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.provider")), 1),
            vue.createVNode(_component_magic_select, {
              value: __props.info.provider,
              "onUpdate:value": _cache[2] || (_cache[2] = ($event) => __props.info.provider = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.provider"),
              inputable: "",
              options: providerOptions,
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_4, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.dimension")), 1),
            vue.createVNode(_component_magic_input, {
              value: __props.info.dimension,
              "onUpdate:value": _cache[3] || (_cache[3] = ($event) => __props.info.dimension = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.dimension"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_5, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.metadataKeys")), 1),
            vue.createVNode(_component_magic_input, {
              value: metadataKeysText.value,
              "onUpdate:value": _cache[4] || (_cache[4] = ($event) => metadataKeysText.value = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.metadataKeys"),
              width: "100%"
            }, null, 8, ["value", "placeholder"])
          ]),
          vue.createElementVNode("div", _hoisted_6, [
            vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("vector.datasource.form.properties")), 1),
            vue.createVNode(_component_magic_monaco_editor, {
              value: propertiesText.value,
              "onUpdate:value": _cache[5] || (_cache[5] = ($event) => propertiesText.value = $event),
              placeholder: vue.unref($i)("vector.datasource.form.placeholder.properties"),
              language: "json",
              style: { "height": "220px" }
            }, null, 8, ["value", "placeholder"])
          ])
        ]);
      };
    }
  };
  if (typeof window !== "undefined") {
    const svgBody = '<symbol viewBox="0 0 24 24" id="magic-vector-vector"><path d="M5 5a2 2 0 1 1 2.83 1.82l2.35 3.52a2.03 2.03 0 0 1 1.64 0l2.35-3.52A2 2 0 1 1 16 7l-2.35 3.52a2 2 0 0 1 0 2.96L16 17a2 2 0 1 1-1.83 1.18l-2.35-3.52a2.03 2.03 0 0 1-1.64 0l-2.35 3.52A2 2 0 1 1 6 17l2.35-3.52a2 2 0 0 1 0-2.96L6 7a2 2 0 0 1-1-2Z"/></symbol>';
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
      name: "vector",
      statusBarItems: [{
        icon: "#magic-vector-vector",
        title: "LangChain4j Vector",
        displayKey: "vector"
      }],
      datasources: [{
        type: "vector-datasource",
        icon: "#magic-vector-vector",
        title: i18n.format("vector.datasource.title"),
        name: i18n.format("vector.datasource.name"),
        service: MagicVectorDataSource(opt.request, i18n.format, opt.modal),
        component: _sfc_main
      }]
    };
  };
  return index;
}(Vue);
