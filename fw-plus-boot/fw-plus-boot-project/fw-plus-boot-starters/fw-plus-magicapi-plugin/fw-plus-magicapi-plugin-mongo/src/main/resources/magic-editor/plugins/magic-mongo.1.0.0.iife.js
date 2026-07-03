var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-mongo-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-mongo-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-mongo-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-mongo-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-mongo-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-mongo-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-mongo-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicMongo = function(vue) {
    "use strict";
    var MagicMongoDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo");
            return findResources && (((_a = findResources("debezium-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/mongo/datasource/test", info).success((res) => {
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
        mongo: {
            datasource: {
                title: "Mongo \u6570\u636E\u6E90",
                name: "Mongo \u6570\u636E\u6E90",
                extra:{
                    mode: {
                        standalone: "单机模式",
                        replicaSet: "副本集",
                        cluster: "分片集群"
                    }
                } ,
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    hostname: "IP",
                    port: "端口",
                    username: "用户名",
                    password: "密码",
                    database: "数据库",
                    mode: "集群模式",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        hostname: "请输入IP，必填",
                        port: "请输入端口，必填",
                        username: "请输入用户名，选填",
                        password: "请输入密码，选填",
                        database: "请输入数据库，必填",
                        mode: "集群模式，选填",
                        clientType: "客户端模式，选填",
                        other: '\u8BF7\u8F93\u5165 Mongo \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        mongo: {
            datasource: {
                title: "mongo DataSource",
                name: "mongo",
                extra:{
                    mode: {
                        standalone:"Standalone",
                        replicaSet: "Replica Set",
                        cluster: "Sharded Cluster"
                    },
                    clientType: {
                        lettuce: "Lettuce",
                        jedis: "Jedis"
                    }
                } ,
                form: {
                    name: "Name",
                    key: "Key",
                    hostname: "Hostname",
                    port: "Port",
                    username: "Username",
                    password: "Password",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        hostname: "Please Enter Server ip, Required",
                        port: "Please Enter Server port, Required",
                        username: "Please Enter username, Option",
                        password: "Please Enter password , Option",
                        database: "Please Enter database, Required",
                        mode: "Please Enter mode, Option",
                        clientType: "Please Enter clientType, Option",
                        other: 'Please Enter Mongo Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-mongo-datasource-info",
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
            const mode = [{ text: $i("mongo.datasource.extra.mode.standalone"), value: "standalone" }, { text: $i("mongo.datasource.extra.mode.replicaSet"), value: "replicaSet"  }, { text: $i("mongo.datasource.extra.mode.cluster"), value: "cluster"  }];return (_ctx, _cache) => {
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.hostname")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).hostname,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).hostname = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.hostname"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            type:"password",
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.database")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).database,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).database = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.database"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.mode")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).mode,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).mode = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.mode"),
                            inputable: "",
                            options: mode,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    /**
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.clientType")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).clientType,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).clientType = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.clientType"),
                            inputable: "",
                            options: clientType,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    **/
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("mongo.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("mongo.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dymongo");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dymongo";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 120 240" id="magic-mongo-mongo"><path d="M82.3229 28.6444C71.5367 15.8469 62.2485 2.84945 60.351 0.149971C60.1512 -0.0499903 59.8515 -0.0499903 59.6518 0.149971C57.7542 2.84945 48.4661 15.8469 37.6798 28.6444C-54.9019 146.721 52.2613 226.406 52.2613 226.406L53.1601 227.006C53.959 239.303 55.9565 257 55.9565 257H59.9514H63.9463C63.9463 257 65.9438 239.403 66.7428 227.006L67.6416 226.306C67.7414 226.406 174.905 146.721 82.3229 28.6444ZM59.9514 224.606C59.9514 224.606 55.1576 220.507 53.8592 218.408V218.207L59.6518 89.6326C59.6518 89.2326 60.2511 89.2326 60.2511 89.6326L66.0436 218.207V218.408C64.7453 220.507 59.9514 224.606 59.9514 224.606Z" fill="#6E6E6E"/></symbol>';
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
                type: "mongo-datasource",
                icon: "#magic-mongo-mongo",
                title: i18n.format("mongo.datasource.title"),
                name: i18n.format("mongo.datasource.name"),
                service: MagicMongoDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
