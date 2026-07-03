var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-redis-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-redis-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-redis-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-redis-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-redis-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-redis-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-redis-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicRedis = function(vue) {
    "use strict";
    var MagicRedisDataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo");
            return findResources && (((_a = findResources("nebula-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/redis/datasource/test", info).success((res) => {
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
        redis: {
            datasource: {
                title: "Redis \u6570\u636E\u6E90",
                name: "Redis \u6570\u636E\u6E90",
                extra:{
                    mode: {
                        standalone: "单机模式",
                        sentinel: "哨兵模式",
                        cluster: "集群模式"
                    },
                    clientType: {
                        lettuce: "Lettuce",
                        jedis: "Jedis"
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
                    clientType: "客户端模式",
                    extra: "Extra",
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
                        other: '\u8BF7\u8F93\u5165 Redis \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };
    var localEn = {
        redis: {
            datasource: {
                title: "Redis DataSource",
                name: "Redis",
                extra:{
                    mode: {
                        standalone:"Standalone",
                        sentinel: "Sentinel",
                        cluster: "Cluster"
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
        __name: "magic-redis-datasource-info",
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
            const mode = [{ text: $i("redis.datasource.extra.mode.standalone"), value: "standalone" }, { text: $i("redis.datasource.extra.mode.sentinel"), value: "sentinel"  }, { text: $i("redis.datasource.extra.mode.cluster"), value: "cluster"  }];
            const clientType = [{ text: $i("redis.datasource.extra.clientType.lettuce"), value: "lettuce" }, { text: $i("redis.datasource.extra.clientType.jedis"), value: "jedis"  }];
            return (_ctx, _cache) => {
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.hostname")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).hostname,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).hostname = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.hostname"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.port")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).port,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).port = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.port"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.username")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).username,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).username = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.username"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.password")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).password,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).password = $event),
                            type:"password",
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.password"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.database")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).database,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).database = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.database"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.mode")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).mode,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).mode = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.mode"),
                            inputable: "",
                            options: mode,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.clientType")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).clientType,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).clientType = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.clientType"),
                            inputable: "",
                            options: clientType,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("redis.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("redis.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dyRedis");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dyRedis";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 85 85" id="magic-redis-redis"><path d="M38.5 0C59.752 5.16002e-07 77 17.248 77 38.5C77 59.752 59.7828 77 38.5 77C34.188 77 30.0297 76.2912 26.1797 74.9668H26.1182C24.0547 74.2584 22.0837 73.3962 20.2051 72.3799C20.0819 72.3183 19.9274 72.2262 19.7734 72.1338C14.9071 69.4234 10.6876 65.6964 7.42285 61.2305C5.5441 58.6433 3.9424 55.8406 2.74121 52.8223C0.985611 48.3871 1.24057e-07 43.5512 0 38.5C0 17.248 17.248 0 38.5 0ZM42.5059 18.4805C35.2685 18.4805 26.3257 21.0761 20.4453 25.1416C20.3757 29.3436 22.8815 34.8101 23.7861 34.1953C28.8661 30.7791 32.9022 28.5582 36.834 27.4307C31.0233 33.5115 17.1068 47.6202 15.4004 50.1152C15.5744 52.4043 18.601 58.5203 20.0625 58.5205C20.5149 58.5205 20.8976 58.2809 21.3496 57.8711C25.5251 53.4298 28.9701 49.4668 31.9971 45.6406C32.4496 51.2434 35.3727 58.1102 43.584 58.1104C50.9258 58.1104 58.2326 53.1217 61.5381 41.8818C61.9555 40.5154 60.147 39.4226 59.1377 40.584C55.9367 44.3422 52.4918 48.6807 45.5674 48.6807C39.4088 48.6806 37.1126 43.5564 36.9385 39.4229C38.2956 42.0875 40.9402 44.2735 45.0459 44.1709C52.9787 43.9318 58.407 37.2021 58.4072 31.0869C58.4072 23.776 52.5964 18.4806 42.5059 18.4805ZM45.5322 26.7822C49.3247 27.3631 50.7518 29.3439 50.752 31.4961C50.752 35.049 47.0292 36.7918 43.6543 36.792C41.8448 36.792 40.4526 36.3475 39.3389 35.7666C41.3919 32.8971 43.3751 29.9252 45.5322 26.7822Z" fill="#6E6E6E"/></symbol>';
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
                type: "redis-datasource",
                icon: "#magic-redis-redis",
                title: i18n.format("redis.datasource.title"),
                name: i18n.format("redis.datasource.name"),
                service: MagicRedisDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
