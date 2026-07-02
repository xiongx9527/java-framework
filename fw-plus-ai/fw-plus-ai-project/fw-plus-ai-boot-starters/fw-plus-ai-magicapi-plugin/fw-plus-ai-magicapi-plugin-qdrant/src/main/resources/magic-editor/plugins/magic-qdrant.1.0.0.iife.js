var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-qdrant-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-qdrant-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-qdrant-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-qdrant-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-qdrant-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-qdrant-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-qdrant-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicQdrant = function(vue) {
    "use strict";
    var MagicQdrantDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo");
            return findResources && (((_a = findResources("qdrant-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/qdrant/datasource/test", info).success((res) => {
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
        qdrant: {
            datasource: {
                title: "Qdrant \u6570\u636E\u6E90",
                name: "Qdrant \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    host: "IP",
                    port: "端口",
                    utls: "安全传输层",
                    apiKey: "API Key",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        host: "请输入IP，必填",
                        port: "请输入端口，必填",
                        utls: "请选择数据源是否使用安全传输层，选填",
                        apiKey: "请输入API Key，选填",
                        other: ''
                    }
                }
            }
        }
    };
    var localEn = {
        qdrant: {
            datasource: {
                title: "Qdrant DataSource",
                name: "Qdrant",
                form: {
                    name: "Name",
                    key: "Key",
                    host: "HOST",
                    port: "PORT",
                    utls: "Secure Transport Layer",
                    apiKey: "API Key",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        host: "Please Enter Server host, Required",
                        port: "Please Enter Server port, Required",
                        utls: "Please Enter utls, Option",
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
        __name: "magic-qdrant-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.host")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).host,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).host = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.host"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.utls")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).utls,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).utls = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.utls"),
                            style: { "width": "20px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.apiKey")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).apiKey,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).apiKey = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.apiKey"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),/**
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.dbName")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).dbName,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).dbName = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.dbName"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),**/
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("qdrant.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("qdrant.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__qdrant");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__qdrant";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 174 200" id="magic-qdrant-qdrant"><path d="M173.21 50L173.211 175L140.73 193.75V68.75L86.5996 37.5L32.4805 68.75V131.25L86.5996 162.5L119.08 143.75V181.25L86.5996 200L0 150V50L86.5996 0L173.21 50ZM119.08 81.25V118.75L86.5996 137.5L54.1299 118.75V81.25L86.5996 62.5L119.08 81.25Z" fill="#6E6E6E" /></symbol>';
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
                type: "qdrant-datasource",
                icon: "#magic-qdrant-qdrant",
                title: i18n.format("qdrant.datasource.title"),
                name: i18n.format("qdrant.datasource.name"),
                service: MagicQdrantDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
