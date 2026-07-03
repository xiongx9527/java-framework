var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-rabbitmq-info[data-v-267a8f7b]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-rabbitmq-info form[data-v-267a8f7b]{\r\n	display: flex;\n}\n.magic-rabbitmq-info form label[data-v-267a8f7b]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-rabbitmq-info form[data-v-267a8f7b] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-rabbitmq-info form[data-v-267a8f7b] .magic-textarea{\r\n	margin: 5px;\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicRabbitMq = function(vue) {
    "use strict";
    function MagicRabbitMqSubscribe(bus, constants, $i, Message, request) {
        return {
            getIcon: (item) => ["RAMQ", "#1296db"],
            name: $i("rabbitmq.name"),
            language: "magicscript",
            defaultScript: `return null; // \u6B63\u5E38\u5904\u7406\u65F6\u8FD4\u56DEnull`,
            doTest: (opened) => {
                opened.running = true;
                const info = opened.item;
                const requestConfig = {
                    baseURL: constants.SERVER_URL,
                    url: "/rabbitmq/subscribe/execute",
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
                bus.status(`\u5F00\u59CB\u6D4B\u8BD5 RabbitMq \u8BA2\u9605\u300C${fullName}\u300D`);
                request.sendPost("/rabbitmq/subscribe/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`RabbitMq \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: false,
            merge: (item) => item
        };
    }
    var MagicRabbitMqDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module.RabbitMqModule", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module.RabbitMqModule");
            return findResources && (((_a = findResources("rabbitmq-datasource")[0]) == null ? void 0 : _a.children) || []).filter((it) => it.key && it.extra == "producer").map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/rabbitmq/datasource/test", info).success((res) => {
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
        rabbitmq: {
            subscribe: {
                title: "RabbitMq \u8BA2\u9605\u4FE1\u606F",
                name: "RabbitMq \u8BA2\u9605",
                form: {
                    key: "\u6570\u636E\u6E90Key",
                    queueName: "\u961F\u5217",
                    name: "\u540D\u79F0",
                    broadcast: "广播",
                    path: "\u8DEF\u5F84",
                    placeholder: {
                        key: "\u8BF7\u8F93\u5165\u6570\u636E\u6E90Key",
                        queueName: "\u8BF7\u8F93\u5165\u8BA2\u9605\u961F\u5217",
                        name: "\u8BF7\u8F93\u5165\u540D\u79F0",
                        path: "\u8BF7\u8F93\u5165\u8DEF\u5F84",
                        other: "\u8BF7\u8F93\u5165 RabbitMq 其它配置",
                        params: "\u8BF7\u8F93\u5165 RabbitMq \u53C2\u6570"
                    }
                }
            },
            datasource: {
                title: "RabbitMq \u6570\u636E\u6E90",
                name: "RabbitMq \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    host: "\u670D\u52A1\u5668\u5730\u5740",
                    port: "\u670D\u52A1\u5668\u7AEF\u53E3",
                    username: "\u7528\u6237\u540D",
                    password: "\u5BC6\u7801",
                    extra: "\u7C7B\u578B",
                    virtualHost: "\u865A\u62DF\u4E3B\u673A",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "\u6570\u636E\u6E90\u540D\u79F0\uFF0C\u4EC5\u4F5C\u663E\u793A\u7528",
                        key: "\u6570\u636E\u6E90Key\uFF0C\u540E\u7EED\u4EE3\u7801\u4E2D\u4F7F\u7528",
                        host: "\u8BF7\u8F93\u5165\u670D\u52A1\u5668\u5730\u5740\uFF0C\u5982\uFF1A192.168.2.5",
                        port: "\u8BF7\u8F93\u5165\u670D\u52A1\u5668\u7AEF\u53E3\uFF0C\u5982\uFF1A5672",
                        username: "\u8BF7\u8F93\u5165 RabbitMq \u8BBF\u95EE\u7528\u6237\u540D\uFF0C\u53EF\u9009",
                        password: "\u8BF7\u8F93\u5165 RabbitMq \u8BBF\u95EE\u5BC6\u7801\uFF0C\u53EF\u9009",
                        extra: "\u8BF7\u9009\u62E9 RabbitMq \u8BBF\u95EE\u7C7B\u578B",
                        virtualHost: "\u8BF7\u8F93\u5165 RabbitMq \u8BBF\u95EE\u865A\u62DF\u4E3B\u673A\uFF0C \u53EF\u9009",
                        other: '\u8BF7\u8F93\u5165 RabbitMq \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        rabbitmq: {
            subscribe: {
                title: "RabbitMq Subscribe Info",
                name: "RabbitMq Subscribe",
                form: {
                    key: "DataSource Key",
                    queueName: "Queue Name",
                    name: "Name",
                    path: "Path",
                    broadcast: "Broadcast",
                    placeholder: {
                        key: "Please Enter DataSource Key",
                        queueName: "Please Enter Subscribe Queue Name",
                        name: "Please Enter Name",
                        path: "Please Enter Path",
                        other: "Please Enter RabbitMq other config",
                        params: "Please Enter Subscribe Params"
                    }
                }
            },
            datasource: {
                title: "RabbitMq DataSource",
                name: "RabbitMq",
                form: {
                    name: "Name",
                    key: "Key",
                    host: "Server Host",
                    port: "Server Port",
                    username: "User Name",
                    password: "Password",
                    extra: "Type",
                    virtualHost: "Virtual Host",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        host: "Please Enter Server Host\uFF0Ceg\uFF1A192.168.2.5,...",
                        port: "Please Enter Server Port\uFF0Ceg\uFF1A5672,...",
                        username: "Please Enter Datasource username, Optional",
                        password: "Please Enter Datasource password, Optional",
                        extra: "Please Enter RabbitMq Access Type",
                        virtualHost: "Please Enter RabbitMq Virtual Host, Optional",
                        other: 'Please Enter RabbitMq Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
    const _sfc_main$1 = {
        __name: "magic-rabbitmq-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.host")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).host,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).host = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.host"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).password = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.password"),
                            type: "password",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    /**
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.extra")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).extra,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).extra = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.extra"),
                            inputable: "",
                            options,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),**/
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.virtualHost")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).virtualHost,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).virtualHost = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.virtualHost"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("rabbitmq.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("rabbitmq.datasource.form.placeholder.other"),
                            language: "json",
                            style: { "height": "150px" }
                        }, null, 8, ["value", "placeholder"])
                    ])
                ], 64);
            };
        }
    };
    var magicRabbitmqSubscribeInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
    var _export_sfc = (sfc, props) => {
        const target = sfc.__vccOpts || sfc;
        for (const [key, val] of props) {
            target[key] = val;
        }
        return target;
    };
    const _hoisted_1 = { class: "magic-rabbitmq-info" };
    const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
    const _sfc_main = {
        __name: "magic-rabbitmq-subscribe-info",
        setup(__props) {
            const $i = vue.inject("i18n.format");
            const info = vue.inject("info");
            return (_ctx, _cache) => {
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
                    vue.createElementVNode("form", null, [
                        vue.createElementVNode("label", { style: { "width": "70px" } }, vue.toDisplayString(vue.unref($i)("message.enable")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).enabled,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).enabled = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", { style: { "width": "70px" } }, vue.toDisplayString(vue.unref($i)("rabbitmq.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("rabbitmq.subscribe.form.placeholder.key"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", { style: { "width": "70px" } }, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.broadcast")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).broadcast,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).broadcast = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", { style: { "width": "70px" } }, vue.toDisplayString(vue.unref($i)("rabbitmq.subscribe.form.queueName")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).queueName,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).queueName = $event),
                            placeholder: vue.unref($i)("rabbitmq.subscribe.form.placeholder.queueName"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", { style: { "width": "70px" } }, vue.toDisplayString(vue.unref($i)("rabbitmq.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("rabbitmq.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("rabbitmq.subscribe.form.placeholder.other")
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    // 添加 style 控制高度一致
                    vue.createElementVNode("div", {
                        ..._hoisted_3,
                        style: { ..._hoisted_3.style, flex: 1 }
                    }, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).params,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).params = $event),
                            placeholder: vue.unref($i)("rabbitmq.subscribe.form.placeholder.params")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicRabbitMqSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-267a8f7b"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728630221__");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728630221__";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 1024 1024"  id="magic-rabbitmq-rabbitmq"><path d="M824.363 439.147H597.312a20.736 20.736 0 0 1-20.48-20.587V180.928a31.488 31.488 0 0 0-9.173-22.379 31.232 31.232 0 0 0-22.272-9.216h-81.259a31.232 31.232 0 0 0-22.272 9.216 31.467 31.467 0 0 0-9.152 22.379v235.968a22.4 22.4 0 0 1-22.336 22.25l-74.24.62a22.187 22.187 0 0 1-22.123-22.251l.619-236.587a31.488 31.488 0 0 0-9.173-22.379 31.253 31.253 0 0 0-22.294-9.216h-81.066a31.147 31.147 0 0 0-22.251 9.216 31.488 31.488 0 0 0-9.173 22.379v665.45c0 15.809 12.821 28.289 28.117 28.289h626.41c15.723 0 28.14-12.886 28.14-28.288V467.2c-1.025-15.808-13.249-28.053-28.971-28.053zM718.08 689.237a36.951 36.951 0 0 1-10.73 26.219 36.544 36.544 0 0 1-26.091 10.795h-64.107a36.565 36.565 0 0 1-26.09-10.795 36.95 36.95 0 0 1-10.731-26.219v-64.661a36.95 36.95 0 0 1 10.73-26.219 36.48 36.48 0 0 1 26.091-10.773h64.107a36.65 36.65 0 0 1 36.821 36.992v64.661zm0 0" /></symbol>';
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
                type: "rabbitmq-subscribe",
                icon: "#magic-rabbitmq-rabbitmq",
                title: "rabbitmq.subscribe.name",
                service: MagicRabbitMqSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{
                type: "rabbitmq-datasource",
                icon: "#magic-rabbitmq-rabbitmq",
                title: i18n.format("rabbitmq.datasource.title"),
                name: i18n.format("rabbitmq.datasource.name"),
                service: MagicRabbitMqDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{
                type: "rabbitmq-subscribe",
                title: "rabbitmq.subscribe.title",
                icon: "parameter",
                component: MagicRabbitMqSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
