/**
 * Magic-API Vue3 网络工具合集
 * =============================
 * HTTP（axios）、SSE（EventSource / fetch）、WebSocket、通用缓存 四合一。
 *
 * ── 快速开始 ──
 *   // 在 index.html 中引入（注意顺序）
 *   <script src="/magic-editor/vue3/js/vue.axios.1.7.0.min.js"></script>
 *   <script src="/magic-editor/vue3/js/network.1.0.0.js"></script>
 *
 *   // 全局配置（可选，在 HTML 中或应用入口设置）
 *   window.MAGIC_API_BASE     = '/magic-api'  // $request 路径前缀
 *   window.MAGIC_API_SSE_BASE = '/magic-api'  // $sse 路径前缀
 *   window.MAGIC_API_WS_BASE  = '/magic-api'  // $ws 路径前缀
 *   window.MAGIC_API_TIMEOUT      = 30000     // $request 请求超时
 *   window.MAGIC_API_SSE_TIMEOUT  = 30000     // $sse 连接超时
 *   window.MAGIC_API_WS_TIMEOUT   = 30000     // $ws 连接超时
 *   // $request 也可运行时配置（$sse/$ws 在 HTML 中设置全局变量）
 *   $request.config({ baseURL: '/magic-api', timeout: 30000 })
 *
 *   // 使用
 *   var users = await $request.get('/api/user/list', { params: { page: 1 } })
 *   var sse = $sse('/api/sse/events', { events: { message: console.log } })
 *   var ws = $ws('ws://localhost:8080/chat', { onMessage: console.log })
 *
 * ── 全局配置项（各自独立，不共享）──
 *   window.MAGIC_API_BASE       $request 的路径前缀（默认 '/magic-api'）
 *   window.MAGIC_API_SSE_BASE   $sse 的路径前缀（默认 '/magic-api'）
 *   window.MAGIC_API_WS_BASE    $ws 的路径前缀（默认 '/magic-api'）
 *   window.MAGIC_API_TIMEOUT      $request 请求超时（默认 30000）
 *   window.MAGIC_API_SSE_TIMEOUT  $sse 连接超时（默认 30000）
 *   window.MAGIC_API_WS_TIMEOUT   $ws 连接超时（默认 30000）
 *
 *   $request 可通过 $request.config({ baseURL, timeout }) 运行时修改。
 *   $sse/$ws 可通过单次 options.baseURL / options.connectTimeout 覆盖，或在 HTML 中设置各自的全局变量。
 *   优先级：单次 options > 各自全局变量 > 默认值 30000 或 '/magic-api'。
 *   绝对路径（含 ://）不做拼接，原样使用。
 *
 * ── 超时说明 ──
 *   $request 的 timeout：请求从发起到收到响应的最大等待时间。
 *   $sse/$ws 的 connectTimeout：建立连接的最大等待时间，超时触发 error 事件并自动重连。
 *   各工具通过各自的全局变量设置默认值（MAGIC_API_TIMEOUT / MAGIC_API_SSE_TIMEOUT / MAGIC_API_WS_TIMEOUT），
 *   单次调用可传入 options.timeout / options.connectTimeout 覆盖。
 *
 * ── 通用模式 ──
 *   // 所有工厂返回的实例都支持：
 *   //   .close()     — 销毁连接/取消请求，不触发 onClose 回调
 *   //   .getState()  — 获取当前状态码
 *
 *   // 在 Vue 组件中使用时，务必在 onBeforeUnmount 中调用 .close() 释放资源：
 *   import { onBeforeUnmount } from 'vue'
 *   const sse = $sse('/api/sse/events', { ... })
 *   onBeforeUnmount(() => { sse.close() })
 *
 * ═══════════════════════════════════════════
 * [1] HTTP 请求 - $request
 * ═══════════════════════════════════════════
 *   基于 axios 封装，支持全局配置、中间件过滤器、自动 baseURL 拼接。
 *   依赖：/magic-editor/vue3/js/vue.axios.1.7.0.min.js
 *   若 axios 未加载，$request 为 null。
 *
 * ── 全局配置：$request.config(opts) ──
 *   在应用入口调用一次，后续所有请求自动应用。支持的字段：
 *
 *     baseURL: '/magic-api'           // 请求路径前缀，所有相对路径自动拼接
 *     timeout: 30000                  // 请求超时 (ms)
 *     headers: { 'Authorization': 'Bearer xxx' }  // 全局请求头，每次请求自动携带
 *
 *     // 拦截器（可选）
 *     onRequest(config) {             // 请求拦截器：在发送前修改 config
 *       config.headers['X-Trace'] = traceId()
 *       return config                 // 必须返回 config
 *     },
 *     onResponse(res) {               // 响应拦截器：统一处理响应
 *       if (res.data.code === 401) logout()
 *       return res                    // 必须返回 res
 *     },
 *     onError(error) {                // 错误拦截器：处理网络/超时错误
 *       ElMessage.error('网络异常')
 *       return Promise.reject(error)
 *     }
 *
 * ── 过滤器：$request.filter(fn) ──
 *   过滤器是洋葱模型中间件，可注册多个，按注册顺序执行。
 *   每个过滤器接收 (config, next)，调用 await next(config) 进入下一层。
 *
 *   $request.filter(async function(config, next) {
 *     // next 之前 → 请求阶段（加 token、trace id、参数校验）
 *     config.headers['Authorization'] = 'Bearer ' + getToken()
 *
 *     var res = await next(config)    // 执行后续过滤器或发送请求
 *
 *     // next 之后 → 响应阶段（统一错误码、格式化）
 *     if (res.data && res.data.code === 401) logout()
 *     return res
 *   })
 *
 *   过滤器只影响通过 $request.get/post/... 发起的请求，
 *   直接使用 $request.instance 发出的请求不经过过滤器。
 *
 * ── 请求方法 ──
 *   所有方法均返回 Promise<axios.Response>。
 *
 *   方法签名：
 *     $request.get(url, cfg)             // GET：查询参数通过 cfg.params 传递
 *     $request.post(url, data, cfg)      // POST：请求体通过 data 传递
 *     $request.put(url, data, cfg)       // PUT：请求体通过 data 传递
 *     $request.patch(url, data, cfg)     // PATCH：请求体通过 data 传递
 *     $request.delete(url, cfg)          // DELETE：查询参数通过 cfg.params 传递
 *     $request.head(url, cfg)            // HEAD：查询参数通过 cfg.params 传递
 *     $request.request(cfg)              // 原始请求（OPTIONS 等，method/url/data 均在 cfg 中）
 *
 * ── 参数传递详解 ──
 *   一次 HTTP 请求涉及三种参数位置：
 *
 *   ① URL 路径参数 — 直接拼在 url 中
 *     $request.get('/api/user/' + userId)
 *     $request.put('/api/user/' + userId, { name: '新名字' })
 *
 *   ② URL 查询参数 (Query String) — 通过 cfg.params 传递，追加到 URL 的 ? 后面
 *     适用于：GET 列表筛选、分页、排序、DELETE 条件等。
 *     所有方法（GET/POST/PUT/DELETE）都可以同时带查询参数。
 *
 *     $request.get('/api/user/list', {
 *       params: { page: 1, size: 20, keyword: '张三', sort: 'age' }
 *     })
 *     // → GET /api/user/list?page=1&size=20&keyword=张三&sort=age
 *
 *     $request.delete('/api/user', {
 *       params: { ids: '1,2,3' }
 *     })
 *     // → DELETE /api/user?ids=1,2,3
 *
 *     // POST 也可以带查询参数（少见但支持）
 *     $request.post('/api/order?action=submit', { items: [...] })
 *
 *  ③ 请求体 (Request Body) — 通过 data 参数传递，放在 HTTP Body 中
 *     适用于：POST/PUT/PATCH 的新增和更新操作。
 *
 *     // JSON 对象（默认 Content-Type: application/json，自动序列化）
 *     $request.post('/api/user', { name: '张三', age: 25, email: 'a@b.com' })
 *     // → POST /api/user   Body: {"name":"张三","age":25,"email":"a@b.com"}
 *
 *     // FormData（文件上传，自动设置 Content-Type: multipart/form-data）
 *     var fd = new FormData(); fd.append('file', file); fd.append('dir', 'avatars')
 *     $request.post('/api/upload', fd)
 *
 *     // URLSearchParams（表单提交，Content-Type: application/x-www-form-urlencoded）
 *     var params = new URLSearchParams(); params.append('name', '张三'); params.append('age', '25')
 *     $request.post('/api/user', params)
 *
 *     // 原始字符串
 *     $request.post('/api/raw', 'plain text', { headers: { 'Content-Type': 'text/plain' } })
 *
 *   ④ GET 请求无 body，DELETE/HEAD 通常也不传 body
 *     $request.get(url, cfg)     — 第二个参数直接是 cfg，没有 data
 *     $request.delete(url, cfg)  — 同上
 *     $request.head(url, cfg)    — 同上
 *
 *   ⑤ 常用组合示例
 *     // 路径参数 + 请求体
 *     $request.put('/api/user/' + userId, { name: '新名字', age: 30 })
 *
 *     // 路径参数 + 查询参数
 *     $request.get('/api/user/' + userId + '/logs', { params: { date: '2026-01', type: 'login' } })
 *
 *     // 请求体 + 查询参数
 *     $request.post('/api/search?index=users', { keyword: '张三', filters: { dept: 'IT' } })
 *
 *   cfg 完整字段参考：
 *   ┌──────────────────┬──────────┬──────────────────────────────────────┐
 *   │ 字段              │ 类型      │ 说明                                  │
 *   ├──────────────────┼──────────┼──────────────────────────────────────┤
 *   │ params            │ object   │ URL 查询参数，拼接到 ? 后面            │
 *   │ data              │ any      │ 请求体（仅 request() 方法用，其他方法已有 data 参数）│
 *   │ headers           │ object   │ 本次请求追加/覆盖的请求头              │
 *   │ timeout           │ number   │ 本次请求超时 (ms)，覆盖全局配置        │
 *   │ responseType      │ string   │ 'blob' / 'arraybuffer' / 'text' / 'json' │
 *   │ baseURL           │ string   │ 本次请求覆盖全局 baseURL              │
 *   │ onUploadProgress  │ function │ 上传进度回调 (e.loaded / e.total)     │
 *   │ onDownloadProgress│ function │ 下载进度回调 (e.loaded / e.total)     │
 *   └──────────────────┴──────────┴──────────────────────────────────────┘
 *
 *   返回值：axios.Response { data, status, statusText, headers, config }
 *
 * ── 使用示例 ──
 *   // 基础查询
 *   var res = await $request.get('/api/user/list', { params: { page: 1, size: 20 } })
 *   console.log(res.data.records)     // 响应数据
 *
 *   // 表单提交
 *   await $request.post('/api/user', { name: '张三', age: 25 })
 *
 *   // 文件上传
 *   var formData = new FormData(); formData.append('file', file)
 *   await $request.post('/api/upload', formData, {
 *     headers: { 'Content-Type': 'multipart/form-data' },
 *     timeout: 60000                  // 上传超时延长到 60s
 *   })
 *
 *   // 文件下载
 *   var res = await $request.get('/api/export', { responseType: 'blob' })
 *   var url = URL.createObjectURL(res.data)
 *   var a = document.createElement('a'); a.href = url; a.download = 'export.xlsx'; a.click()
 *
 *   // 获取响应头
 *   var res = await $request.head('/api/file/123')
 *   console.log(res.headers['content-length'])
 *
 *   // 直接访问 axios 实例（绕过过滤器）
 *   $request.instance.interceptors.request.use(function(config) { ... })
 *
 * ═══════════════════════════════════════════
 * [2] SSE - $sse / $sse.post
 * ═══════════════════════════════════════════
 *   SSE（Server-Sent Events）是服务端向客户端单向推送的技术。
 *   适用场景：实时日志、进度条、通知推送、AI 工作流结果流式输出。
 *   零额外依赖。
 *
 * ── 两种模式对比 ──
 *   ┌──────────────┬─────────────────────────┬──────────────────────────────┐
 *   │              │ GET 模式 ($sse)          │ POST 模式 ($sse.post)         │
 *   ├──────────────┼─────────────────────────┼──────────────────────────────┤
 *   │ 底层实现      │ EventSource（浏览器原生） │ fetch + ReadableStream       │
 *   │ 请求方式      │ 仅 GET                   │ POST，支持 body + headers    │
 *   │ 自定义 headers│ 不支持                   │ 支持（Authorization 等）     │
 *   │ 适用场景      │ 无参数长连接              │ 需要传参（Dify 工作流等）    │
 *   │ 下载进度      │ 浏览器原生管理            │ 手动解析 ReadableStream      │
 *   └──────────────┴─────────────────────────┴──────────────────────────────┘
 *
 * ── 共同选项 ──
 *   ┌──────────────────────┬──────────┬────────┬──────────────────────────────────────────┐
 *   │ 选项                  │ 类型      │ 默认值   │ 说明                                      │
 *   ├──────────────────────┼──────────┼────────┼──────────────────────────────────────────┤
 *   │ reconnect             │ boolean  │ true   │ 断开后是否自动重连                          │
 *   │ reconnectDelay        │ number   │ 3000   │ 断开后等待多久再重连 (ms)                   │
 *   │ reconnectMaxAttempts  │ number   │ 3      │ 最大重连次数，0=无限。达到后 onClose()      │
 *   │ connectTimeout        │ number   │ 30000  │ 连接超时 (ms)，超时→error+重连              │
 *   │ onReconnect(count)    │ function │ noop   │ 重连成功后回调，count=第几次（成功后归零）   │
 *   │ onClose()             │ function │ noop   │ 最终关闭回调（重连耗尽/手动close/complete）  │
 *   │ events                │ object   │ {}     │ 事件回调集合，见下方详细说明                 │
 *   └──────────────────────┴──────────┴────────┴──────────────────────────────────────────┘
 *
 * ── events 事件回调 ──
 *   events 对象中可以定义以下回调函数，按事件名称匹配：
 *
 *     events: {
 *       open() {},              // 连接成功
 *       message(data) {},       // 收到消息（无 event: 字段时），data 自动 JSON.parse
 *       error(e) {},            // 错误通知。⚠️ 仅通知！重连自动处理，不要在此手动重连
 *       custom(data) {},        // 自定义事件（服务端 emitter.send(event().name("custom"))）
 *       done(data) {}           // 同上，事件名由服务端 emitter.event().name("done") 定义
 *     }
 *
 *   注意：error 回调仅作为通知使用，不要在 error 中调用 close() 或手动重连，
 *   框架会自动处理重连逻辑。
 *
 * ── GET 模式详细说明 ──
 *   const sse = $sse('/api/sse/events', {
 *     events: {
 *       open() { console.log('SSE 已连接') },
 *       message(data) { console.log('收到:', data) },
 *       error(e) { console.warn('SSE 错误:', e.message) }
 *     }
 *   })
 *
 *   // 方法
 *   sse.close()                    // 手动关闭，不触发 onClose，不重连
 *   sse.complete()                 // 标记服务端正常完成，后续断连触发 onClose 而不重连
 *   sse.getState()                 // 0=CONNECTING, 1=OPEN, 2=CLOSED
 *
 *   // 重连规则：
 *   //   onopen 后 → 重连计数清零
 *   //   连接断开 → onerror 通知 → 重连
 *   //   重连成功 → onReconnect(count) → 继续接收消息
 *   //   重连耗尽 → onClose() → 停止重连
 *   //   手动 close() → 不触发 onClose，不重连
 *   //   手动 complete() → 下次断连触发 onClose，不重连
 *
 * ── POST 模式详细说明 ──
 *   const sse = $sse.post('/api/runWorkflow', {
 *     body: { workflowKey: 'my-workflow', inputs: { query: 'hello' } },
 *     headers: { 'Authorization': 'Bearer ' + token },
 *     events: {
 *       message(data) { console.log('输出:', data) },
 *       error(e) { console.error('错误:', e.message) },
 *       workflow_finished(data) { console.log('工作流完成:', data) }
 *     }
 *   })
 *
 *   // 方法
 *   sse.close()                    // 手动关闭（底层 abort fetch）
 *   sse.getState()                 // 0=已关闭, 1=连接中
 *
 *   // 重连规则（不同于 GET）：
 *   //   ✅ 重连：fetch 失败（网络不通）、HTTP 4xx/5xx（连接建立前）
 *   //   ❌ 不重连：连接成功后的任何关闭（防止重复提交 POST body）
 *   //   手动 close() → 不触发 onClose，不重连
 *
 * ── 服务端配合（Java SseEmitter）──
 *   // emitter.send(data)                 → events.message(data)
 *   // emitter.send(event().name("x"))    → events.x(data)
 *   // emitter.complete()                 → GET: sse.complete()后→onClose / 未调→重连
 *   //                                    → POST: onClose()，不重连
 *   // emitter.completeWithError(e)       → events.error(e) → GET重连 / POST:onClose
 *
 * ── 使用示例 ──
 *   // 日志流
 *   var logSse = $sse('/api/log/stream', {
 *     events: { message: function(data) { appendLog(data.line) } }
 *   })
 *
 *   // AI 工作流（需传参 → POST 模式）
 *   var aiSse = $sse.post('/api/ai/runWorkflow', {
 *     body: { workflowKey: 'chat', inputs: { prompt: '你好' } },
 *     events: {
 *       message: function(data) { appendMessage(data.text) },
 *       workflow_finished: function(data) { console.log('完成:', data.outputs) },
 *       error: function(e) { showError(e.message) }
 *     }
 *   })
 *
 * ═══════════════════════════════════════════
 * [3] WebSocket - $ws
 * ═══════════════════════════════════════════
 *   全双工通信，双方可随时发送消息。零依赖，自带心跳保活 + 自动重连。
 *
 * ── 选项 ──
 *   const ws = $ws('ws://localhost:8080/chat', {
 *     // ── 重连 ──
 *     reconnect: true,             // 断开后是否自动重连，默认 true
 *     reconnectDelay: 3000,        // 断开后等待多久再重连 (ms)
 *     reconnectMaxAttempts: 3,     // 最大重连次数，0=无限。达到后 onClose()
 *     connectTimeout: 30000,       // 连接超时 (ms)，超时 → onError + onclose → 重连
 *
 *     // ── 心跳 ──
 *     heartbeat: true,             // 是否启用心跳，默认 true
 *     heartbeatInterval: 30000,    // 心跳间隔 (ms)
 *     heartbeatMessage: 'ping',    // 心跳消息内容
 *
 *     // ── 事件回调 ──
 *     onOpen(e) {},                // 连接成功，典型用途：ws.send 订阅频道
 *     onMessage(data, e) {},       // 收到消息，data 自动 JSON.parse，失败原样返回
 *     onError(e) {},               // 错误通知。⚠️ 仅通知！重连由 onclose 自动处理
 *     onClose(e) {},               // 最终关闭。触发：手动 close / 重连耗尽 / reconnect:false
 *     onReconnect(count) {}        // 重连成功后回调，典型用途：重新订阅频道
 *   })
 *
 * ── 方法 ──
 *   ws.send(data)      // 发送消息。对象→JSON.stringify，字符串原样。非 OPEN 状态静默跳过
 *   ws.close()         // 手动关闭，不触发 onClose，不重连
 *   ws.getState()      // 0=CONNECTING, 1=OPEN, 2=CLOSING, 3=CLOSED
 *
 * ── 事件时序 ──
 *   // 正常连接：     connect() → onOpen → onMessage / ws.send → ws.close()
 *   // 异常断开：     onError（通知）→ onclose → 自动重连 → onReconnect → 继续
 *   // 连接超时：     connect() → 超时 → onError("连接超时") → onclose → 自动重连
 *   // 重连耗尽：     重连 N 次 → onClose → 停止
 *   // 手动关闭：     ws.close() → 立即关闭，不触发任何回调
 *   //
 *   // 注：浏览器规范保证 onerror 后必定触发 onclose，
 *   // 因此重连逻辑放在 onclose 中，onError 仅作为通知。
 *
 * ── 使用示例 ──
 *   // 基础使用
 *   var ws = $ws('ws://localhost:8080/chat', {
 *     onMessage: function(data) { appendMessage(data.text) },
 *     onOpen: function() { ws.send({ type: 'subscribe', channel: 'general' }) },
 *     onReconnect: function() { ws.send({ type: 'subscribe', channel: 'general' }) }
 *   })
 *   onBeforeUnmount(() => { ws.close() })
 *
 *   // 关闭心跳（低频消息场景）
 *   var ws = $ws('ws://localhost:8080/events', {
 *     heartbeat: false,
 *     onMessage: function(data) { updateDashboard(data) }
 *   })
 *
 * ═══════════════════════════════════════════
 * 使用场景速查
 * ═══════════════════════════════════════════
 *
 * ── 0. 全局初始化 ──
 *   // 方式一：HTML 中设置（脚本加载前）
 *   <script>
 *   window.MAGIC_API_BASE     = '/magic-api'
 *   window.MAGIC_API_SSE_BASE = '/magic-api'
 *   window.MAGIC_API_WS_BASE  = '/magic-api'
 *   window.MAGIC_API_TIMEOUT     = 30000
 *   window.MAGIC_API_SSE_TIMEOUT = 30000
 *   window.MAGIC_API_WS_TIMEOUT  = 30000
 *   </script>
 *   <script src="/magic-editor/vue3/js/network.1.0.0.js"></script>
 *
 *   // 方式二：应用入口 JS 中配置
 *   if ($request) {
 *     $request.config({
 *       baseURL: '/magic-api',
 *       timeout: 30000,
 *       headers: { 'Authorization': 'Bearer ' + getToken() },
 *       onResponse(res) { if (res.data.code === 401) logout(); return res },
 *       onError(err) { ElMessage.error('网络异常'); return Promise.reject(err) }
 *     })
 *     // 注册认证过滤器
 *     $request.filter(async function(config, next) {
 *       config.headers['Authorization'] = 'Bearer ' + getToken()
 *       return await next(config)
 *     })
 *   }
 *
 * ── 1. 列表查询 + 分页 ──
 *   async function loadUsers(page, size) {
 *     var res = await $request.get('/api/user/list', { params: { page: page, size: size } })
 *     return { records: res.data.records, total: res.data.total }
 *   }
 *   var page1 = await loadUsers(1, 20)
 *
 * ── 2. 增删改查（CRUD）──
 *   // 新增（请求体 JSON）
 *   await $request.post('/api/user', { name: '张三', age: 25, email: 'zhangsan@example.com' })
 *
 *   // 查询单个（路径参数）
 *   var user = await $request.get('/api/user/' + userId)
 *
 *   // 查询列表（查询参数 params → ?page=1&size=20&keyword=张三）
 *   var list = await $request.get('/api/user/list', {
 *     params: { page: 1, size: 20, keyword: '张三' }
 *   })
 *
 *   // 全量更新（路径参数 + 请求体）
 *   await $request.put('/api/user/' + userId, { name: '张三三', age: 26 })
 *
 *   // 局部更新（路径参数 + 请求体，只传要改的字段）
 *   await $request.patch('/api/user/' + userId, { age: 27 })
 *
 *   // 删除（路径参数）
 *   await $request.delete('/api/user/' + userId)
 *
 *   // 批量删除（查询参数传 ID 列表）
 *   await $request.delete('/api/user', { params: { ids: '1,2,3' } })
 *
 * ── 3. 文件上传（带进度条）──
 *   var formData = new FormData(); formData.append('file', file); formData.append('dir', 'avatars')
 *   var res = await $request.post('/api/upload', formData, {
 *     headers: { 'Content-Type': 'multipart/form-data' },
 *     timeout: 120000,                        // 大文件延长超时
 *     onUploadProgress: function(e) {
 *       var pct = Math.round(e.loaded / e.total * 100)
 *       console.log('上传进度: ' + pct + '%')
 *     }
 *   })
 *   console.log('文件URL:', res.data.url)
 *
 * ── 4. 文件下载（带进度条）──
 *   var res = await $request.get('/api/export/report', {
 *     params: { month: '2026-01' },
 *     responseType: 'blob',
 *     onDownloadProgress: function(e) {
 *       console.log('下载: ' + Math.round(e.loaded / e.total * 100) + '%')
 *     }
 *   })
 *   // 触发浏览器下载
 *   var blobUrl = URL.createObjectURL(res.data)
 *   var a = document.createElement('a')
 *   a.href = blobUrl; a.download = 'report.xlsx'; a.click()
 *   URL.revokeObjectURL(blobUrl)
 *
 * ── 5. 并发请求 ──
 *   var [users, roles, depts] = await Promise.all([
 *     $request.get('/api/user/list'),
 *     $request.get('/api/role/list'),
 *     $request.get('/api/dept/list')
 *   ])
 *
 * ── 6. 错误处理 ──
 *   try {
 *     var res = await $request.post('/api/user', data)
 *     ElMessage.success('保存成功')
 *   } catch (err) {
 *     if (err.response) {
 *       // 服务端返回了错误响应
 *       var code = err.response.status
 *       if (code === 400) ElMessage.warning('参数错误')
 *       else if (code === 401) { logout(); ElMessage.error('请重新登录') }
 *       else if (code === 403) ElMessage.error('无权限')
 *       else if (code === 500) ElMessage.error('服务器错误')
 *       else ElMessage.error('请求失败: ' + code)
 *     } else if (err.code === 'ECONNABORTED') {
 *       ElMessage.error('请求超时')
 *     } else {
 *       ElMessage.error('网络连接失败')
 *     }
 *   }
 *
 * ── 7. Token 自动刷新 ──
 *   var isRefreshing = false, pendingRequests = []
 *   $request.filter(async function(config, next) {
 *     // 跳过登录接口本身
 *     if (config.url === '/api/login') return await next(config)
 *
 *     var res = await next(config)
 *     if (res.data.code === 401 && !isRefreshing) {
 *       isRefreshing = true
 *       try {
 *         var newToken = await refreshToken()
 *         config.headers['Authorization'] = 'Bearer ' + newToken
 *         isRefreshing = false
 *         return await next(config)          // 用新 token 重试
 *       } catch (e) {
 *         isRefreshing = false; logout()
 *       }
 *     }
 *     return res
 *   })
 *
 * ── 8. 实时日志 / 终端输出（SSE GET）──
 *   var logContainer = document.getElementById('log')
 *   var logSse = $sse('/api/log/stream', {
 *     connectTimeout: 0,                     // 0=不限时
 *     reconnectMaxAttempts: 0,               // 0=无限重连
 *     events: {
 *       open: function() { logContainer.innerHTML = '' },
 *       message: function(data) {
 *         var div = document.createElement('div')
 *         div.textContent = '[' + data.time + '] ' + data.line
 *         logContainer.appendChild(div)
 *         logContainer.scrollTop = logContainer.scrollHeight
 *       }
 *     }
 *   })
 *   // 切换页面时关闭
 *   onBeforeUnmount(function() { logSse.close() })
 *
 * ── 9. 任务进度条（SSE GET + complete）──
 *   var pct = ref(0), taskDone = ref(false)
 *   var taskSse = $sse('/api/task/progress/' + taskId, {
 *     events: {
 *       message: function(data) { pct.value = data.percent },
 *       done: function() { taskSse.complete(); taskDone.value = true }
 *     },
 *     onClose: function() { if (!taskDone.value) ElMessage.warning('任务监控中断') }
 *   })
 *
 * ── 10. Dify AI 工作流（SSE POST）──
 *   var output = ref('')
 *   var aiSse = $sse.post('/api/ai/runWorkflow', {
 *     body: { workflowKey: 'my-chat', inputs: { query: userInput } },
 *     headers: { 'Authorization': 'Bearer ' + getToken() },
 *     events: {
 *       message: function(data) { output.value += data.text },
 *       workflow_finished: function(data) { console.log('完成:', data.outputs) },
 *       error: function(e) { ElMessage.error('AI 调用失败: ' + e.message) }
 *     }
 *   })
 *
 * ── 11. 系统通知推送（SSE GET，多事件类型）──
 *   var notifications = ref([])
 *   var notifySse = $sse('/api/sse/notifications', {
 *     reconnectMaxAttempts: 0,               // 通知通道无限重连
 *     events: {
 *       info: function(data) { addNotify('info', data) },
 *       warning: function(data) { addNotify('warning', data) },
 *       error: function(data) { addNotify('error', data) },
 *       message: function(data) { addNotify('default', data) }
 *     }
 *   })
 *
 * ── 12. WebSocket 聊天室 ──
 *   var messages = ref([])
 *   var chatWs = $ws('ws://localhost:8080/chat', {
 *     onOpen: function() {
 *       // 连接成功后加入房间
 *       chatWs.send({ type: 'join', roomId: 'room-1', userId: myId })
 *     },
 *     onMessage: function(data) {
 *       messages.value.push({ sender: data.from, text: data.text, time: Date.now() })
 *     },
 *     onReconnect: function(count) {
 *       // 重连后重新加入房间
 *       chatWs.send({ type: 'join', roomId: 'room-1', userId: myId })
 *       console.log('已重连（第' + count + '次）')
 *     }
 *   })
 *   function sendMsg(text) { chatWs.send({ type: 'msg', text: text, roomId: 'room-1' }) }
 *
 * ── 13. WebSocket 实时数据看板 ──
 *   var dashboardData = ref({ cpu: 0, mem: 0, disk: 0, net: 0 })
 *   var dashWs = $ws('ws://monitor:8080/dashboard', {
 *     heartbeat: true,
 *     heartbeatInterval: 30000,              // 30s 心跳保活
 *     heartbeatMessage: 'PING',
 *     onMessage: function(data) { dashboardData.value = data },
 *     reconnectMaxAttempts: 0                // 看板连接无限重连
 *   })
 *
 * ── 14. 低频消息 WebSocket（关闭心跳）──
 *   // 某些场景消息频率很低（如配置变更通知），心跳反而浪费资源
 *   var configWs = $ws('ws://localhost:8080/config', {
 *     heartbeat: false,                       // 关闭心跳
 *     onMessage: function(data) { console.log('配置变更:', data.key) }
 *   })
 *
 * ── 15. Vue 组件中完整示例 ──
 *   <script setup>
 *   import { ref, onBeforeUnmount, watch } from 'vue'
 *
 *   // 列表数据
 *   const loading = ref(false), list = ref([]), total = ref(0)
 *   async function fetchList(page, size) {
 *     loading.value = true
 *     try {
 *       var res = await $request.get('/api/user/list', { params: { page, size } })
 *       list.value = res.data.records; total.value = res.data.total
 *     } catch (e) { ElMessage.error('加载失败') }
 *     finally { loading.value = false }
 *   }
 *
 *   // 实时日志
 *   const logs = ref([])
 *   const logSse = $sse('/api/log/stream', {
 *     events: { message: function(d) { logs.value.push(d); if (logs.value.length > 500) logs.value.shift() } }
 *   })
 *
 *   // 通知 WebSocket
 *   const msgCount = ref(0)
 *   const notifyWs = $ws('ws://host/ws/notify', {
 *     onMessage: function(data) { msgCount.value++ }
 *   })
 *
 *   // 组件卸载时统一释放
 *   onBeforeUnmount(() => { logSse.close(); notifyWs.close() })
 *   </script>
 *
 * ── 16. 无 axios 时的降级处理 ──
 *   // 如果页面不加载 axios，$request 为 null
 *   if (!$request) {
 *     // 降级方案：使用原生 fetch 或仅使用 SSE/WS
 *     console.warn('HTTP 不可用（axios 未加载），仅 SSE/WS 可用')
 *   }
 */
