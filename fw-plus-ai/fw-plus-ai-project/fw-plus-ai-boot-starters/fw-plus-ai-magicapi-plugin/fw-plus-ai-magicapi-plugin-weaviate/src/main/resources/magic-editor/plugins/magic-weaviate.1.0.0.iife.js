var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-weaviate-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-weaviate-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-weaviate-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-weaviate-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-weaviate-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-weaviate-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-weaviate-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicWeaviate = function(vue) {
    "use strict";
    var MagicWeaviateDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo");
            return findResources && (((_a = findResources("weaviate-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/weaviate/datasource/test", info).success((res) => {
                    if (res === "ok") {
                        modal.alert($i("datasource.connected"), $i("datasource.test"));
                    } else {
                        modal.alert($i("datasource.connectFailed", res), $i("datasource.test"));
                    }
                });
            }
        };
    };
    var localZhCN = {
        weaviate: {
            datasource: {
                title: "Weaviate \u6570\u636E\u6E90",
                name: "Weaviate \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    host: "Host",
                    scheme: "协议",
                    connectionTimeout: "连接超时",
                    connectionRequestTimeout: "请求超时",
                    socketTimeout: "读取超时",
                    apiKey: "API Key",
                    apiKeyName: "API Key Name",
                    apiKeyValue: "API Key Value",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        host: "请输入Host，必填 ，例如：localhost:8080",
                        scheme: "请输入协议，必填",
                        connectionTimeout: "请输入连接超时时间，默认60秒，选填",
                        connectionRequestTimeout: "请输入请求超时时间，默认60秒，选填",
                        socketTimeout: "请输入读取超时时间，默认60秒，选填",
                        apiKey: "请输入API Key，选填",
                        other: ''
                    }
                }
            }
        }
    };
    var localEn = {
        weaviate: {
            datasource: {
                title: "Weaviate DataSource",
                name: "Weaviate",
                form: {
                    name: "Name",
                    key: "Key",
                    host: "HOST",
                    scheme: "Scheme",
                    connectionTimeout: "ConnectionTimeout",
                    connectionRequestTimeout: "ConnectionRequestTimeout",
                    socketTimeout: "SocketTimeout",
                    apiKey: "API Key",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        scheme: "Please Enter Scheme, Required",
                        host: "Please Enter Server host, Required，Example：localhost:8080",
                        connectionTimeout: "Please Enter ConnectionTimeout, Default 60 seconds,Option",
                        connectionRequestTimeout: "Please Enter ConnectionRequestTimeout, Default 60 seconds,Option",
                        socketTimeout: "Please Enter SocketTimeout, Default 60 seconds,Option",
                        apiKey: "Please Enter API Key, Option",
                        other: ''
                    }
                }
            }
        }
    };
    const _hoisted_1$1 = { class: "magic-form-row" };
    const _hoisted_2$1 = { class: "magic-form-row" };
    const _hoisted_3 = { class: "magic-form-row" };
    const _hoisted_4 = { class: "magic-form-row" };
    const _hoisted_5 = { class: "magic-form-row" };
    const _hoisted_6 = { class: "magic-form-row" };
    const _hoisted_7 = { class: "magic-form-row" };
    const _hoisted_8 = { class: "magic-form-row" };

    const _hoisted_9 = { class: "magic-form-row" };
    const _hoisted_10 = { class: "magic-form-row" };
    const _hoisted_11 = { class: "magic-form-row" };
    const _hoisted_12 = { class: "magic-form-row" };
    const _hoisted_13 = { class: "magic-form-row" };
    const _hoisted_14 = { class: "magic-form-row" };
    const _sfc_main$1 = {
        __name: "magic-weaviate-datasource-info",
        props: {
            info: {
                type: Object,
                default: {
                    other: "{}"
                }
            }
        },
        setup(__props) {
            const props = __props;
            const $i = vue.inject("i18n.format");
            const info = props.info;
            return (_ctx, _cache) => {
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.scheme")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).scheme,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).scheme = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.scheme"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.host")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).host,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).host = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.host"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.connectionTimeout")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).connectionTimeout,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).connectionTimeout = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.connectionTimeout"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.connectionRequestTimeout")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).connectionRequestTimeout,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).connectionRequestTimeout = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.connectionRequestTimeout"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.socketTimeout")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).socketTimeout,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).socketTimeout = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.socketTimeout"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.apiKey")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).apiKey,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).apiKey = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.apiKey"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("weaviate.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("weaviate.datasource.form.placeholder.other"),
                            language: "json",
                            style: { "height": "150px" }
                        }, null, 8, ["value", "placeholder"])
                    ])

                ], 64);
            };
        }
    };
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__weaviate");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__weaviate";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol viewBox="0 0 64 64" id="magic-weaviate-weaviate">' +
              '<path d="M32 2C15.432 2 2 15.432 2 32s13.432 30 30 30s30-13.432 30-30S48.568 2 32 2m10.279 44.508h-5.807l-3.504-16.969l-1.023-5.61l-1.023 5.61l-3.504 16.969h-5.631l-8.229-29.016h6.438l3.832 16.615l.834 4.625l.836-4.529l3.277-16.711h6.398l3.447 16.613l.883 4.627l.896-4.447l3.867-16.793h6.174l-8.161 29.016" fill="#000000"></path></symbol>';
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
            datasources: [{
                type: "weaviate-datasource",
                icon: "#magic-weaviate-weaviate",
                title: i18n.format("weaviate.datasource.title"),
                name: i18n.format("weaviate.datasource.name"),
                service: MagicWeaviateDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
