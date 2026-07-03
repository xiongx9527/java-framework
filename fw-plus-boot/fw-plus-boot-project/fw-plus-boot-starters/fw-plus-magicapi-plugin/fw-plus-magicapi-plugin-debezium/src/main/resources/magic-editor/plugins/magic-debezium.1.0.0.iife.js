var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-debezium-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-debezium-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-debezium-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-debezium-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-debezium-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-debezium-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-debezium-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicDebezium = function(vue) {
    "use strict";
    var MagicDebeziumDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo");
            return findResources && (((_a = findResources("debezium-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/debezium/datasource/test", info).success((res) => {
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
        debezium: {
            kafka: {
                form:{
                    kafkaServers:"Kafka地址",
                    kafkaTopic:"Kafka主题",
                    placeholder: {
                        kafkaServers:"请输入Kafka地址，必填",
                        kafkaTopic:"请输入Kafka主题，必填"
                    }
                }
            } ,
            datasource: {
                title: "Debezium \u6570\u636E\u6E90",
                name: "Debezium \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    connectorClass: "连接器类",
                    hostname: "数据库IP",
                    port: "数据库端口",
                    username: "数据库用户",
                    password: "数据库密码",
                    database: "数据库名称",
                    includeTables: "包含表",
                    excludeTables: "排除表",
                    producerEnable: "启用生产者",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    enabled: "是否启用",
                    placeholder: {
                        name: "数据源名称，仅显示使用",
                        key: "数据源Key，必填",
                        connectorClass: "请输入 Debezium 连接器类，必填",
                        hostname: "请输入数据库IP，必填",
                        port: "请输入数据库端口，必填",
                        database: "请输入数据库名称，必填",
                        username: "请输入数据库用户名，必填",
                        password: "请输入数据库密码，必填",
                        includeTables: "包含表,例如：user,order",
                        excludeTables: "排除表,例如：user,order",
                        producerEnable: '启用生产者',
                        enabled: "是否启用"
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
        debezium: {
            kafka: {
                form:{
                    kafkaServers:"Kafka Servers",
                    kafkaTopic:"Kafka Topic",
                    placeholder: {
                        kafkaServers:"Please Enter Kafka Servers",
                        kafkaTopic:"Please Enter Kafka Topic"
                    }
                }
            } ,
            datasource: {
                title: "Debezium DataSource",
                name: "Debezium",
                form: {
                    name: "Name",
                    key: "Key",
                    connectorClass: "Connector Class",
                    hostname: "DatabaseHostname",
                    port: "DatabasePort",
                    username: "DatabaseUserName",
                    password: "DatabasePassword",
                    database: "Database",
                    includeTables: "Include Tables",
                    excludeTables: "Exclude Tables",
                    producerEnable: "Enabled Producer Enabled",
                    other: "Other Config",
                    enabled: "Enabled",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        connectorClass: "Please Enter Debezium Connector Class",
                        hostname: "Please Enter database Server ip",
                        port: "Please Enter database Server port",
                        username: "Please Enter database username",
                        password: "Please Enter database password",
                        database: "Please Enter database Name",
                        includeTables: "Please Enter Include Tables, eg: user,order",
                        excludeTables: "Please Enter Exclude Tables, eg: user,order",
                        producerEnable: 'Please Enter Enabled Producer Enabled',
                        other: 'Please Enter Debezium Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}' ,
                        enabled: "Enabled"
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
        __name: "magic-debezium-datasource-info",
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
            const options = [{ text: "io.debezium.connector.mysql.MySqlConnector", value: "io.debezium.connector.mysql.MySqlConnector" },
                { text: "io.debezium.connector.postgresql.PostgresConnector", value: "io.debezium.connector.postgresql.PostgresConnector" },
                { text: "io.debezium.connector.sqlserver.SqlServerConnector", value: "io.debezium.connector.sqlserver.SqlServerConnector" },
                { text: "io.debezium.connector.oracle.OracleConnector", value: "io.debezium.connector.oracle.OracleConnector" },
                { text: "io.debezium.connector.mongodb.MongoDbConnector", value: "io.debezium.connector.mongodb.MongoDbConnector" }];
            return (_ctx, _cache) => {
                const _component_magic_textarea = vue.resolveComponent("magic-textarea");
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_14, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.enabled")), 1),
                        vue.createVNode(_component_magic_checkbox , {
                            value: vue.unref(info).enabled,
                            "onUpdate:value": _cache[13] || (_cache[13] = ($event) => vue.unref(info).enabled = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.enabled"),
                            style: { "width": "20px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.connectorClass")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).connectorClass,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).connectorClass = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.connectorClass"),
                            options:options,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.hostname")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).hostname,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).hostname = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.hostname"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.password"),
                            type:"password",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.database")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).database,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).database = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.database"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.includeTables")), 1),
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).includeTables,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).includeTables = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.includeTables"),
                            width: "100%",
                            style: { "height": "100px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.excludeTables")), 1),
                        vue.createVNode(_component_magic_textarea, {
                            value: vue.unref(info).excludeTables,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).excludeTables = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.excludeTables"),
                            width: "100%",
                            style: { "height": "100px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_11, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.kafka.form.kafkaServers")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).kafkaServers,
                            "onUpdate:value": _cache[10] || (_cache[10] = ($event) => vue.unref(info).kafkaServers = $event),
                            placeholder: vue.unref($i)("debezium.kafka.form.placeholder.kafkaServers"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_12, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.kafka.form.kafkaTopic")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).kafkaTopic,
                            "onUpdate:value": _cache[11] || (_cache[11] = ($event) => vue.unref(info).kafkaTopic = $event),
                            placeholder: vue.unref($i)("debezium.kafka.form.placeholder.kafkaTopic"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),

                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.producerEnable")), 1),
                        vue.createVNode(_component_magic_checkbox, {
                            value: vue.unref(info).producerEnable,
                            "onUpdate:value": _cache[13] || (_cache[13] = ($event) => vue.unref(info).producerEnable = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.producerEnable"),
                            style: { "width": "20px" }
                        }, null, 8, ["value", "placeholder"])
                    ]),


                    vue.createElementVNode("div", _hoisted_14, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("debezium.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[14] || (_cache[14] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("debezium.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__debezium");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__debezium";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 160 160" id="magic-debezium-debezium"><path d="M20.86 93.8a16.64 16.64 0 0 1 16.62 16.44v20.71H16a16.628 16.628 0 0 1-16-16.5V93.8h20.86Zm-.27-46.89a16.65 16.65 0 0 1 16.57 16.63v3.9A26.39 26.39 0 0 0 63.53 93.8h3.9A16.64 16.64 0 0 1 84 110.19v20.75H63.54a16.65 16.65 0 0 1-16.63-16.63v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91A16.65 16.65 0 0 1 0 67.44V46.91h20.59ZM20.53 0a16.651 16.651 0 0 1 16.63 16.62v3.9a26.39 26.39 0 0 0 26.35 26.37h3.9A16.65 16.65 0 0 1 84 63.51v3.89a26.39 26.39 0 0 0 26.41 26.36h3.91a16.649 16.649 0 0 1 16.63 16.63v20.53h-20.53a16.647 16.647 0 0 1-16.63-16.63v-3.93A26.39 26.39 0 0 0 67.43 84h-3.9A16.651 16.651 0 0 1 46.9 67.4v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91A16.65 16.65 0 0 1 0 20.52V0h20.53Zm46.88 0A16.65 16.65 0 0 1 84 16.62v3.9a26.391 26.391 0 0 0 26.4 26.36h3.91a16.65 16.65 0 0 1 16.629 16.63V84H110.36a16.65 16.65 0 0 1-16.57-16.6v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91a16.64 16.64 0 0 1-16.62-16.4V0h20.51ZM115 0a16.632 16.632 0 0 1 15.94 16.49v20.65h-20.85A16.64 16.64 0 0 1 93.47 20.7V0H115Z" fill="#6E6E6E" /></symbol>';
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
                type: "debezium-datasource",
                icon: "#magic-debezium-debezium",
                title: i18n.format("debezium.datasource.title"),
                name: i18n.format("debezium.datasource.name"),
                service: MagicDebeziumDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
