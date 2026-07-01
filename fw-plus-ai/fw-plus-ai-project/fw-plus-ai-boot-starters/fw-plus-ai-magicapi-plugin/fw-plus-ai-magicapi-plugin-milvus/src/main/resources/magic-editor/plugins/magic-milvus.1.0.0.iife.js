var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-milvus-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-milvus-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-milvus-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-milvus-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-milvus-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-milvus-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-milvus-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicMilvus = function(vue) {
    "use strict";
    var MagicMilvusDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo");
            return findResources && (((_a = findResources("milvus-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/milvus/datasource/test", info).success((res) => {
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
        milvus: {
            datasource: {
                title: "Milvus \u6570\u636E\u6E90",
                name: "Milvus \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    uri: "URI",
                    token: "Token",
                    username: "用户名",
                    password: "密码",
                    database: "数据库",
                    dbName: "数据库名",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        uri: "请输入uri，必填  , 例如：http://localhost:19530 ",
                        token: "请输入token，选填",
                        username: "请输入用户名，选填",
                        password: "请输入密码，选填",
                        dbName: "请输入数据库，选填",
                        other: ''
                    }
                }
            }
        }
    };
    var localEn = {
        milvus: {
            datasource: {
                title: "Milvus DataSource",
                name: "Milvus",
                form: {
                    name: "Name",
                    key: "Key",
                    uri: "URI ",
                    token: "Token",
                    username: "Username",
                    password: "Password",
                    dbName: "dbName",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        uri: "Please Enter Server ip, Required, example: http://localhost:19530",
                        token: "Please Enter Server port, Option",
                        username: "Please Enter username, Option",
                        password: "Please Enter password , Option",
                        dbName: "Please Enter dbName, Option",
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
        __name: "magic-milvus-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.uri")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).uri,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).uri = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.uri"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.token")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).token,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).token = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.token"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            type:"password",
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.dbName")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).dbName,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).dbName = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.dbName"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("milvus.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("milvus.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__milvus");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__milvus";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 36 24" id="magic-milvus-milvus"><path d="M7.9751 3.5166C12.6472 -1.17235 20.2239 -1.17238 24.896 3.5166C29.5757 8.20571 29.5757 15.8088 24.896 20.4902C20.2239 25.1716 12.6473 25.1715 7.9751 20.4824L0.338379 12.8184C-0.112845 12.3649 -0.112941 11.6342 0.338379 11.1807L7.9751 3.5166ZM23.0825 5.76953C19.6518 2.32572 14.0846 2.32572 10.6538 5.76953L5.04053 11.3965C4.70985 11.7347 4.70966 12.273 5.04053 12.6035L10.6616 18.2373C14.0923 21.6811 19.6595 21.6811 23.0903 18.2373C26.5207 14.7935 26.5206 9.21325 23.0825 5.76953ZM16.9136 5.99219C20.1669 5.99242 22.8042 8.68764 22.8042 12.0117C22.804 15.3356 20.1668 18.03 16.9136 18.0303C13.6602 18.0303 11.0222 15.3358 11.022 12.0117C11.022 8.6875 13.6601 5.99219 16.9136 5.99219ZM31.0649 8.02051C30.9973 7.73621 31.3353 7.54387 31.5386 7.75098L34.9019 11.1875C35.3457 11.641 35.3457 12.3718 34.9019 12.833L31.5386 16.2686C31.3429 16.4684 31.005 16.2843 31.0649 16C31.6442 13.3787 31.6442 10.6418 31.0649 8.02051Z" fill="#6E6E6E" /></symbol>';
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
                type: "milvus-datasource",
                icon: "#magic-milvus-milvus",
                title: i18n.format("milvus.datasource.title"),
                name: i18n.format("milvus.datasource.name"),
                service: MagicMilvusDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
