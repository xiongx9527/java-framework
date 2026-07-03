var __vite_style__ = document.createElement("style");
__vite_style__.innerHTML = "\n.magic-s3-info[data-v-4e71cade]{\r\n	display: flex;\r\n	flex-direction: column;\r\n	flex: 1;\r\n	padding: 5px;\n}\n.magic-s3-info form[data-v-4e71cade]{\r\n	display: flex;\n}\n.magic-s3-info form label[data-v-4e71cade]{\r\n	display: inline-block;\r\n	width: 100px;\r\n	height: var(--magic-input-height);\r\n	line-height: var(--magic-input-height);\r\n	font-weight: 400;\r\n	text-align: right;\r\n	padding: 0 5px;\n}\n.magic-s3-info form[data-v-4e71cade] .magic-checkbox{\r\n	width: 22px;\r\n	height: 22px;\n}\n.magic-s3-info form[data-v-4e71cade] .magic-textarea{\r\n	margin: 5px;\n}\n.magic-s3-info form .magic-form-row.align-left label[data-v-4e71cade] {\r\n	text-align: left;\r\n	width: 100%;\r\n}\n.magic-s3-info form .magic-form-row.align-left .magic-checkbox[data-v-4e71cade] {\r\n	margin-left: 0 !important;\r\n}\r\n";
document.head.appendChild(__vite_style__);
var MagicS3 = function(vue) {
    "use strict";
    var MagicS3DataSource = (request, $i, modal, JavaClass) => {
        let findResources;
        JavaClass.setExtensionAttribute("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo", () => {
            var _a;
            JavaClass.loadClass("com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo");
            return findResources && (((_a = findResources("s3-datasource")[0]) == null ? void 0 : _a.children) || []).map((it) => {
                return {
                    name: it.key,
                    type: "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo",
                    comment: it.name
                };
            }) || [];
        });
        return {
            injectResources: (fn) => findResources = fn,
            dialogWidth: "1100px",
            doTest: (info) => {
                request.sendJson("/s3/datasource/test", info).success((res) => {
                    if (res === "ok") {
                        modal.alert($i("datasource.connected"), $i("datasource.test"));
                    } else {
                        modal.alert($i("datasource.connectFailed", res), $i("datasource.test"));
                    }
                });
            }
        };
    };
    // 在 localZhCN 中添加新的配置项文本
    var localZhCN = {
        s3: {
            datasource: {
                title: "S3 \u6570\u636E\u6E90",
                name: "S3 \u6570\u6E90",
                form: {
                    name: "\u540D\u79F0",
                    key: "Key",
                    platform: "平台",
                    accessKey: "AccessKey",
                    secretKey: "SecretKey",
                    endPointSchema: "端点协议",
                    endPoint: "端点",
                    domainSchema: "访问协议",
                    domain: "访问域名",
                    bucketName: "储桶名称",
                    basePath: "基础路径",
                    tmpPath: "临时路径",
                    expirationSeconds: "有效期(秒)",
                    presignedUrlMethod: "预签名方法",
                    other: "\u5176\u4ED6\u914D\u7F6E",
                    placeholder: {
                        name: "名称，仅显示使用",
                        key: "数据源Key，必填",
                        platform: "请输入平台，必填",
                        accessKey: "请输入 AccessKey，必填",
                        secretKey: "请输入 SecretKey，必填",
                        endPointSchema: "请输入 端点协议，例如:https，选填",
                        endPoint: "请输入 端点，必填",
                        domainSchema: "请输入 访问协议，例如:https，选填",
                        domain: "请输入 访问域名，必填",
                        bucketName: "请输入 储桶名称，必填",
                        basePath: "请输入 基础路径，必填",
                        tmpPath: "请输入 临时路径，选填",
                        expirationSeconds: "请输入 有效期(秒)，选填",
                        presignedUrlMethod: "请选择预签名方法，选填",
                        other: '\u8BF7\u8F93\u5165 s3 \u8BBF\u95EE\u76F8\u5173\u914D\u7F6E\uFF0C\u5982\uFF1A {xxxx: "yyyy"}'
                    }
                }
            }
        }
    };

// 在 localEn 中添加英文配置项文本
    var localEn = {
        s3: {
            datasource: {
                title: "S3 DataSource",
                name: "S3",
                form: {
                    name: "Name",
                    key: "Key",
                    platform: "Platform",
                    accessKey: "AccessKey",
                    secretKey: "SecretKey",
                    endPointSchmema: "EndPointSchmema",
                    endPoint: "EndPoint",
                    bucketName: "BucketName",
                    domainSchmema: "DomainSchmema",
                    domain: "Domain",
                    basePath: "BasePath",
                    tmpPath: "TmpPath",
                    expirationSeconds: "ExpirationSeconds",
                    presignedUrlMethod: "Presigned Method",
                    other: "Other Config",
                    placeholder: {
                        name: "DataSource Name, Only Display Use",
                        key: "DataSource Key, Required",
                        platform: "Please Enter Platform , Required",
                        accessKey: "Please Enter AccessKey, Required",
                        secretKey: "Please Enter SecretKey, Required",
                        endPointSchmema: "Please Enter EndPointSchmema, For example: https,Optional",
                        endPoint: "Please Enter EndPoint, Required",
                        domainSchmema: "Please Enter DomainSchmema, For example: https,Optional",
                        domain: "Please Enter Domain, Required",
                        bucketName: "Please Enter BucketName, Required",
                        basePath: "Please Enter BasePath, Required",
                        tmpPath: "Please Enter TmpPath, Optional",
                        expirationSeconds: "Please Enter ExpirationSeconds, Optional",
                        presignedUrlMethod: "Please Select Presigned Method, Optional",
                        other: 'Please Enter s3 Other Config\uFF0Ceg\uFF1A {xxxx: "yyyy"}'
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
        __name: "magic-s3-datasource-info",
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
            const platform = [
                { text: "minio", value: "minio"},
                { text: "aliyun-oss", value: "aliyun-oss"},
                { text: "tencent-cos", value: "tencent-cos"},
                { text: "aws-s3", value: "aws-s3"}
             ];
            return (_ctx, _cache) => {
                const presignedUrlMethodOptions = [
                    { text: "PUT", value: "PUT" },
                    { text: "POST", value: "POST" }
                ];
                const _component_magic_input = vue.resolveComponent("magic-input");
                const _component_magic_select = vue.resolveComponent("magic-select");
                const _component_magic_monaco_editor = vue.resolveComponent("magic-monaco-editor");
                const _component_magic_checkbox = vue.resolveComponent("magic-checkbox");
                return vue.openBlock(), vue.createElementBlock(vue.Fragment, null, [
                    vue.createElementVNode("div", _hoisted_1$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.name")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).name,
                            "onUpdate:value": _cache[0] || (_cache[0] = ($event) => vue.unref(info).name = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.name"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_2$1, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.key")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).key,
                            "onUpdate:value": _cache[1] || (_cache[1] = ($event) => vue.unref(info).key = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.key"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_4, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.platform")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).platform,
                            "onUpdate:value": _cache[3] || (_cache[3] = ($event) => vue.unref(info).platform = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.platform"),
                            options: platform,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_5, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.accessKey")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).accessKey,
                            "onUpdate:value": _cache[4] || (_cache[4] = ($event) => vue.unref(info).accessKey = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.accessKey"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_6, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.secretKey")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).secretKey,
                            "onUpdate:value": _cache[5] || (_cache[5] = ($event) => vue.unref(info).secretKey = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.secretKey"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),

                    vue.createElementVNode("div", _hoisted_7, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.endPointSchema")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).endPointSchema,
                            "onUpdate:value": _cache[6] || (_cache[6] = ($event) => vue.unref(info).endPointSchema = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.endPointSchema"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_8, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.endPoint")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).endPoint,
                            "onUpdate:value": _cache[7] || (_cache[7] = ($event) => vue.unref(info).endPoint = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.endPoint"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_9, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.domainSchema")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).domainSchema,
                            "onUpdate:value": _cache[8] || (_cache[8] = ($event) => vue.unref(info).domainSchema = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.domainSchema"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_10, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.domain")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).domain,
                            "onUpdate:value": _cache[9] || (_cache[9] = ($event) => vue.unref(info).domain = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.domain"),
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_11, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.bucketName")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).bucketName,
                            "onUpdate:value": _cache[10] || (_cache[10] = ($event) => vue.unref(info).bucketName = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.bucketName"),
                            inputable: "",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_3, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.basePath")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).basePath,
                            "onUpdate:value": _cache[2] || (_cache[2] = ($event) => vue.unref(info).basePath = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.basePath"),
                            inputable: "",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_12, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.tmpPath")), 1),
                        vue.createVNode(_component_magic_input, {
                            value: vue.unref(info).tmpPath,
                            "onUpdate:value": _cache[11] || (_cache[11] = ($event) => vue.unref(info).tmpPath = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.tmpPath"),
                            inputable: "",
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_14, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.presignedUrlMethod")), 1),
                        vue.createVNode(_component_magic_select, {
                            value: vue.unref(info).presignedUrlMethod,
                            "onUpdate:value": _cache[13] || (_cache[13] = ($event) => vue.unref(info).presignedUrlMethod = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.presignedUrlMethod"),
                            options: presignedUrlMethodOptions,
                            width: "100%"
                        }, null, 8, ["value", "placeholder"])
                    ]),
                    vue.createElementVNode("div", _hoisted_13, [
                        vue.createElementVNode("label", null, vue.toDisplayString(vue.unref($i)("s3.datasource.form.other")), 1),
                        vue.createVNode(_component_magic_monaco_editor, {
                            value: vue.unref(info).other,
                            "onUpdate:value": _cache[12] || (_cache[12] = ($event) => vue.unref(info).other = $event),
                            placeholder: vue.unref($i)("s3.datasource.form.placeholder.other"),
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
            var svgDom = document.getElementById("__svg__icons__dom__1717728641709__dys3");
            if (!svgDom) {
                svgDom = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svgDom.style.position = "absolute";
                svgDom.style.width = "0";
                svgDom.style.height = "0";
                svgDom.id = "__svg__icons__dom__1717728641709__dys3";
                svgDom.setAttribute("xmlns", "http://www.w3.org/2000/svg");
                svgDom.setAttribute("xmlns:link", "http://www.w3.org/1999/xlink");
            }
            svgDom.innerHTML = '<symbol fill="none" class="icon" viewBox="0 0 183 135" id="magic-s3-s3"><path d="M179.102 19.3359L135.547 66.4062C140.234 65.3646 144.596 64.8438 148.633 64.8438C157.487 64.8438 164.974 67.1875 171.094 71.875C178.906 77.9948 182.812 86.3281 182.812 96.875C182.812 104.688 180.339 111.914 175.391 118.555C167.057 129.362 154.818 134.766 138.672 134.766C130.599 134.766 123.438 133.398 117.188 130.664C106.901 126.107 101.758 119.271 101.758 110.156C101.758 109.115 101.888 108.073 102.148 107.031C103.32 102.344 106.38 100 111.328 100C113.932 100 116.081 100.586 117.773 101.758C119.987 103.451 121.419 107.487 122.07 113.867C122.461 116.862 122.786 119.01 123.047 120.312C124.609 126.693 128.971 129.883 136.133 129.883C154.883 129.753 164.388 119.661 164.648 99.6094C164.648 94.0104 163.737 88.9974 161.914 84.5703C158.008 75.4557 151.497 70.8984 142.383 70.8984C137.956 70.8984 133.398 71.875 128.711 73.8281L124.219 70.1172L159.18 29.6875H128.32C125.456 29.6875 123.307 29.7526 121.875 29.8828C117.448 30.5339 114.518 33.138 113.086 37.6953L109.57 48.2422H104.883L105.273 15.2344H179.102V19.3359ZM41.9922 0C53.5807 0 64.1927 2.53906 73.8281 7.61719C73.8281 6.3151 73.9583 5.20833 74.2188 4.29688C74.6094 2.08333 76.4974 0.976562 79.8828 0.976562V35.3516H73.6328C73.763 27.9297 71.875 21.6797 67.9688 16.6016C62.3698 9.17969 54.1667 5.46875 43.3594 5.46875C39.1927 5.46875 35.3516 6.1849 31.8359 7.61719C23.763 10.8724 19.7266 16.6667 19.7266 25C19.7266 28.125 20.4427 31.1198 21.875 33.9844C24.8698 39.3229 32.0964 44.5312 43.5547 49.6094C56.3151 55.3385 63.737 58.8542 65.8203 60.1562C78.3203 67.3177 84.5703 76.6276 84.5703 88.0859C84.5703 96.1589 81.6406 103.125 75.7812 108.984C67.4479 117.057 55.7292 121.094 40.625 121.094C27.6042 121.094 16.0807 117.773 6.05469 111.133C6.05469 113.737 5.92448 115.625 5.66406 116.797C5.01302 118.62 3.125 119.531 0 119.531V78.9062H6.25C6.38021 80.2083 6.51042 81.5755 6.64062 83.0078C9.63542 104.753 20.5729 115.625 39.4531 115.625C44.2708 115.625 48.6979 114.779 52.7344 113.086C61.3281 109.18 65.625 102.669 65.625 93.5547C65.625 90.1693 64.8438 87.0443 63.2812 84.1797C60.1562 78.7109 52.7995 73.3724 41.2109 68.1641C28.5807 62.4349 21.1589 58.9193 18.9453 57.6172C6.83594 50.7161 0.78125 41.7969 0.78125 30.8594C0.78125 23.4375 3.45052 17.0573 8.78906 11.7188C16.7318 3.90625 27.7995 0 41.9922 0Z" fill="#6E6E6E"/></symbol>';
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
                type: "s3-datasource",
                icon: "#magic-s3-s3",
                title: i18n.format("s3.datasource.title"),
                name: i18n.format("s3.datasource.name"),
                service: MagicS3DataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
                component: _sfc_main$1
            }]
        };
    };
    return index;
}(Vue);
