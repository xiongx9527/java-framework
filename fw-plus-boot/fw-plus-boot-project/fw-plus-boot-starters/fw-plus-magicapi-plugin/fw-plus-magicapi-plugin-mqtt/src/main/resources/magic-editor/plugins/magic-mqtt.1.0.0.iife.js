var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-mqtt-info[data-v-4e71cade_mqtt]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-mqtt-info form[data-v-4e71cade_mqtt]{\r\n	display: flex;\n}\n.magic-mqtt-info form label[data-v-4e71cade_mqtt]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-mqtt-info form[data-v-4e71cade_mqtt] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-mqtt-info form[data-v-4e71cade_mqtt] .magic-textarea{\r\n	margin: 5px;\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicMqtt = function(vue) {
    "use strict";
    function MagicMqttSubscribe(bus, constants, $i, Message, request) {
        return {
            getIcon: (item) => ["MQTT", "#1296db"],
            name: $i("mqtt.name"),
            language: "magicscript",
            defaultScript: `return null; // \u6B63\u5E38\u5904\u7406\u65F6\u8FD4\u56DEnull`,
            doTest: (opened) => {
                opened.running = true;
                const info = opened.item;
                const requestConfig = {
                    baseURL: constants.SERVER_URL,
                    url: "/mqtt/execute",
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
                bus.status(`\u5F00\u59CB\u6D4B\u8BD5 Mqtt \u8BA2\u9605\u300C${fullName}\u300D`);
                request.sendPost("/mqtt/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`Mqtt \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: true,
            merge: (item) => item
        };
    }
    var MagicMqttDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaModule", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaModule");
            return findResources && (((_a = findResources("kafka-datasource")[0]) == null ? void 0 : _a.children) || []).filter((it) => it.key && it.extra == "producer").map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer.KafkaProducer",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/mqtt/datasource/test", info).success((res) => {
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
        mqtt: {
            subscribe: {
                title: "Mqtt \u8BA2\u9605\u4FE1\u606F",
                name: "Mqtt \u8BA2\u9605",
                form: {
                    key: "\u6570\u636E\u6E90Key",
                    topic: "\u4E3B\u9898",
                    name: "\u540D\u79F0",
                    description: "\u63CF\u8FF0",
                    placeholder: {
                        key: "\u8BF7\u8F93\u5165\u6570\u636E\u6E90Key",
                        topic: "\u8BF7\u8F93\u5165\u8BA2\u9605\u4E3B\u9898",
                        name: "\u8BF7\u8F93\u5165\u540D\u79F0",
                        description: "\u8BF7\u8F93\u5165\u63CF\u8FF0"
                    }
                }
            },
            datasource: {
                title: "Mqtt \u6570\u636E\u6E90",
                name: "Mqtt \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    ip: "IP",
                    port: "Port",
                    webPort: "Web Port",
                    username: "用户名",
                    password: "密码",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "\u6570\u636E\u6E90\u540D\u79F0\uFF0C\u4EC5\u4F5C\u663E\u793A\u7528",
                        key: "\u6570\u636E\u6E90Key\uFF0C\u540E\u7EED\u4EE3\u7801\u4E2D\u4F7F\u7528",
                        ip: "请输入Mqtt服务ip，必填，例如：0.0.0.0",
                        port: "请输入Mqtt服务端口，必填，例如：1883",
                        webPort: "请输入Mqtt服务web端口，必填，例如：8083",
                        username: "请输入用户名,选填",
                        password: "请输入密码,选填",
                        other: '\u8BF7\u8F93\u5165 Mqtt \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        mqtt: {
            subscribe: {
                title: "Mqtt Subscribe Info",
                name: "Mqtt Subscribe",
                form: {
                    key: "DataSource Key",
                    topic: "Topic",
                    broadcast: "Broadcast",
                    name: "Name",
                    description: "Description",
                    placeholder: {
                        key: "Please Enter DataSource Key",
                        topic: "Please Enter Subscribe Topic",
                        name: "Please Enter Name",
                        description: "Please Enter Description",
                    }
                }
            },
            datasource: {
                title: "Mqtt DataSource",
                name: "Mqtt",
                form: {
                    name: "Name",
                    key: "Key",
                    ip: "IP",
                    port: "Port",
                    webPort: "Web Port",
                    username: "Username",
                    password: "Password",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        ip: "Please Enter Mqtt IP, Required ,For example: 0.0.0.0 ",
                        port: "Please Enter Mqtt Port,Required ,For example: 1883",
                        webPort: "Please Enter Mqtt Web Port,Required,For example: 8083",
                        username: "Please Enter Username, Optional",
                        password: "Please Enter Password, Optional",
                        other: 'Please Enter Mqtt Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-kafka-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.ip")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).ip,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).ip = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.ip"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),

                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.webPort")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).webPort,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).webPort = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.webPort"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.password"),
                            type: "password",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("mqtt.datasource.form.placeholder.other"),
                            language: "json",
                            style: { "height": "150px" }
                        }, null, 8, ["value", "placeholder"])
                    ])
                ], 64);
            };
        }
    };
    var magicMqttSubscribeInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
    var _export_sfc = (sfc, props) => {
        const target = sfc.__vccOpts || sfc;
        for (const [key, val] of props) {
            target[key] = val;
        }
        return target;
    };
    const _hoisted_1 = { class: "magic-mqtt-info" };
    const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
    const _sfc_main = {
        __name: "magic-mqtt-subscribe-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("mqtt.subscribe.form.placeholder.key"),
                            width: "300px"
                        }, null, 8, ["value", "placeholder"]),
                        /**
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.subscribe.form.broadcast")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).broadcast,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).broadcast = $event)
                        }, null, 8, ["value"]),
                            **/
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.subscribe.form.topic")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).topic,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).topic = $event),
                            placeholder: vue.unref($i)("mqtt.subscribe.form.placeholder.topic"),
                            width: "300px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mqtt.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("mqtt.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).description,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).description = $event),
                            placeholder: vue.unref($i)("mqtt.subscribe.form.placeholder.description")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicMqttSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-4e71cade_mqtt"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__mqtt");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__mqtt";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 24 24"  id="magic-mqtt-mqtt"><path d="M11.987 1.913c-1.9 0-3.693.321-5.298.883C2.756 4.268 0 7.826 0 12c0 4.147 2.756 7.706 6.689 9.204 1.632.562 3.425.883 5.325.883a16.74 16.74 0 0 0 5.27-.856C21.217 19.759 24 16.174 24 12.027V12c0-4.174-2.783-7.732-6.716-9.204a16.295 16.295 0 0 0-5.297-.883zM10.89 5.257h2.167v3.827c1.525-.402 2.702-1.766 2.782-3.399l2.168.027c-.16 2.73-2.22 4.95-4.897 5.378v1.793c2.676.428 4.736 2.675 4.924 5.404l-2.167.028c-.08-1.633-1.258-2.997-2.783-3.425v3.853h-2.167V14.89a3.775 3.775 0 0 0-2.81 3.425l-2.167-.028a5.868 5.868 0 0 1 4.923-5.404v-1.766C8.187 10.716 6.1 8.468 5.94 5.74l2.167-.027A3.711 3.711 0 0 0 10.89 9.11Z"/></symbol>';
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
                type: "mqtt-subscribe",
                icon: "#magic-mqtt-mqtt",
                title: "mqtt.subscribe.name",
                service: MagicMqttSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{
                type: "mqtt-datasource",
                icon: "#magic-mqtt-mqtt",
                title: i18n.format("mqtt.datasource.title"),
                name: i18n.format("mqtt.datasource.name"),
                service: MagicMqttDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{
                type: "mqtt-subscribe",
                title: "mqtt.subscribe.title",
                icon: "parameter",
                component: MagicMqttSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
