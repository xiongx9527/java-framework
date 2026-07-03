var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-influx-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-influx-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-influx-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-influx-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-influx-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-influx-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-influx-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicInflux = function(vue) {
    "use strict";
    var MagicInfluxDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo");
            return findResources && (((_a = findResources("nebula-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/influx/datasource/test", info).success((res) => {
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
        influx: {
            datasource: {
                title: "Influx \u6570\u636E\u6E90",
                name: "Influx \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    url: "URL地址",
                    username: "用户名",
                    password: "密码",
                    token:"访问令牌",
                    org: "组织",
                    bucket: "桶名称",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        url: "请输入URL地址，必填",
                        username: "请输入用户名，选填",
                        password: "请输入密码，选填",
                        token:"请输入访问令牌，选填",
                        org: "请输入组织名称，选填",
                        bucket: "请输入桶名称，选填",
                        other: '\u8BF7\u8F93\u5165 Nebula \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        influx: {
            datasource: {
                title: "Influx DataSource",
                name: "Influx",
                form: {
                    name: "Name",
                    key: "Key",
                    url: "url",
                    username: "Username",
                    password: "Password",
                    token: "Token",
                    org: "Org",
                    bucket: "Bucket",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        url: "Please Enter url, Required",
                        username: "Please Enter username, Optional",
                        password: "Please Enter password , Optional",
                        token: "Please Enter token, Optional",
                        org: "Please Enter org, Optional",
                        bucket: "Please Enter bucket, Optional",
                        other: 'Please Enter Nebula Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-influx-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.url")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).url,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).hostAddress = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.url"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            type:"password",
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.token")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).token,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).token = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.token"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.org")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).org,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).org = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.org"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.bucket")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).bucket,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).bucket = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.bucket"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("influx.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("influx.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dyInflux");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dyInflux";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 128 128" id="magic-influx-influx"><path d="M76.32 16.27H51.68c-1.29 0-2.33 1.05-2.33 2.33v99.96c0 1.29 1.04 2.33 2.33 2.33h24.64c1.29 0 2.33-1.04 2.33-2.33V18.6c0-1.28-1.04-2.33-2.33-2.33z" fill="#40C0E7"></path></symbol>';
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
                type: "influx-datasource",
                icon: "#magic-influx-influx",
                title: i18n.format("influx.datasource.title"),
                name: i18n.format("influx.datasource.name"),
                service: MagicInfluxDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
