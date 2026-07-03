var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-sse-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-sse-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-ws-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-sse-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-sse-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicSse = function(vue) {
    "use strict";
    function MagicSseSubscribe(bus, constants, $i, Message, request) {
        return {
            getIcon: (item) => ["WS", "#1296db"],
            name: $i("ws.name"),
            language: "magicscript",
            defaultScript: `return null; // \u6B63\u5E38\u5904\u7406\u65F6\u8FD4\u56DEnull`,
            doTest: (opened) => {
                opened.running = true;
                const info = opened.item;
                const requestConfig = {
                    baseURL: constants.SERVER_URL,
                    url: "/sse/execute",
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
                bus.status(`\u5F00\u59CB\u6D4B\u8BD5 Ws \u8BA2\u9605\u300C${fullName}\u300D`);
                request.sendPost("/sse/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`Sse \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: true,
            merge: (item) => item
        };
    }
    var MagicSseDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseModule", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseModule");
            return findResources && (((_a = findResources("ws-datasource")[0]) == null ? void 0 : _a.children) || []).filter((it) => it.key && it.extra == "producer").map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.producer.SseProducer",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/sse/datasource/test", info).success((res) => {
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
        datasource: {
            extra: {
                producer: "\u751F\u4EA7",
                consumer: "\u6D88\u8D39"
            }
        },
        sse: {
            subscribe: {
                title: "SSE \u8BA2\u9605\u4FE1\u606F",
                name: "SSE \u8BA2\u9605",
                form: {
                    key: "数据源Key",
                    name: "名称",
                    businessId: "业务标识",
                    placeholder: {
                        key: "请输入数据源Key,必填",
                        name: "请输入名称,必填",
                        businessId:"请输入业务标识,必填",
                        description: "描述"
                    }
                }
            },
            datasource: {
                title: "SSE \u6570\u636E\u6E90",
                name: "SSE \u6570\u636E\u6E90",
                form: {
                    name: "名称",
                    key: "Key",
                    secret: "密钥",
                    mcp: "开启MCP",
                    description: "描述",
                    other: "其他配置",
                    placeholder: {
                        name: "请输入名称,必填",
                        key: "请输入Key,必填" ,
                        secret: "请输入密钥,必填",
                        description: "请输入描述",
                        other: "请输入其他配置"
                    }
                }
            }
        }
    };
    var localEn = {
        datasource: {
            extra: {
                producer: "Producer",
                consumer: "Consumer"
            }
        },
        sse: {
            subscribe: {
                title: "SSE Subscribe Info",
                name: "SSE Subscribe",
                form: {
                    key: "DataSource Key",
                    name: "Name",
                    businessId: "Business Id",
                    placeholder: {
                        key: "Please Enter DataSource Key, Required",
                        businessId: "Please Enter Business Id, Required",
                        name: "Please Enter Name, Required",
                        description: "Please Enter WS Description, Option"
                    }
                }
            },
            datasource: {
                title: "SSE DataSource",
                name: "SSE",
                form: {
                    name: "Name",
                    key: "Key",
                    secret: "Secret Key",
                    mcp: "Enable MCP",
                    description: "Description",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required" ,
                        secret: "DataSource Secret Key, Required",
                        description: "Please Enter Description, Option",
                        other: "Please Enter Other Config"
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
    const _sfc_main$1 = {
        __name: "magic-ws-datasource-info",
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
            const options = [{ text: $i("datasource.extra.producer"), value: "producer" }, { text: $i("datasource.extra.consumer"), value: "consumer" }];
            return (_ctx, _cache) => {
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("sse.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("sse.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.secret")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).secret,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).secret = $event),
                            placeholder: vue.unref($i)("sse.datasource.form.placeholder.secret"),
                            type:"password",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.mcp")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).mcp,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).mcp = $event),
                            style: { "width": "20px" }
                        }, null, 8, ["value"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.description")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).description,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).description = $event),
                            placeholder: vue.unref($i)("sse.datasource.form.placeholder.description"),
                            width: "100%",
                            style: { "height": "100px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("sse.datasource.form.placeholder.other"),
                            style: { "height": "150px" }
                        }, null, 8, ["value", "placeholder"])
                    ])
                ], 64);
            };
        }
    };
    var _export_sfc = (sfc, props) => {
        const target = sfc.__vccOpts || sfc;
        for (const [key, val] of props) {
            target[key] = val;
        }
        return target;
    };
    const _hoisted_1 = { class: "magic-ws-info" };
    const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
    const _sfc_main = {
        __name: "magic-sse-subscribe-info",
        setup(__props) {
            const $i = vue.inject("i18n.format");
            const info = vue.inject("info");
            return (_ctx, _cache) => {
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
                    vue.createElementVNode("form", null, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("message.enable")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).enabled,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).enabled = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("sse.subscribe.form.placeholder.key"),
                            width: "150px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.subscribe.form.businessId")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).businessId,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).businessId = $event),
                            placeholder: vue.unref($i)("sse.subscribe.form.placeholder.businessId"),
                            width: "150px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("sse.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("sse.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).description,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).description = $event),
                            placeholder: vue.unref($i)("sse.subscribe.form.placeholder.description")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicSseSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-4e71cade"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__sse");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__sse";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 128 128"  id="magic-sse-sse"><path d="M94.9 71.35c-8.08-7.63-21.82-13.99-30.91-18.2c-1.54-.71-2.93-1.35-4.11-1.92c-3.43-1.65-8.12-6.22-6.1-11.47c1.36-3.55 4.81-5.34 10.27-5.34c1.75 0 3.67.2 5.72.58c7.12 1.33 12.52 3.99 15.58 5.5c.38.19.82.21 1.22.07c.39-.15.72-.45.89-.84l7.01-15.81c.31-.69.07-1.51-.57-1.93c-4.94-3.28-17.27-8.15-30.97-8.15c-1.98 0-3.95.1-5.87.3c-10.92 1.12-21.86 4.03-27.92 17.64c-3.78 8.47-3.77 18.01.01 24.89c4.04 7.51 10.66 10.87 19.04 15.11l1.08.55c6.05 3.02 13.3 6.38 18.07 8.59c7.05 3.4 9.66 9.44 8.04 13.08c-2.61 5.87-8.38 7.16-18.36 4.33c-8.81-2.39-16.94-9.14-19.21-11.15c-.34-.3-.79-.44-1.25-.38c-.45.06-.85.32-1.1.69L24.6 104.14c-.41.63-.32 1.46.22 1.98c5.63 5.43 10.22 8.59 18.17 12.5c5.35 2.63 13.18 4.21 20.95 4.21c8.12 0 27.95-1.88 36.65-19.26c5.55-11.12 3.48-22.84-5.69-32.22z" fill="#40C0E7"></path></symbol>';
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
                type: "sse-subscribe",
                icon: "#magic-sse-sse",
                title: "sse.subscribe.name",
                service: MagicSseSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{
                type: "sse-datasource",
                icon: "#magic-sse-sse",
                title: i18n.format("sse.datasource.title"),
                name: i18n.format("sse.datasource.name"),
                service: MagicSseDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{
                type: "sse-subscribe",
                title: "sse.subscribe.title",
                icon: "parameter",
                component: MagicSseSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
