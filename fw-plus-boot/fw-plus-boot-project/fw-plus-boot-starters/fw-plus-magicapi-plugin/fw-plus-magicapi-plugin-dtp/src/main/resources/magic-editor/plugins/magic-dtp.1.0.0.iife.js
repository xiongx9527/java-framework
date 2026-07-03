var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-dtp-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-dtp-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-dtp-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-dtp-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-dtp-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-dtp-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-dtp-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicDtp = function(vue) {
    "use strict";
    var MagicDtpDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo");
            return findResources && (((_a = findResources("dtp-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/dtp/datasource/test", info).success((res) => {
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
        dtp: {
            datasource: {
                title: "Dtp \u6570\u636E\u6E90",
                name: "Dtp \u6570\u636E\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    corePoolSize: "核心线程数",
                    maximumPoolSize: "最大线程数",
                    keepAliveTime: "空闲时间",
                    queueType: "队列类型",
                    queueCapacity: "队列容量",
                    threadFactory: "线程工厂",
                    rejectedPolicy: "拒绝策略",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        corePoolSize: "核心线程数，默认1",
                        maximumPoolSize: "最大线程数，默认10",
                        queueType: "队列类型，默认ArrayBlockingQueue",
                        queueCapacity: "队列容量，默认1000",
                        threadFactory: "线程工厂，默认DefaultThreadFactory",
                        rejectedPolicy: "拒绝策略，默认CallerRunsPolicy",
                        other: ''
                    }
                }
            }
        }
    };
    var localEn = {
        dtp: {
            datasource: {
                title: "Dtp DataSource",
                name: "Dtp",
                form: {
                    name: "Name",
                    key: "Key",
                    corePoolSize: "Core Pool Size",
                    maximumPoolSize: "Maximum Pool Size",
                    keepAliveTime: "Keep Alive Time",
                    queueType: "Queue Type",
                    queueCapacity: "Queue Capacity",
                    threadFactory: "Thread Factory",
                    rejectedPolicy: "Rejected Policy",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        corePoolSize: "Core Pool Size, Default 1",
                        maximumPoolSize: "Maximum Pool Size, Default 10",
                        queueType: "Queue Type, Default ArrayBlockingQueue",
                        queueCapacity: "Queue Capacity, Default 1000",
                        threadFactory: "Thread Factory, Default DefaultThreadFactory",
                        rejectedPolicy: "Rejected Policy, Default CallerRunsPolicy",
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
        __name: "magic-dtp-datasource-info",
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
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.corePoolSize")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).corePoolSize,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).corePoolSize = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.corePoolSize"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.maximumPoolSize")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).maximumPoolSize,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).maximumPoolSize = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.maximumPoolSize"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.queueType")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).queueType,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).queueType = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.queueType"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.queueCapacity")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).queueCapacity,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).queueCapacity = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.queueCapacity"),
                            type: "number",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.threadFactory")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).threadFactory,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).threadFactory = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.threadFactory"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.rejectedPolicy")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).rejectedPolicy,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).rejectedPolicy = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.rejectedPolicy"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("dtp.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("dtp.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dtp");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dtp";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol viewBox="0 0 24 24" id="magic-dtp-dtp">' +
              '<path d="M22.6 5.5a9.91 9.91 0 0 0-3.88-4.04A11.18 11.18 0 0 0 13.08.04H.18l6.91 4h5.99c1.94 0 3.41.62 4.42 1.88C18.5 7.17 19 9 19 11.43v1.27c-.01 2.34-.53 4.14-1.55 5.4-1.02 1.26-2.5 1.89-4.42 1.89H6.86L0 23.95h13.13c2.09 0 3.97-.49 5.63-1.44 1.66-.95 2.95-2.3 3.87-4.02S24 14.79 24 12.56v-1.1c0-2.22-.47-4.2-1.4-5.96zM.13 3.96v16.1L14.07 12z"/></symbol>';
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
                type: "dtp-datasource",
                icon: "#magic-dtp-dtp",
                title: i18n.format("dtp.datasource.title"),
                name: i18n.format("dtp.datasource.name"),
                service: MagicDtpDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
