var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-pulsar-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-pulsar-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-pulsar-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-pulsar-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-pulsar-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicPulsar = function(vue) {
    "use strict";
    function MagicPulsarSubscribe(bus, constants, $i, Message, request) {
        return {
            getIcon: (item) => ["PLS", "#1296db"],
            name: $i("pulsar.name"),
            language: "magicscript",
            defaultScript: `return null; // \u6B63\u5E38\u5904\u7406\u65F6\u8FD4\u56DEnull`,
            doTest: (opened) => {
                opened.running = true;
                const info = opened.item;
                const requestConfig = {
                    baseURL: constants.SERVER_URL,
                    url: "/pulsar/execute",
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
                bus.status(`\u5F00\u59CB\u6D4B\u8BD5 Pulsar \u8BA2\u9605\u300C${fullName}\u300D`);
                request.sendPost("/pulsar/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`Pulsar \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: true,
            merge: (item) => item
        };
    }
    var MagicPulsarDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.module.PulsarModule", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.module.PulsarModule");
            return findResources && (((_a = findResources("pulsar-datasource")[0]) == null ? void 0 : _a.children) || []).filter((it) => it.key && it.extra == "producer").map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.producer.PulsarProducer",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            requireScript: false,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/pulsar/datasource/test", info).success((res) => {
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
        pulsar: {
            subscribe: {
                title: "Pulsar \u8BA2\u9605\u4FE1\u606F",
                name: "Pulsar \u8BA2\u9605",
                form: {
                    key: "\u6570\u636E\u6E90Key",
                    topic: "\u4E3B\u9898",
                    broadcast:"广播",
                    batchIndexAcknowledgmentEnabled: "批量确认",
                    name: "\u540D\u79F0",
                    path: "\u8DEF\u5F84",
                    placeholder: {
                        key: "\u8BF7\u8F93\u5165\u6570\u636E\u6E90Key",
                        topic: "\u8BF7\u8F93\u5165\u8BA2\u9605\u4E3B\u9898",
                        group:"请输入订阅分组",
                        name: "\u8BF7\u8F93\u5165\u540D\u79F0",
                        path: "\u8BF7\u8F93\u5165\u8DEF\u5F84",
                        other: "请输入Pulsar其它json配置" ,
                        params:  "请输入Pulsar订阅消息"
                    }
                }
            },
            datasource: {
                title: "Pulsar \u6570\u636E\u6E90",
                name: "Pulsar \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    servers: "\u670D\u52A1\u5668\u5730\u5740",
                    username: "\u7528\u6237\u540D",
                    password: "\u5BC6\u7801",
                    tenant: "租户",
                    namespace: "命名空间",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "\u6570\u636E\u6E90\u540D\u79F0\uFF0C\u4EC5\u4F5C\u663E\u793A\u7528",
                        key: "\u6570\u636E\u6E90Key\uFF0C\u540E\u7EED\u4EE3\u7801\u4E2D\u4F7F\u7528",
                        servers: "\u8BF7\u8F93\u5165 Pulsar \u670D\u52A1\u5668\u5730\u5740\uFF0C\u5982\uFF1A127.0.0.1:6650,...",
                        username: "\u8BF7\u8F93\u5165 Pulsar \u8BBF\u95EE\u7528\u6237\u540D\uFF0C\u53EF\u9009",
                        password: "\u8BF7\u8F93\u5165 Pulsar \u8BBF\u95EE\u5BC6\u7801\uFF0C\u53EF\u9009",
                        tenant: "\u8BF7\u9009\u62E9 Pulsar 租户",
                        namespace: "\u8BF7\u8F93\u5165 Pulsar 命名空间",
                        other: '\u8BF7\u8F93\u5165 Pulsar \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
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
        pulsar: {
            subscribe: {
                title: "Pulsar Subscribe Info",
                name: "Pulsar Subscribe",
                form: {
                    key: "DataSource Key",
                    topic: "Topic",
                    broadcast:"Broadcast",
                    batchIndexAcknowledgmentEnabled: "Batch Index Acknowledgment Enabled",
                    name: "Name",
                    path: "Path",
                    placeholder: {
                        key: "Please Enter DataSource Key",
                        topic: "Please Enter Subscribe Topic",
                        group: "Please Enter Subscribe Group",
                        name: "Please Enter Name",
                        path: "Please Enter Path",
                        other: "Please Enter Pulsar Subscribe Other json Config" ,
                        params: "Please Enter Pulsar Subscribe Params"
                    }
                }
            },
            datasource: {
                title: "Pulsar DataSource",
                name: "Pulsar",
                form: {
                    name: "Name",
                    key: "Key",
                    servers: "Server Addresses",
                    username: "User Name",
                    password: "Password",
                    tenant: "Tenant",
                    namespace: "Namespace",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        servers: "Please Enter Pulsar Server Addresses\uFF0Ceg\uFF1A127.0.0.1:6650,...",
                        username: "Please Enter Pulsar username, Optional",
                        password: "Please Enter Pulsar password, Optional",
                        tenant: "Please Enter Pulsar Tenant",
                        namespace: "Please Enter Pulsar Namespace",
                        other: 'Please Enter Pulsar Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-pulsar-datasource-info",
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
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.servers")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).servers,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).servers = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.servers"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),/**
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).password = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),**/
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.tenant")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).tenant,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).tenant = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.tenant"),
                            inputable: "",
                            options,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.namespace")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).namespace,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).namespace = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.namespace"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("pulsar.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("pulsar.datasource.form.placeholder.other"),
                            language: "json",
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
    const _hoisted_1 = { class: "magic-pulsar-info" };
    const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
    const _sfc_main = {
        __name: "magic-pulsar-subscribe-info",
        setup(__props) {
            const $i = vue.inject("i18n.format");
            const info = vue.inject("info");
            return (_ctx, _cache) => {
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
                    vue.createElementVNode("form", null, [
                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("message.enable")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).enabled,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).enabled = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("pulsar.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.key"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("pulsar.subscribe.form.broadcast")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).broadcast,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).broadcast = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("pulsar.subscribe.form.topic")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).topic,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).topic = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.topic"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),


                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("pulsar.subscribe.form.batchIndexAcknowledgmentEnabled")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).batchIndexAcknowledgmentEnabled,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).batchIndexAcknowledgmentEnabled = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.batchIndexAcknowledgmentEnabled"),
                        }, null, 8, ["value", "placeholder"]),


                        vue.createElementVNode("label", { style: { width: "70px" } }, vue.toDisplayString(vue.unref($i)("pulsar.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.other")
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    // 添加 style 控制高度一致
                    vue.createElementVNode("div", {
                        ..._hoisted_3,
                        style: { ..._hoisted_3.style, flex: 1 }
                    }, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).params,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).params = $event),
                            placeholder: vue.unref($i)("pulsar.subscribe.form.placeholder.params")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicPulsarSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-4e71cade"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__pulsar");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__pulsar";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 1024 1024"  id="magic-pulsar-pulsar"><path d="M1185.553067 271.428267h-289.6896C817.493333 118.6816 734.651733 0 621.909333 0c-146.773333 0-215.722667 130.218667-282.385066 257.058133l-5.802667 11.127467H0v75.332267h194.389333c68.608 0 82.602667 23.517867 82.602667 23.517866-53.828267 84.992-118.5792 169.0624-226.5088 169.0624H0v78.984534h50.926933c209.169067 0 300.475733-172.578133 373.828267-311.125334 67.242667-127.6928 114.141867-204.970667 197.290667-204.970666 86.357333 0 177.083733 191.044267 256.9216 359.697066 11.605333 24.4736 23.552 49.5616 35.566933 74.478934-306.312533 9.8304-452.266667 82.773333-571.016533 142.336-86.869333 43.485867-155.477333 97.416533-268.3904 97.416533H0v79.121067h74.410667c136.021333 0 220.842667-42.666667 311.057066-87.995734 124.928-62.498133 266.581333-133.3248 600.576-133.3248h193.024V551.594667h-109.021866c-9.0112 0.682667-17.988267-0.682667-26.282667-4.232534a57.2416 57.2416 0 0 1-21.742933-15.633066 3481.6 3481.6 0 0 1-56.0128-116.701867l-25.326934-52.6336s18.090667-19.729067 68.1984-19.729067h176.674134V271.428267z" /></symbol>';
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
                type: "pulsar-subscribe",
                icon: "#magic-pulsar-pulsar",
                title: "pulsar.subscribe.name",
                service: MagicPulsarSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{
                type: "pulsar-datasource",
                icon: "#magic-pulsar-pulsar",
                title: i18n.format("pulsar.datasource.title"),
                name: i18n.format("pulsar.datasource.name"),
                service: MagicPulsarDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{
                type: "pulsar-subscribe",
                title: "pulsar.subscribe.title",
                icon: "parameter",
                component: MagicPulsarSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
