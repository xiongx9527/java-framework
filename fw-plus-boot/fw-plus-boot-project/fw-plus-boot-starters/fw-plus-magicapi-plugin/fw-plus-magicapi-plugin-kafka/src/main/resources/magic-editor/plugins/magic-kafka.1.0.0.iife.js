var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-kafka-info[data-v-4e71cade]{\r\n\tdisplay: flex;\r\n\tflex-direction: column;\r\n\tflex: 1;\r\n\tpadding: 2px;\n}\n.magic-kafka-info form[data-v-4e71cade]{\r\n\tdisplay: flex;\r\n\tmargin: 0;\r\n\tpadding: 0;\n}\n.magic-kafka-info form label[data-v-4e71cade]{\r\n\tdisplay: inline-block;\r\n\twidth: 70px;\r\n\theight: var(--magic-input-height);\r\n\tline-height: var(--magic-input-height);\r\n\tfont-weight: 400;\r\n\ttext-align: right;\r\n\tpadding: 0 2px;\r\n\tmargin-right: 2px;\r\n\tfont-size: 12px;\n}\n.magic-kafka-info form[data-v-4e71cade] input,\r\n.magic-kafka-info form[data-v-4e71cade] select,\r\n.magic-kafka-info form[data-v-4e71cade] .magic-checkbox{\r\n\tmargin-right: 2px;\r\n\tpadding: 0;\n}\n.magic-kafka-info form[data-v-4e71cade] .magic-checkbox{\r\n\twidth: 22px;\r\n\theight: 22px;\r\n\tmargin-right: 2px;\r\n\tmargin-left: 0;\r\n}\n.magic-kafka-info form[data-v-4e71cade] .magic-textarea{\r\n\tmargin: 2px 0;\r\n\tpadding: 0;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicKafka = function(vue) {
    "use strict";
    function MagicKafkaSubscribe(bus, constants, $i, Message, request) {
        return {
            getIcon: (item) => ["KFK", "#1296db"],
            name: $i("kafka.name"),
            language: "magicscript",
            defaultScript: `return null; // \u6B63\u5E38\u5904\u7406\u65F6\u8FD4\u56DEnull`,
            doTest: (opened) => {
                opened.running = true;
                const info = opened.item;
                const requestConfig = {
                    baseURL: constants.SERVER_URL,
                    url: "/kafka/execute",
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
                bus.status(`\u5F00\u59CB\u6D4B\u8BD5 Kafka \u8BA2\u9605\u300C${fullName}\u300D`);
                request.sendPost("/kafka/execute", { id: info.id }, requestConfig).success((res) => {
                    opened.running = false;
                }).end(() => {
                    bus.status(`Kafka \u8BA2\u9605\u300C${fullName}\u300D\u6D4B\u8BD5\u5B8C\u6BD5`);
                    opened.running = false;
                });
            },
            runnable: true,
            requirePath: true,
            merge: (item) => item
        };
    }
    var MagicKafkaDataSource = (request, $i, modal, JavaClass) => {
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
                request.sendJson("/kafka/datasource/test", info).success((res) => {
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
        kafka: {
            subscribe: {
                title: "Kafka \u8BA2\u9605\u4FE1\u606F",
                name: "Kafka \u8BA2\u9605",
                form: {
                    key: "\u6570\u636E\u6E90Key",
                    topic: "\u4E3B\u9898",
                    broadcast: "\u5E7F\u64AD",
                    ackMode: "\u786E\u8BA4\u65B9\u5F0F",
                    concurrency: "\u5E76\u53D1\u6570",
                    name: "\u540D\u79F0",
                    path: "\u8DEF\u5F84",
                    placeholder: {
                        key: "\u8BF7\u8F93\u5165\u6570\u636E\u6E90Key",
                        topic: "\u8BF7\u8F93\u5165\u8BA2\u9605\u4E3B\u9898",
                        name: "\u8BF7\u8F93\u5165\u540D\u79F0",
                        path: "\u8BF7\u8F93\u5165\u8DEF\u5F84",
                        params: "\u8BF7\u8F93\u5165 Kafka \u8BA2\u9605\u6D88\u606F",
                        other: "\u8BF7\u8F93\u5165Kafka \u8BA2\u9605\u5176\u4ED6JSON\u914D\u7F6E"
                    }
                }
            },
            datasource: {
                title: "Kafka \u6570\u636E\u6E90",
                name: "Kafka \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    servers: "\u670D\u52A1\u5668\u5730\u5740",
                    username: "\u7528\u6237\u540D",
                    password: "\u5BC6\u7801",
                    extra: "\u7C7B\u578B",
                    group: "\u8BBF\u95EE\u7EC4",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "\u6570\u636E\u6E90\u540D\u79F0\uff0c\u4EC5\u4F5C\u663E\u793A\u7528",
                        key: "\u6570\u636E\u6E90Key\uff0c\u540E\u7EED\u4EE3\u7801\u4E2D\u4F7F\u7528",
                        servers: "\u8BF7\u8F93\u5165 Kafka \u670D\u52A1\u5668\u5730\u5740\uff0c\u5982\uff1A192.168.2.5:9092,...",
                        username: "\u8BF7\u8F93\u5165 Kafka \u8BBF\u95EE\u7528\u6237\u540D\uff0c\u53EF\u9009",
                        password: "\u8BF7\u8F93\u5165 Kafka \u8BBF\u95EE\u5BC6\u7801\uff0c\u53EF\u9009",
                        extra: "\u8BF7\u9009\u62E9 Kafka \u8BBF\u95EE\u7C7B\u578B",
                        group: "\u8BF7\u8F93\u5165 Kafka \u8BBF\u95EE\u7EC4\uff0c \u53EF\u9009",
                        other: '\u8BF7\u8F93\u5165 Kafka \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uff0c\u5982\uff1A {xxxx: "yyyy"}'
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
        kafka: {
            subscribe: {
                title: "Kafka Subscribe Info",
                name: "Kafka Subscribe",
                form: {
                    key: "DataSource Key",
                    topic: "Topic",
                    broadcast: "Broadcast",
                    name: "Name",
                    ackMode: "Ack Mode",
                    concurrency: "Concurrency",
                    path: "Path",
                    placeholder: {
                        key: "Please Enter DataSource Key",
                        topic: "Please Enter Subscribe Topic",
                        name: "Please Enter Name",
                        path: "Please Enter Path",
                        params: "Please Enter Kafka Subscribe Params",
                        other: "Please Enter Kafka Subscribe Other JSON Config"
                    }
                }
            },
            datasource: {
                title: "Kafka DataSource",
                name: "Kafka",
                form: {
                    name: "Name",
                    key: "Key",
                    servers: "Server Addresses",
                    username: "User Name",
                    password: "Password",
                    extra: "Type",
                    group: "Group Id",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        servers: "Please Enter Kafka Server Addresses\uff0ceg\uff1A192.168.2.5:9092,...",
                        username: "Please Enter Kafka username, Optional",
                        password: "Please Enter Kafka password, Optional",
                        extra: "Please Enter Kafka Access Type",
                        other: 'Please Enter Kafka Other Config\uff0ceg\uff1A {xxxx: "yyyy"}'
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.servers")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).servers,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).servers = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.servers"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    /**
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).password = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),**/
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("kafka.datasource.form.placeholder.other"),
                            language: "json",
                            style: { "height": "150px" }
                        }, null, 8, ["value", "placeholder"])
                    ])
                ], 64);
            };
        }
    };
    var magicKafkaSubscribeInfo_vue_vue_type_style_index_0_scoped_true_lang = "";
    var _export_sfc = (sfc, props) => {
        const target = sfc.__vccOpts || sfc;
        for (const [key, val] of props) {
            target[key] = val;
        }
        return target;
    };
    const _hoisted_1 = { class: "magic-kafka-info" };
    const _hoisted_2 = { style: { "flex": "1", "padding-top": "5px" } };
    const ackModeOtions = [{ text: "RECORD", value: "RECORD" }, { text: "BATCH", value: "BATCH" },
        { text: "TIME", value: "TIME" }, { text: "COUNT", value: "COUNT" },
        { text: "COUNT_TIME", value: "COUNT_TIME" }, { text: "MANUAL", value: "MANUAL" }, { text: "MANUAL_IMMEDIATE", value: "MANUAL_IMMEDIATE" }];
    const _sfc_main = {
        __name: "magic-kafka-subscribe-info",
        setup(__props) {
            const $i = vue.inject("i18n.format");
            const info = vue.inject("info");
            return (_ctx, _cache) => {
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                return vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
                    vue.createElementVNode("form", null, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("message.enable")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).enabled,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).enabled = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.key"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.broadcast")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).broadcast,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).broadcast = $event)
                        }, null, 8, ["value"]),
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.topic")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).topic,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).topic = $event),
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.topic"),
                            width: "200px"
                        }, null, 8, ["value", "placeholder"]),

                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.ackMode")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).ackMode,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).ackMode = $event),
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.ackMode"),
                            options: ackModeOtions,
                            width: "150px"
                        }, null, 8, ["value", "placeholder"]),

                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.concurrency")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).concurrency,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).concurrency = $event),
                            width: "50px"
                        }, null, 8, ["value", "placeholder"]),


                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("kafka.subscribe.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.name"),
                            width: "auto",
                            style: { "flex": "1" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2, [
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.other")
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
                            placeholder: vue.unref($i)("kafka.subscribe.form.placeholder.params")
                        }, null, 8, ["value", "placeholder"])
                    ])
                ]);
            };
        }
    };
    var MagicKafkaSubscribeInfo = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-4e71cade"]]);
    if (typeof window !== "undefined") {
        let loadSvg = function() {
            var body = document.body;
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 1024 1024"  id="magic-kafka-kafka"><path d="M725.333 597.333c-38.4 0-72.533 17.067-96 44.8L524.8 582.4c6.4-19.2 10.667-38.4 10.667-57.6 0-23.467-4.267-44.8-12.8-66.133l115.2-66.134c23.466 21.334 53.333 36.267 89.6 36.267 70.4 0 128-57.6 128-128s-57.6-128-128-128-128 57.6-128 128c0 12.8 2.133 25.6 6.4 38.4l-115.2 66.133a170.24 170.24 0 0 0-104.534-57.6v-115.2c36.267-12.8 64-49.066 64-89.6C448 85.333 405.333 42.667 352 42.667s-96 42.666-96 96c0 42.666 27.733 76.8 64 89.6v115.2c-85.333 14.933-149.333 89.6-149.333 179.2s64 164.266 149.333 179.2v93.866c-36.267 12.8-64 49.067-64 89.6 0 53.334 42.667 96 96 96s96-42.666 96-96c0-42.666-27.733-76.8-64-89.6v-93.866c44.8-8.534 81.067-32 108.8-64l106.667 61.866c-2.134 8.534-2.134 17.067-2.134 25.6 0 70.4 57.6 128 128 128s128-57.6 128-128-57.6-128-128-128z" /></symbol>';
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
                type: "kafka-subscribe",
                icon: "#magic-kafka-kafka",
                title: "kafka.subscribe.name",
                service: MagicKafkaSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
            }],
            datasources: [{ 
                type: "kafka-datasource",
                icon: "#magic-kafka-kafka",
                title: i18n.format("kafka.datasource.title"),
                name: i18n.format("kafka.datasource.name"),
                service: MagicKafkaDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }],
            toolbars: [{ 
                type: "kafka-subscribe",
                title: "kafka.subscribe.title",
                icon: "parameter",
                component: MagicKafkaSubscribeInfo
            }]
        };
    };
    return index;
}(Vue);
