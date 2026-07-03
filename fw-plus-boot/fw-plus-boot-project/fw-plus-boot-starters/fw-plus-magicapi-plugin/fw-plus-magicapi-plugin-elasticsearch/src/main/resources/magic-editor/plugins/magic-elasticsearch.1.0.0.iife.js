var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-elasticsearch-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-elasticsearch-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-elasticsearch-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-elasticsearch-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-elasticsearch-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-elasticsearch-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-elasticsearch-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicElasticsearch = function(vue) {
    "use strict";
    var MagicElasticsearchDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo");
            return findResources && (((_a = findResources("nebula-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/elasticsearch/datasource/test", info).success((res) => {
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
        elasticsearch: {
            datasource: {
                title: "Elasticsearch \u6570\u636E\u6E90",
                name: "Elasticsearch \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    scheme: "协议",
                    hostname: "IP",
                    port: "端口",
                    username: "用户名",
                    password: "密码",
                    database: "数据库",
                    mode: "集群模式",
                    clientType: "客户端模式",
                    extra: "Extra",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        scheme: "请选择协议，选填",
                        hostname: "请输入IP，必填",
                        port: "请输入端口，必填",
                        username: "请输入用户名，选填",
                        password: "请输入密码，选填",
                        database: "请输入数据库，必填",
                        mode: "集群模式，选填",
                        clientType: "客户端模式，选填",
                        other: '\u8BF7\u8F93\u5165 Redis \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        elasticsearch: {
            datasource: {
                title: "Elasticsearch DataSource",
                name: "Elasticsearch",
                form: {
                    name: "Name",
                    key: "Key",
                    scheme: "Scheme",
                    hostname: "IP",
                    port: "Port",
                    username: "Username",
                    password: "Password",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        scheme: "Scheme",
                        hostname: "Please Enter Server ip, Required",
                        port: "Please Enter Server port, Required",
                        username: "Please Enter username, Option",
                        password: "Please Enter password , Option",
                        database: "Please Enter database, Required",
                        mode: "Please Enter mode, Option",
                        clientType: "Please Enter clientType, Option",
                        other: 'Please Enter Redis Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-elasticsearch-datasource-info",
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
            const mode = [{ text: $i("elasticsearch.datasource.extra.mode.standalone"), value: "standalone" }, { text: $i("elasticsearch.datasource.extra.mode.sentinel"), value: "sentinel"  }, { text: $i("elasticsearch.datasource.extra.mode.cluster"), value: "cluster"  }];
            const clientType = [{ text: $i("elasticsearch.datasource.extra.clientType.lettuce"), value: "lettuce" }, { text: $i("elasticsearch.datasource.extra.clientType.jedis"), value: "jedis"  }];
            return (_ctx, _cache) => {
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.scheme")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).scheme,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).scheme = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.scheme"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.hostname")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).hostname,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).hostname = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.hostname"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            type:"password",
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    /**
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.database")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).database,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).database = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.database"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.mode")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).mode,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).mode = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.mode"),
                            inputable: "",
                            options: mode,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.clientType")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).clientType,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).clientType = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.clientType"),
                            inputable: "",
                            options: clientType,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                        ***/
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("elasticsearch.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("elasticsearch.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dyelasticsearch");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dyelasticsearch";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 36 36" id="magic-elasticsearch-elasticsearch"><path fill="#3B88C3" d="M36 32a4 4 0 0 1-4 4H4a4 4 0 0 1-4-4V4a4 4 0 0 1 4-4h28a4 4 0 0 1 4 4v28z"></path><path fill="#FFF" d="M12.219 9.621c0-1.55.775-2.697 2.418-2.697h7.689c1.488 0 2.202 1.054 2.202 2.14c0 1.054-.744 2.139-2.202 2.139H16.87v4.527h5.085c1.52 0 2.264 1.054 2.264 2.14c0 1.054-.775 2.139-2.264 2.139H16.87v4.713h5.736c1.488 0 2.201 1.055 2.201 2.14c0 1.055-.744 2.14-2.201 2.14h-7.999c-1.364 0-2.387-.93-2.387-2.325V9.621z"></path></symbol>';
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
                type: "elasticsearch-datasource",
                icon: "#magic-elasticsearch-elasticsearch",
                title: i18n.format("elasticsearch.datasource.title"),
                name: i18n.format("elasticsearch.datasource.name"),
                service: MagicElasticsearchDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
