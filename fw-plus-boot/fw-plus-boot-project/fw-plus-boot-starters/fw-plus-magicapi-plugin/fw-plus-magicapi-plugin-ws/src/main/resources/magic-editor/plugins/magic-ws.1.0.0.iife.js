var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-ws-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-ws-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-ws-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-ws-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-ws-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicWs = function(vue) {
    "use strict";
    function MagicWsSubscribe(bus, constants, $i, Message, request) {
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
                    url: "/ws/execute",
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
                request.sendPost("/ws/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`Ws \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: true,
            merge: (item) => item
        };
    }
    var MagicWsDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsModule", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsModule");
            return findResources && (((_a = findResources("ws-datasource")[0]) == null ? void 0 : _a.children) || []).filter((it) => it.key && it.extra == "producer").map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.producer.WsProducer",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/ws/datasource/test", info).success((res) => {
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
        ws: {
            subscribe: {
                title: "WS \u8BA2\u9605\u4FE1\u606F",
                name: "WS \u8BA2\u9605",
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
                title: "WS \u6570\u636E\u6E90",
                name: "WS \u6570\u636E\u6E90",
                form: {
                    name: "名称",
                    key: "Key",
                    secret: "密钥",
                    description: "描述",
                    other: "其它配置",
                    placeholder: {
                        name: "请输入名称,必填",
                        key: "请输入Key,必填" ,
                        secret: "请输入密钥,必填",
                        description: "请输入描述",
                        other: "请输入其它配置"
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
        ws: {
            subscribe: {
                title: "WS Subscribe Info",
                name: "WS Subscribe",
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
                title: "WS DataSource",
                name: "WS",
                form: {
                    name: "Name",
                    key: "Key",
                    secret: "Secret Key",
                    description: "Description",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required" ,
                        secret: "DataSource Secret Key, Required",
                        description: "Please Enter WS Description, Option",
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
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("ws.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("ws.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.datasource.form.secret")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).secret,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).secret = $event),
                            placeholder: vue.unref($i)("ws.datasource.form.placeholder.secret"),
                            type:"password",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.datasource.form.description")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).description,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).description = $event),
                            placeholder: vue.unref($i)("ws.datasource.form.placeholder.description"),
                            width: "100%",
                            style: { "height": "100px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("ws.datasource.form.placeholder.other"),
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
        __name: "magic-ws-subscribe-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("ws.subscribe.form.placeholder.key"),
                            width: "150px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.subscribe.form.businessId")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).businessId,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).businessId = $event),
                            placeholder: vue.unref($i)("ws.subscribe.form.placeholder.businessId"),
                            width: "150px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("ws.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("ws.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).description,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).description = $event),
                            placeholder: vue.unref($i)("ws.subscribe.form.placeholder.description")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicWsSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-4e71cade"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__ws");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__ws";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 222 97"  id="magic-ws-ws"><path d="M221.875 69.2188C221.875 76.0938 219.427 81.9271 214.531 86.7188C207.448 93.4896 197.292 96.875 184.062 96.875C172.708 96.875 163.021 94.2188 155 88.9062C155 90.6771 154.896 92.0833 154.688 93.125C154.167 94.7917 152.5 95.625 149.688 95.625V62.5H155.781C156.198 68.0208 157.344 72.9167 159.219 77.1875C163.698 87.2917 171.458 92.3438 182.5 92.3438C185.417 92.3438 188.073 91.9271 190.469 91.0938C197.135 88.5938 200.469 83.5938 200.469 76.0938C200.469 73.9062 200 71.7708 199.062 69.6875C196.875 65.3125 191.302 60.7812 182.344 56.0938C172.344 50.9896 166.719 48.0208 165.469 47.1875C155.573 41.0417 150.625 33.6458 150.625 25C150.625 19.1667 152.656 14.1146 156.719 9.84375C163.073 3.28125 172.552 0 185.156 0C194.74 0 203.854 2.03125 212.5 6.09375C212.5 5.15625 212.552 4.32292 212.656 3.59375C213.177 1.61458 214.896 0.625 217.812 0.625V28.9062H211.719C211.615 23.2812 210.26 18.4375 207.656 14.375C203.385 7.91667 196.979 4.6875 188.438 4.6875C186.25 4.6875 184.167 5 182.188 5.625C176.25 7.60417 173.281 11.6667 173.281 17.8125C173.281 20.2083 173.802 22.5 174.844 24.6875C177.031 29.1667 182.5 33.6979 191.25 38.2812C200.833 43.3854 206.302 46.3021 207.656 47.0312C217.135 53.1771 221.875 60.5729 221.875 69.2188Z" fill="#6E6E6E"/>' +
                '<path d="M103.438 95.3125H100.312C98.5417 95.3125 97.0833 95 95.9375 94.375C94.0625 93.3333 91.875 89.6354 89.375 83.2812L72.5 39.375L51.875 95.3125H48.75C46.9792 95.3125 45.5208 95 44.375 94.375C42.5 93.3333 40.3125 89.6354 37.8125 83.2812L7.8125 5.78125H0V1.5625H42.3438V5.78125H29.8438L54.375 74.0625L69.6875 32.3438L57.8125 1.5625H94.0625V5.78125L81.5625 5.625L105.781 72.6562L127.812 5.78125H115.312V1.5625H141.094V5.78125H133.281L103.438 95.3125Z" fill="#6E6E6E"/></symbol>';
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
                type: "ws-subscribe",
                icon: "#magic-ws-ws",
                title: "ws.subscribe.name",
                service: MagicWsSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{
                type: "ws-datasource",
                icon: "#magic-ws-ws",
                title: i18n.format("ws.datasource.title"),
                name: i18n.format("ws.datasource.name"),
                service: MagicWsDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{
                type: "ws-subscribe",
                title: "ws.subscribe.title",
                icon: "parameter",
                component: MagicWsSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