;(function() {
  // ═══ 内部工具 ═══

  /** JSON.parse 安全封装：成功返回解析值，失败返回原值 */
  function tryJson(v) {
    try { return JSON.parse(v) } catch (_) { return v }
  }

  /** 安全触发 events[name](data) */
  function emit(events, name, data) {
    var fn = events[name]; if (fn) { try { fn(data) } catch (_) {} }
  }

  /** 拼接 baseUrl：base + path。base 为空字符串时不拼接。绝对路径（含 ://）原样 */
  function resolveUrl(path, base) {
    if (base == null) base = '/magic-api'
    return (!path || path.indexOf('://') !== -1) ? path : base + path
  }

  /** 从 options 中取 baseURL，未传则回退到全局变量 */
  function pickBase(options, globalBase) {
    return (options && options.baseURL != null) ? options.baseURL : globalBase
  }

  var noop = function() {}

  // ═══ $cache — 通用缓存（localStorage 持久化）═══
  window.$cache = (function() {
    var PREFIX = '$cache.', _mem = {}, _keys = [], _max = 100

    // 启动时从 localStorage 恢复
    try {
      Object.keys(localStorage).forEach(function(k) {
        if (k.indexOf(PREFIX) !== 0) return
        try {
          var entry = JSON.parse(localStorage.getItem(k))
          if (entry.t && Date.now() > entry.t) { localStorage.removeItem(k); return }
          var key = k.slice(PREFIX.length)
          _mem[key] = entry; _keys.push(key)
        } catch (_) { localStorage.removeItem(k) }
      })
    } catch (_) {}

    function save(key, entry) {
      try { localStorage.setItem(PREFIX + key, JSON.stringify(entry)) } catch (_) {}
    }

    return {
      /** 写入缓存。ttl 默认 0=永不过期 (ms)。超容量时淘汰最旧的 */
      set: function(key, value, ttl) {
        var idx = _keys.indexOf(key)
        if (idx !== -1) _keys.splice(idx, 1)
        _keys.push(key)
        var entry = { v: value, t: ttl ? Date.now() + ttl : 0 }
        _mem[key] = entry; save(key, entry)
        while (_keys.length > _max) this.del(_keys[0])
      },
      /** 读取缓存。过期或不存在返回 null。命中提升到最新 */
      get: function(key) {
        var entry = _mem[key], idx = _keys.indexOf(key)
        if (!entry) return null
        if (entry.t && Date.now() > entry.t) { this.del(key); return null }
        if (idx !== -1) { _keys.splice(idx, 1); _keys.push(key) }
        return entry.v
      },
      /** 删除指定 key */
      del: function(key) {
        delete _mem[key]
        var idx = _keys.indexOf(key); if (idx !== -1) _keys.splice(idx, 1)
        try { localStorage.removeItem(PREFIX + key) } catch (_) {}
      },
      /** 清除全部缓存 */
      clear: function() {
        var prefix = PREFIX
        Object.keys(_mem).forEach(function(k) { try { localStorage.removeItem(prefix + k) } catch (_) {} })
        _mem = {}; _keys = []
      },
      /** 设置最大容量（默认 100） */
      maxSize: function(n) { _max = n; while (_keys.length > n) this.del(_keys[0]) }
    }
  })()

  // ═══ 通用工具 ═══

  /** 防抖：延迟 delay ms 后执行，期间重复调用重新计时 */
  window.$debounce = function(fn, delay) {
    var timer
    return function() {
      var ctx = this, args = arguments
      clearTimeout(timer)
      timer = setTimeout(function() { fn.apply(ctx, args) }, delay || 300)
    }
  }

  /** 节流：每 delay ms 最多执行一次 */
  window.$throttle = function(fn, delay) {
    var last = 0
    return function() {
      var now = Date.now()
      if (now - last >= (delay || 300)) { last = now; fn.apply(this, arguments) }
    }
  }

  /** 取 URL 查询参数，不存在返回空字符串 */
  window.$getUrlParam = function(name) {
    var m = location.search.match(new RegExp('[?&]' + name + '=([^&]*)'))
    return m ? decodeURIComponent(m[1]) : ''
  }

  /** 生成唯一 ID（36 进制时间戳 + 随机数） */
  window.$uid = function() {
    return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
  }

  /** 深拷贝（支持 Date/RegExp/Array/普通对象，无法拷贝函数和循环引用） */
  window.$deepClone = function(obj) {
    if (obj == null || typeof obj !== 'object') return obj
    if (obj instanceof Date) return new Date(obj.getTime())
    if (obj instanceof RegExp) return new RegExp(obj.source, obj.flags)
    if (Array.isArray(obj)) return obj.map($deepClone)
    var copy = {}
    for (var k in obj) { if (obj.hasOwnProperty(k)) copy[k] = $deepClone(obj[k]) }
    return copy
  }

  /** 日期格式化：$formatDate(date, 'yyyy-MM-dd hh:mm:ss') */
  window.$formatDate = function(date, fmt) {
    date = date instanceof Date ? date : new Date(date || Date.now())
    fmt = fmt || 'yyyy-MM-dd hh:mm:ss'
    var o = {
      'M+': date.getMonth() + 1, 'd+': date.getDate(),
      'h+': date.getHours(), 'm+': date.getMinutes(), 's+': date.getSeconds(),
      'q+': Math.floor((date.getMonth() + 3) / 3), 'S': date.getMilliseconds()
    }
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').slice(4 - RegExp.$1.length))
    for (var k in o) {
      if (new RegExp('(' + k + ')').test(fmt)) {
        fmt = fmt.replace(RegExp.$1, RegExp.$1.length === 1 ? o[k] : ('00' + o[k]).slice(('' + o[k]).length))
      }
    }
    return fmt
  }

  /** 一键复制文本到剪贴板。支持 Promise 和回调两种方式 */
  window.$copy = function(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(text).then(function() { return true }).catch(function() { return false })
    }
    // 降级方案
    var ta = document.createElement('textarea')
    ta.value = text; ta.style.position = 'fixed'; ta.style.left = '-9999px'
    document.body.appendChild(ta); ta.select()
    var ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return Promise.resolve(ok)
  }

  /** 平铺列表转树形结构 */
  window.$tree = function(list, idKey, pidKey, childrenKey) {
    idKey = idKey || 'id'; pidKey = pidKey || 'parentId'; childrenKey = childrenKey || 'children'
    var map = {}, tree = []
    for (var i = 0; i < list.length; i++) { map[list[i][idKey]] = list[i]; list[i][childrenKey] = [] }
    for (var i = 0; i < list.length; i++) {
      var parent = map[list[i][pidKey]]
      if (parent) { parent[childrenKey].push(list[i]) } else { tree.push(list[i]) }
    }
    return tree
  }

  /** 判断值是否为空：null / undefined / '' / [] / {} */
  window.$isEmpty = function(val) {
    if (val == null || val === '') return true
    if (Array.isArray(val)) return val.length === 0
    if (typeof val === 'object') return Object.keys(val).length === 0
    return false
  }

  /** 异步延时，await $sleep(1000) */
  window.$sleep = function(ms) { return new Promise(function(resolve) { setTimeout(resolve, ms || 0) }) }

  /** 随机整数 [min, max] */
  window.$random = function(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min
  }

  /** localStorage 读取（自动 JSON.parse） */
  window.$getStorage = function(key) {
    try { var v = localStorage.getItem(key); return v ? JSON.parse(v) : null } catch (_) { return null }
  }

  /** localStorage 写入（自动 JSON.stringify） */
  window.$setStorage = function(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); return true } catch (_) { return false }
  }

  // ═══ $request — HTTP（需要 axios）═══
  if (typeof axios !== 'undefined') {
    var instance = axios.create({
      baseURL: window.MAGIC_API_BASE != null ? window.MAGIC_API_BASE : '/magic-api',
      timeout: window.MAGIC_API_TIMEOUT != null ? window.MAGIC_API_TIMEOUT : 30000,
      headers: { 'Content-Type': 'application/json' }
    })

    function requestConfig(opts) {
      if (!opts) return
      if (opts.baseURL != null) instance.defaults.baseURL = opts.baseURL
      if (opts.timeout != null) instance.defaults.timeout = opts.timeout
      if (opts.headers) Object.assign(instance.defaults.headers.common, opts.headers)
      if (opts.onRequest) instance.interceptors.request.use(opts.onRequest)
      if (opts.onResponse) instance.interceptors.response.use(opts.onResponse, undefined)
      if (opts.onError) instance.interceptors.response.use(undefined, opts.onError)
    }

    var filters = [], _send = instance.request.bind(instance), _debounceMap = {}

    function requestFilter(fn) {
      filters.push(fn)
      instance.request = function(config) {
        var i = 0
        function next(cfg) { return i < filters.length ? filters[i++](cfg, next) : _send(cfg) }
        return next(config)
      }
    }

    /** 请求防抖：cfg.debounce 指定延迟 ms，相同 url+params+body 合并为一次请求 */
    function _debounce(method, args) {
      var cfg = args[args.length - 1], delay = cfg && cfg.debounce
      if (!delay) return null
      delete cfg.debounce
      // key = method|url|params|body。用排序 JSON 保证 key 一致性（{a:1,b:2} 和 {b:2,a:1} 相同）
      var toKey = function(v) {
        if (!v || typeof v !== 'object') return ''
        var keys = Object.keys(v).sort(), o = {}
        for (var i = 0; i < keys.length; i++) o[keys[i]] = v[keys[i]]
        return JSON.stringify(o)
      }
      var hasBody = method === 'post' || method === 'put' || method === 'patch'
      var key = method + '|' + args[0] + '|' + toKey(cfg && cfg.params) + '|' + (hasBody ? toKey(args[1]) : '')
      if (_debounceMap[key]) clearTimeout(_debounceMap[key].t)
      return new Promise(function(resolve, reject) {
        _debounceMap[key] = { t: setTimeout(function() {
          delete _debounceMap[key]
          instance[method].apply(instance, args).then(resolve, reject)
        }, delay) }
      })
    }

    window.$request = {
      config: requestConfig, filter: requestFilter, instance: instance,
      get: function(u, c) { return _debounce('get', arguments) || instance.get(u, c) },
      post: function(u, d, c) { return _debounce('post', arguments) || instance.post(u, d, c) },
      put: function(u, d, c) { return _debounce('put', arguments) || instance.put(u, d, c) },
      patch: function(u, d, c) { return _debounce('patch', arguments) || instance.patch(u, d, c) },
      delete: function(u, c) { return _debounce('delete', arguments) || instance.delete(u, c) },
      head: function(u, c) { return instance.head(u, c) },
      request: function(c) { return instance.request(c) }
    }
  } else {
    window.$request = null
  }

  // ═══ $ws — WebSocket ═══
  window.$ws = function(url, options) {
    url = resolveUrl(url, pickBase(options, window.MAGIC_API_WS_BASE))
    var opts = Object.assign({
      reconnect: true, reconnectDelay: 3000, reconnectMaxAttempts: 3,
      connectTimeout: window.MAGIC_API_WS_TIMEOUT != null ? window.MAGIC_API_WS_TIMEOUT : 30000, heartbeat: true, heartbeatInterval: 30000,
      heartbeatMessage: 'ping',
      onMessage: noop, onOpen: noop, onClose: noop, onError: noop, onReconnect: noop
    }, options)

    var ws, heartbeatTimer, reconnectTimer, timeoutTimer, reconnectAttempts = 0, destroyed = false

    function connect() {
      if (destroyed) return
      ws = new WebSocket(url)
      var opened = false
      if (opts.connectTimeout > 0) {
        timeoutTimer = setTimeout(function() {
          if (!opened) { ws.close(); opts.onError(new Error('连接超时')) }
        }, opts.connectTimeout)
      }
      ws.onopen = function(e) {
        clearTimeout(timeoutTimer)
        if (destroyed) { ws.close(); return }
        opened = true
        opts.onOpen(e)
        if (opts.heartbeat) startHeartbeat()
        if (reconnectAttempts > 0) opts.onReconnect(reconnectAttempts)
        reconnectAttempts = 0
      }
      ws.onmessage = function(e) { opts.onMessage(tryJson(e.data || '{}'), e) }
      ws.onerror = function() { clearTimeout(timeoutTimer); opts.onError(new Error('WebSocket 连接错误')) }
      ws.onclose = function(e) {
        clearTimeout(timeoutTimer); clearInterval(heartbeatTimer)
        if (destroyed || !opts.reconnect ||
            (opts.reconnectMaxAttempts > 0 && reconnectAttempts >= opts.reconnectMaxAttempts)) {
          destroyed = true; opts.onClose(e); return
        }
        reconnectAttempts++; reconnectTimer = setTimeout(connect, opts.reconnectDelay)
      }
    }

    function startHeartbeat() {
      clearInterval(heartbeatTimer)
      heartbeatTimer = setInterval(function() {
        if (ws && ws.readyState === WebSocket.OPEN) ws.send(opts.heartbeatMessage)
      }, opts.heartbeatInterval)
    }

    connect()

    return {
      send: function(data) {
        if (!ws || ws.readyState !== WebSocket.OPEN) return
        ws.send(typeof data === 'string' ? data : JSON.stringify(data))
      },
      close: function() {
        destroyed = true; clearTimeout(timeoutTimer); clearTimeout(reconnectTimer)
        clearInterval(heartbeatTimer); if (ws) ws.close()
      },
      getState: function() { return ws ? ws.readyState : WebSocket.CLOSED }
    }
  }

  // ═══ $sse — SSE GET + POST ═══
  ;(function() {
    var DEFAULTS = {
      reconnect: true, reconnectDelay: 3000, reconnectMaxAttempts: 3,
      onReconnect: noop, onClose: noop
      // 注意: connectTimeout 不在此处固化，由工厂函数动态读取全局变量
    }

    // 合并 options + 默认值 + 全局变量回退，同时创建 emitEvent 快捷方法
    function initOpts(options, extra) {
      var opts = Object.assign({}, DEFAULTS, extra, options)
      opts.events = (options && options.events) || {}
      if (opts.connectTimeout == null) opts.connectTimeout = window.MAGIC_API_SSE_TIMEOUT != null ? window.MAGIC_API_SSE_TIMEOUT : 30000
      opts.emitEvent = function(n, d) { emit(opts.events, n, d) }
      return opts
    }

    // ── GET：EventSource ──
    function createGet(url, options) {
      url = resolveUrl(url, pickBase(options, window.MAGIC_API_SSE_BASE))
      var opts = initOpts(options), emitEvent = opts.emitEvent
      var es, reconnectTimer, timeoutTimer, reconnectAttempts = 0
      var destroyed = false, completed = false, timedOut = false

      function connect() {
        if (destroyed) return
        es = new EventSource(url); completed = false; timedOut = false
        if (opts.connectTimeout > 0) {
          timeoutTimer = setTimeout(function() {
            if (es.readyState === EventSource.CONNECTING) {
              timedOut = true; es.close(); emitEvent('error', new Error('连接超时'))
              scheduleReconnect()
            }
          }, opts.connectTimeout)
        }
        es.onopen = function() {
          clearTimeout(timeoutTimer)
          if (destroyed) { es.close(); return }
          emitEvent('open')
          if (reconnectAttempts > 0) opts.onReconnect(reconnectAttempts)
          reconnectAttempts = 0
        }
        Object.keys(opts.events).forEach(function(name) {
          if (name === 'open' || name === 'error' || name === 'close') return
          es.addEventListener(name, function(e) { emitEvent(name, tryJson(e.data)) })
        })
        es.onerror = function() {
          clearTimeout(timeoutTimer)
          if (timedOut) return
          emitEvent('error', new Error('SSE 连接错误'))
          es.close(); scheduleReconnect()
        }
      }

      function scheduleReconnect() {
        if (completed || !opts.reconnect || destroyed) { if (!destroyed) opts.onClose(); return }
        if (opts.reconnectMaxAttempts > 0 && reconnectAttempts >= opts.reconnectMaxAttempts) {
          destroyed = true; opts.onClose(); return
        }
        reconnectAttempts++; reconnectTimer = setTimeout(connect, opts.reconnectDelay)
      }

      connect()

      return {
        complete: function() { completed = true },
        close: function() {
          destroyed = true; clearTimeout(timeoutTimer); clearTimeout(reconnectTimer)
          if (es) es.close()
        },
        getState: function() { return es ? es.readyState : EventSource.CLOSED }
      }
    }

    // ── POST：fetch + ReadableStream ──
    function createPost(url, options) {
      url = resolveUrl(url, pickBase(options, window.MAGIC_API_SSE_BASE))
      var opts = initOpts(options, { body: null, headers: {} }), emitEvent = opts.emitEvent
      var abortCtrl, reconnectTimer, timeoutTimer, reconnectAttempts = 0
      var destroyed = false, connected = false, timedOut = false

      async function connect() {
        if (destroyed) return
        abortCtrl = new AbortController(); connected = false; timedOut = false
        if (opts.connectTimeout > 0) {
          timeoutTimer = setTimeout(function() { timedOut = true; abortCtrl.abort() }, opts.connectTimeout)
        }
        try {
          var res = await fetch(url, {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json', 'Accept': 'text/event-stream' }, opts.headers),
            body: opts.body != null ? JSON.stringify(opts.body) : undefined,
            signal: abortCtrl.signal
          })
          clearTimeout(timeoutTimer)
          if (!res.ok) { emitEvent('error', new Error('HTTP ' + res.status)); scheduleReconnect(); return }
          if (!res.body) { emitEvent('error', new Error('响应体为空')); scheduleReconnect(); return }
          connected = true; emitEvent('open')
          if (reconnectAttempts > 0) opts.onReconnect(reconnectAttempts); reconnectAttempts = 0
          await readStream(res.body.getReader())
          if (!destroyed) opts.onClose()
        } catch (e) {
          clearTimeout(timeoutTimer)
          if (e.name === 'AbortError') {
            if (timedOut) { emitEvent('error', new Error('连接超时')); scheduleReconnect() }
            return
          }
          emitEvent('error', e)
          if (connected) { opts.onClose() } else { scheduleReconnect() }
        }
      }

      async function readStream(reader) {
        var decoder = new TextDecoder(), buffer = ''
        while (true) {
          var chunk = await reader.read()
          if (chunk.done) break
          buffer += decoder.decode(chunk.value, { stream: true })
          buffer = parseSSE(buffer)
        }
      }

      function parseSSE(buffer) {
        var lines = buffer.split('\n'), data = '', eventType = ''
        buffer = lines.pop()
        for (var i = 0; i < lines.length; i++) {
          var line = lines[i]
          if (line[0] === ':') continue
          if (line.startsWith('data:')) data += (data ? '\n' : '') + line.slice(5).replace(/^ /, '')
          else if (line.startsWith('event:')) eventType = line.slice(6).trim()
          else if (line === '' && data) {
            emitEvent(eventType || 'message', tryJson(data))
            data = ''; eventType = ''
          }
        }
        return buffer
      }

      function scheduleReconnect() {
        if (connected || !opts.reconnect || destroyed) return
        if (opts.reconnectMaxAttempts > 0 && reconnectAttempts >= opts.reconnectMaxAttempts) {
          destroyed = true; opts.onClose(); return
        }
        reconnectAttempts++; reconnectTimer = setTimeout(connect, opts.reconnectDelay)
      }

      connect()

      return {
        close: function() {
          destroyed = true; clearTimeout(timeoutTimer); clearTimeout(reconnectTimer)
          if (abortCtrl) abortCtrl.abort()
        },
        getState: function() { return connected ? 1 : 0 }
      }
    }

    window.$sse = function(url, options) { return createGet(url, options) }
    window.$sse.post = function(url, options) { return createPost(url, options) }
  })()
})()
