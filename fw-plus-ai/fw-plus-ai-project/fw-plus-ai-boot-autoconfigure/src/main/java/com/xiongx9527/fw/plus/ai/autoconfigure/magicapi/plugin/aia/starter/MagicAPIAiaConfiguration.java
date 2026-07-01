package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.web.MagicAiaController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@EnableConfigurationProperties(MagicAPIAiaConfig.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.aia", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIAiaConfiguration implements MagicPluginConfiguration , WebSocketConfigurer , BaseService {

    /**
     * MagicAPI基础配置属性
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于初始化MagicAPIAiaConfiguration实例
     *
     * @param properties MagicAPIProperties类型的配置属性对象，用于提供必要的配置信息
     */
    public MagicAPIAiaConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 实现PluginConfiguration接口的plugin方法，返回AIA插件实例
     *
     * @return Plugin 返回名为"aia"的插件实例
     */
    @Override
    public Plugin plugin() {
        return new Plugin("aia");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        // 创建并返回一个控制器注册器，用于注册MagicAiaController实例
        return (mapping, configuration) -> mapping.registerController(new MagicAiaController(configuration));
    }

    /**
     * 构建完整的路径，将基础路径和工作空间路径进行拼接
     *
     * @param wsPath 工作空间路径，用于与基础路径拼接
     * @return 拼接后的完整路径字符串
     */
    private String buildFullPath(String wsPath) {
        String webBasePath = properties != null && properties.getWeb() != null ? properties.getWeb() : "";

        // 标准化基础路径结尾是否带 "/"
        boolean baseEndsWithSlash = webBasePath.endsWith("/");
        boolean wsStartsWithSlash = wsPath.startsWith("/");

        StringBuilder sb = new StringBuilder(webBasePath.length() + wsPath.length() + 1);
        sb.append(webBasePath);

        // 处理路径连接处的斜杠，避免出现双斜杠或缺少斜杠的情况
        if (!baseEndsWithSlash && !wsStartsWithSlash) {
            sb.append('/');
        } else if (baseEndsWithSlash && wsStartsWithSlash) {
            sb.setLength(sb.length() - 1); // 移除重复斜杠
        }

        sb.append(wsPath);
        return sb.toString();
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 创建ObjectMapper实例用于JSON序列化
        ObjectMapper objectMapper = new ObjectMapper();
        
        registry.addHandler(new WebSocketHandler(){
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("WebSocket连接已建立");
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String payload = message.getPayload().toString();
                log.info("收到WebSocket消息：{}", payload);
                JSONObject jsonObject = JSONObject.parseObject(payload);
                String content = jsonObject.getString("content");
                String id = jsonObject.getString("id");
                // 生成唯一消息ID
                String messageId = StringUtils.isBlank(id) ? String.valueOf(System.currentTimeMillis()) : id;
                if(StringUtils.isBlank(id)) {
                    long timestamp = System.currentTimeMillis();
                    // 1. 先发送思考状态消息，使用Map对象构建
                    Map<String, Object> thinkingMessage = new HashMap<>();
                    thinkingMessage.put("id", messageId);
                    thinkingMessage.put("content", content);
                    thinkingMessage.put("isUser", false);
                    thinkingMessage.put("timestamp", timestamp);
                    thinkingMessage.put("liked", false);
                    thinkingMessage.put("isThinking", true);
                    // 序列化为JSON字符串
                    String thinkingResponse = objectMapper.writeValueAsString(thinkingMessage);
                    session.sendMessage(new org.springframework.web.socket.TextMessage(thinkingResponse));
                }
                // 2. 延迟一段时间后发送正式响应
                if (StringUtils.isNotBlank(content)){
                    new Thread(() -> {
                        try {
                            // 在生成随机延迟后添加日志打印
                            Random random = new Random();
                            // 生成2秒到2分钟(120秒)之间的随机延迟，单位为毫秒
                            int randomDelay = 2000 + random.nextInt(118001); // 2000ms + 0-118000ms的随机值
                            log.info("AI思考延迟时间: {} 毫秒 ({} 秒)", randomDelay, randomDelay / 1000.0);
                            Thread.sleep(randomDelay);
                            sendTextResponse(session, messageId, objectMapper);
                        } catch (Exception e) {
                            log.error("发送WebSocket响应失败：{}", e.getMessage());
                        }
                    }).start();
                }
            }
            
            /**
             * 发送文本响应
             */
            private void sendTextResponse(WebSocketSession session, String messageId, ObjectMapper objectMapper) throws Exception {
                // 构建文本响应数据
                Map<String, Object> textMessage = new HashMap<>();
                textMessage.put("id", messageId);
                
                // content是文本类型的Map对象
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "text");
                // 示例：在sendTextResponse方法中替换以下代码段
                String[] responses1 = {
                        "# 关于AI发展的深入讨论\n\n近年来，人工智能技术取得了突破性进展，特别是在自然语言处理领域。深度学习模型的不断完善使得AI能够生成更加自然流畅的文本内容。如果您想了解更多最新研究动态，我强烈推荐访问<a href=\"https://arxiv.org/\">arXiv预印本库</a>，这里汇集了全球最前沿的学术论文。同时，参与<a href=\"https://www.kaggle.com\">Kaggle竞赛平台</a>的实际项目也是提升技能的好方法。值得注意的是，开源社区如<a href=\"https://github.com\">GitHub</a>上也有大量优质资源和项目值得探索。\n\n**关键要点：**\n- AI技术在NLP领域快速发展\n- arXiv是最新的学术研究资源\n- Kaggle提供实践机会\n- GitHub有丰富的开源项目",

                        "## 云计算服务全面比较\n\n在选择云计算服务提供商时，需要综合考虑多个因素：性能稳定性、价格体系、技术支持以及生态系统完整性。目前主流云平台包括<a href=\"https://aws.amazon.com\">亚马逊AWS</a>、<a href=\"https://azure.microsoft.com\">微软Azure</a>和<a href=\"https://cloud.google.com\">谷歌云平台</a>。每个平台都有其特色服务，例如AWS的Lambda函数计算、Azure的企业级集成方案以及Google Cloud的机器学习工具集。建议访问各平台官方文档进行详细对比，也可以通过<a href=\"https://www.cloudflare.com\">Cloudflare</a>了解内容分发网络和网络安全相关服务。\n\n### 主要云平台对比：\n| 平台 | 优势领域 | 特色服务 |\n|------|----------|----------|\n| AWS | 全面性、成熟度 | Lambda、S3 |\n| Azure | 企业集成、.NET生态 | Azure Active Directory |\n| Google Cloud | AI/ML、数据分析 | BigQuery、TensorFlow服务 |",

                        "### 开源软件生态深度解析\n\n开源软件已经成为现代软件开发不可或缺的部分。从操作系统层的<a href=\"https://www.linux.org\">Linux内核</a>到应用层的各种框架库，开源文化促进了技术创新和知识共享。对于开发者来说，参与开源项目不仅能提升技术水平，还能建立专业声誉。除了<a href=\"https://github.com\">GitHub</a>外，<a href=\"https://gitlab.com\">GitLab</a>和<a href=\"https://bitbucket.org\">Bitbucket</a>也是优秀的代码托管平台。同时，<a href=\"https://stackoverflow.com\">Stack Overflow</a>社区提供了丰富的技术问答资源，是解决问题的宝贵场所。\n\n```markdown\n开源贡献建议流程：\n1. 寻找感兴趣的项目\n2. 阅读CONTRIBUTING.md\n3. 从简单issue开始\n4. 提交PR并参与讨论\n5. 持续贡献建立信誉\n```",

                        "## 在线教育平台综合推荐\n\n随着在线教育的普及，越来越多高质量学习资源变得触手可及。对于编程初学者，<a href=\"https://www.freecodecamp.org\">freeCodeCamp</a>提供了完整的免费课程体系；对于数据科学爱好者，<a href=\"https://www.datacamp.com\">DataCamp</a>的交互式学习体验非常出色。如果想系统学习计算机科学知识，<a href=\"https://www.coursera.org\">Coursera</a>和<a href=\"https://www.edx.org\">edX</a>上的大学课程是不错选择。此外，<a href=\"https://www.udemy.com\">Udemy</a>上的实用技能课程涵盖范围广泛，适合有特定学习目标的人群。\n\n> **学习建议：** 根据个人学习风格选择合适的平台，理论性课程适合Coursera/edX，实践性技能适合Udemy，编程练习适合freeCodeCamp。",

                        "### 学术研究资源导航\n\n进行学术研究时，获取高质量的参考文献至关重要。除了传统的图书馆资源外，数字化学术平台提供了极大便利。\n- **搜索引擎：** <a href=\"https://scholar.google.com\">Google Scholar</a>是最常用的学术搜索引擎\n- **社交网络：** <a href=\"https://www.researchgate.net\">ResearchGate</a>促进学者交流合作\n- **专业数据库：** \n  - <a href=\"https://www.ieee.org\">IEEE Xplore</a>（工程电子类）\n  - <a href=\"https://www.ncbi.nlm.nih.gov/pubmed\">PubMed</a>（生物医学类）\n- **管理工具：** <a href=\"https://www.mendeley.com\">Mendeley</a>等文献管理工具能显著提高研究效率\n\n**最佳实践：** 建立个人文献数据库，定期跟踪相关领域的最新研究。",

                        "# 科技行业趋势分析\n\n当前科技行业正经历着多轮创新浪潮，人工智能、区块链、物联网和量子计算等前沿技术相互交融。\n\n**跟踪资源的推荐：**\n1. **科技媒体：** <a href=\"https://techcrunch.com\">TechCrunch</a>和<a href=\"https://www.theverge.com\">The Verge</a>\n2. **投资分析：** <a href=\"https://www.bloomberg.com/technology\">Bloomberg科技板块</a>\n3. **社区讨论：** <a href=\"https://news.ycombinator.com\">Hacker News</a>\n4. **深度报道：** <a href=\"https://www.wired.com\">Wired杂志</a>\n\n**重点关注领域：**\n- 生成式AI应用\n- Web3.0和去中心化技术\n- 边缘计算和5G融合\n- 可持续科技解决方案",

                        "## 数字营销工具全指南\n\n在现代数字营销中，合理利用工具可以事半功倍。\n\n**核心工具分类：**\n- **社交媒体管理：** <a href=\"https://buffer.com\">Buffer</a>、<a href=\"https://hootsuite.com\">Hootsuite</a>\n- **网站分析：** <a href=\"https://analytics.google.com\">Google Analytics</a>\n- **邮件营销：** <a href=\"https://mailchimp.com\">Mailchimp</a>\n- **SEO优化：** <a href=\"https://moz.com\">Moz</a>、<a href=\"https://ahrefs.com\">Ahrefs</a>\n- **内容创作：** <a href=\"https://canva.com\">Canva</a>\n\n```\n数字营销工作流示例：\n市场调研 → 内容策划 → 设计制作 → \n多渠道发布 → 数据分析 → 优化调整\n```\n\n**成功关键：** 数据驱动的决策和持续的内容优化。",

                        "### 旅行规划实用资源合集\n\n规划一次完美旅行需要多方面的信息支持。\n\n**按类别推荐：**\n| 需求 | 推荐工具 | 特点 |\n|------|----------|------|\n| 机票比价 | <a href=\"https://www.skyscanner.com\">Skyscanner</a>、<a href=\"https://www.kayak.com\">Kayak</a> | 价格对比全面 |\n| 住宿预订 | <a href=\"https://www.booking.com\">Booking.com</a>、<a href=\"https://www.airbnb.com\">Airbnb</a> | 选择多样化 |\n| 目的地攻略 | <a href=\"https://www.tripadvisor.com\">TripAdvisor</a>、<a href=\"https://www.lonelyplanet.com\">Lonely Planet</a> | 用户评价+专业指南 |\n| 美食探索 | <a href=\"https://www.yelp.com\">Yelp</a> | 本地餐厅评价 |\n| 文化体验 | <a href=\"https://www.culturetrip.com\">Culture Trip</a> | 深度文化推荐 |\n\n> **旅行小贴士：** 提前规划但保持灵活性，关注当地活动和季节性变化。",

                        "## 健康生活方式资源推荐\n\n维持健康生活方式需要科学知识和持续动力。\n\n**应用和工具推荐：**\n- **健身追踪：** <a href=\"https://www.myfitnesspal.com\">MyFitnessPal</a>（营养）、<a href=\"https://www.fitbit.com\">Fitbit</a>（活动）\n- **心理健康：** <a href=\"https://www.headspace.com\">Headspace</a>、<a href=\"https://www.calm.com\">Calm</a>（冥想）\n- **营养知识：** <a href=\"https://www.eatright.org\">Academy of Nutrition and Dietetics</a>（权威信息）\n- **运动教程：** <a href=\"https://www.youtube.com/user/yogawithadriene\">Yoga with Adriene</a>（免费瑜伽）\n- **社交激励：** <a href=\"https://www.strava.com\">Strava</a>（运动社区）\n\n**健康习惯养成策略：**\n1. 设定明确可行的目标\n2. 建立日常规律\n3. 寻找社群支持\n4. 定期评估进度\n5. 奖励自己的进步",

                        "# 全栈开发学习路径规划\n\n要成为一名全栈开发者，需要系统学习前后端技术。\n\n## 学习路线图\n\n### 前端技术栈\n1. **基础三件套：** HTML/CSS/JavaScript（<a href=\"https://developer.mozilla.org\">MDN Web Docs</a>）\n2. **现代框架：** \n   - <a href=\"https://reactjs.org\">React</a> \n   - <a href=\"https://vuejs.org\">Vue.js</a>\n\n### 后端技术栈\n- **运行时：** <a href=\"https://nodejs.org\">Node.js</a>\n- **框架选择：** \n  - <a href=\"https://www.djangoproject.com\">Django</a>（Python）\n  - <a href=\"https://spring.io\">Spring</a>（Java）\n\n### 数据库技术\n- **SQL数据库：** <a href=\"https://www.postgresql.org\">PostgreSQL</a>\n- **NoSQL数据库：** <a href=\"https://www.mongodb.com\">MongoDB</a>\n\n### DevOps与部署\n- **容器化：** <a href=\"https://www.docker.com\">Docker</a>\n- **编排工具：** <a href=\"https://kubernetes.io\">Kubernetes</a>\n\n**学习建议：** 先深度后广度，建立一个完整的项目作品集。"
                };

                String[] responses = {
                        //"## Java集合框架最佳实践\n\n在Java开发中，合理使用集合框架是提高代码质量的关键。以下是常用集合类的选择指南：\n\n```java\nimport java.util.*;\n\npublic class CollectionExamples {\n    // ArrayList vs LinkedList\n    public void listComparison() {\n        // 随机访问多，使用ArrayList\n        List<String> arrayList = new ArrayList<>();\n        arrayList.add(\"元素1\");\n        arrayList.add(\"元素2\");\n        System.out.println(arrayList.get(0)); // O(1)访问\n        \n        // 频繁插入删除，使用LinkedList\n        List<String> linkedList = new LinkedList<>();\n        linkedList.add(\"节点A\");\n        linkedList.add(\"节点B\");\n        linkedList.add(1, \"插入节点\"); // O(1)插入\n    }\n    \n    // HashMap使用示例\n    public void mapExample() {\n        Map<String, Integer> wordCount = new HashMap<>();\n        wordCount.put(\"apple\", 5);\n        wordCount.put(\"banana\", 3);\n        \n        // Java 8+ 的lambda遍历\n        wordCount.forEach((key, value) -> \n            System.out.println(key + \": \" + value));\n    }\n}\n```\n\n**更多资源：**\n- 官方文档：<a href=\"https://docs.oracle.com/javase/8/docs/technotes/guides/collections/index.html\">Java Collections Framework</a>\n- 性能分析：<a href=\"https://www.baeldung.com/java-collections-complexity\">集合复杂度分析</a>",

                        //"### JavaScript异步编程模式\n\n现代JavaScript提供了多种异步处理方式，从回调到async/await的演进：\n\n```javascript\n// 1. 回调函数（Callback Hell）\nfunction getUserData(userId, callback) {\n    fetch(`/api/users/${userId}`)\n        .then(response => response.json())\n        .then(data => {\n            fetch(`/api/posts/${data.id}`)\n                .then(res => res.json())\n                .then(posts => callback(null, { user: data, posts }))\n                .catch(err => callback(err));\n        })\n        .catch(err => callback(err));\n}\n\n// 2. Promise链式调用\nfunction getUserDataPromise(userId) {\n    return fetch(`/api/users/${userId}`)\n        .then(response => response.json())\n        .then(userData => \n            fetch(`/api/posts/${userData.id}`)\n                .then(res => res.json())\n        )\n        .then(posts => ({ user: userData, posts }));\n}\n\n// 3. async/await（推荐）\nasync function getUserDataAsync(userId) {\n    try {\n        const userResponse = await fetch(`/api/users/${userId}`);\n        const userData = await userResponse.json();\n        \n        const postsResponse = await fetch(`/api/posts/${userData.id}`);\n        const posts = await postsResponse.json();\n        \n        return { user: userData, posts };\n    } catch (error) {\n        console.error('获取数据失败:', error);\n        throw error;\n    }\n}\n\n// 4. 并行请求优化\nasync function getDashboardData(userId) {\n    const [userReq, postsReq] = await Promise.all([\n        fetch(`/api/users/${userId}`),\n        fetch(`/api/posts?userId=${userId}`)\n    ]);\n    \n    const user = await userReq.json();\n    const posts = await postsReq.json();\n    \n    return { user, posts };\n}\n```\n\n**学习资源：**\n- MDN文档：<a href=\"https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Guide/Using_promises\">Promise指南</a>\n- 异步模式：<a href=\"https://javascript.info/async\">现代异步JavaScript</a>",

                        //"#### REST API设计与JSON响应规范\n\n设计良好的RESTful API需要清晰的JSON响应结构：\n\n```json\n// 成功响应示例\n{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"操作成功\",\n  \"data\": {\n    \"user\": {\n      \"id\": \"12345\",\n      \"username\": \"john_doe\",\n      \"email\": \"john@example.com\",\n      \"profile\": {\n        \"avatar\": \"https://example.com/avatar.jpg\",\n        \"bio\": \"全栈开发者\"\n      },\n      \"created_at\": \"2024-01-15T10:30:00Z\",\n      \"updated_at\": \"2024-03-20T14:25:00Z\"\n    },\n    \"metadata\": {\n      \"total_count\": 1,\n      \"page\": 1,\n      \"per_page\": 20\n    }\n  },\n  \"links\": {\n    \"self\": \"/api/v1/users/12345\",\n    \"posts\": \"/api/v1/users/12345/posts\",\n    \"followers\": \"/api/v1/users/12345/followers\"\n  }\n}\n\n// 错误响应示例\n{\n  \"status\": \"error\",\n  \"code\": 404,\n  \"message\": \"用户不存在\",\n  \"errors\": [\n    {\n      \"field\": \"user_id\",\n      \"message\": \"指定的用户ID不存在\",\n      \"code\": \"USER_NOT_FOUND\"\n    }\n  ],\n  \"timestamp\": \"2024-03-21T09:15:30Z\",\n  \"request_id\": \"req_abc123xyz456\"\n}\n\n// 分页响应示例\n{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"data\": {\n    \"items\": [\n      {\"id\": 1, \"title\": \"文章标题1\", \"author\": \"作者A\"},\n      {\"id\": 2, \"title\": \"文章标题2\", \"author\": \"作者B\"}\n    ],\n    \"pagination\": {\n      \"total\": 150,\n      \"page\": 1,\n      \"per_page\": 20,\n      \"total_pages\": 8,\n      \"has_next\": true,\n      \"has_prev\": false\n    }\n  }\n}\n```\n\n**API设计原则：**\n- 一致性：保持响应结构统一\n- 可读性：使用清晰的字段名\n- 版本控制：API版本管理\n- 文档化：提供完整API文档\n\n**推荐工具：**\n- <a href=\"https://swagger.io\">Swagger/OpenAPI</a> - API文档生成\n- <a href=\"https://www.postman.com\">Postman</a> - API测试\n- <a href=\"https://insomnia.rest\">Insomnia</a> - API客户端",

                        //"## Spring Boot配置最佳实践\n\nSpring Boot提供了灵活的配置方式，以下是一些实用示例：\n\n```java\n// application.yml - 多环境配置\nspring:\n  profiles:\n    active: @activatedProperties@  # Maven/Gradle属性替换\n\n---\n# 开发环境配置\nspring:\n  config:\n    activate:\n      on-profile: dev\n  datasource:\n    url: jdbc:h2:mem:testdb\n    username: sa\n    password: \n    driver-class-name: org.h2.Driver\n  jpa:\n    hibernate:\n      ddl-auto: create-drop\n    show-sql: true\n    properties:\n      hibernate:\n        format_sql: true\n\nlogging:\n  level:\n    com.example: DEBUG\n    org.springframework.web: INFO\n\nserver:\n  port: 8080\n  servlet:\n    context-path: /api\n\n---\n# 生产环境配置\nspring:\n  config:\n    activate:\n      on-profile: prod\n  datasource:\n    url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}\n    username: ${DB_USERNAME}\n    password: ${DB_PASSWORD}\n    hikari:\n      maximum-pool-size: 20\n      connection-timeout: 30000\n\n  redis:\n    host: ${REDIS_HOST:localhost}\n    port: ${REDIS_PORT:6379}\n    password: ${REDIS_PASSWORD:}\n\n# 自定义配置\napp:\n  security:\n    jwt:\n      secret: ${JWT_SECRET:defaultSecretKey}\n      expiration: 86400000  # 24小时\n\n  cors:\n    allowed-origins: \n      - \"https://example.com\"\n      - \"https://api.example.com\"\n    allowed-methods: \n      - GET\n      - POST\n      - PUT\n      - DELETE\n\n  rate-limit:\n    enabled: true\n    requests-per-minute: 100\n```\n\n```java\n// 配置类示例\n@Configuration\n@ConfigurationProperties(prefix = \"app.security.jwt\")\n@Data\npublic class JwtConfig {\n    private String secret;\n    private Long expiration;\n    private String issuer = \"myapp\";\n}\n\n// 使用@Value注入\n@Component\npublic class ApiConfig {\n    @Value(\"${app.rate-limit.requests-per-minute}\")\n    private int requestsPerMinute;\n    \n    @Value(\"${app.cors.allowed-origins}\")\n    private List<String> allowedOrigins;\n}\n\n// 多环境Bean配置\n@Configuration\npublic class DataSourceConfig {\n    \n    @Bean\n    @Profile(\"dev\")\n    public DataSource devDataSource() {\n        return DataSourceBuilder.create()\n            .url(\"jdbc:h2:mem:testdb\")\n            .username(\"sa\")\n            .build();\n    }\n    \n    @Bean\n    @Profile(\"prod\")\n    public DataSource prodDataSource(\n            @Value(\"${spring.datasource.url}\") String url,\n            @Value(\"${spring.datasource.username}\") String username,\n            @Value(\"${spring.datasource.password}\") String password) {\n        HikariDataSource dataSource = new HikariDataSource();\n        dataSource.setJdbcUrl(url);\n        dataSource.setUsername(username);\n        dataSource.setPassword(password);\n        dataSource.setMaximumPoolSize(20);\n        return dataSource;\n    }\n}\n```\n\n**学习资源：**\n- 官方文档：<a href=\"https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config\">Spring Boot配置</a>\n- 最佳实践：<a href=\"https://www.baeldung.com/spring-boot-configuration\">配置管理指南</a>",

                        "### React Hooks使用模式\n\nReact Hooks彻底改变了函数组件的开发方式，以下是一些常用模式：\n\n```jsx\nimport React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';\nimport axios from 'axios';\n\n// 自定义Hook：数据获取\nfunction useFetch(url, options = {}) {\n    const [data, setData] = useState(null);\n    const [loading, setLoading] = useState(true);\n    const [error, setError] = useState(null);\n    \n    useEffect(() => {\n        const fetchData = async () => {\n            try {\n                setLoading(true);\n                const response = await axios.get(url, options);\n                setData(response.data);\n                setError(null);\n            } catch (err) {\n                setError(err.message || '请求失败');\n                setData(null);\n            } finally {\n                setLoading(false);\n            }\n        };\n        \n        fetchData();\n    }, [url, JSON.stringify(options)]); // 依赖项\n    \n    return { data, loading, error, refetch: () => fetchData() };\n}\n\n// 自定义Hook：本地存储\nfunction useLocalStorage(key, initialValue) {\n    const [storedValue, setStoredValue] = useState(() => {\n        try {\n            const item = window.localStorage.getItem(key);\n            return item ? JSON.parse(item) : initialValue;\n        } catch (error) {\n            console.error(error);\n            return initialValue;\n        }\n    });\n    \n    const setValue = useCallback((value) => {\n        try {\n            const valueToStore = value instanceof Function ? value(storedValue) : value;\n            setStoredValue(valueToStore);\n            window.localStorage.setItem(key, JSON.stringify(valueToStore));\n        } catch (error) {\n            console.error(error);\n        }\n    }, [key, storedValue]);\n    \n    return [storedValue, setValue];\n}\n\n// 组件示例：用户列表\nfunction UserList() {\n    const [searchTerm, setSearchTerm] = useState('');\n    const [page, setPage] = useState(1);\n    const inputRef = useRef(null);\n    \n    const { data: users, loading, error } = useFetch(\n        `/api/users?page=${page}&search=${searchTerm}`\n    );\n    \n    const [favorites, setFavorites] = useLocalStorage('user_favorites', []);\n    \n    // 记忆化的过滤用户\n    const filteredUsers = useMemo(() => {\n        if (!users) return [];\n        return users.filter(user => \n            user.name.toLowerCase().includes(searchTerm.toLowerCase())\n        );\n    }, [users, searchTerm]);\n    \n    // 聚焦输入框\n    useEffect(() => {\n        if (inputRef.current) {\n            inputRef.current.focus();\n        }\n    }, []);\n    \n    const toggleFavorite = useCallback((userId) => {\n        setFavorites(prev => {\n            if (prev.includes(userId)) {\n                return prev.filter(id => id !== userId);\n            } else {\n                return [...prev, userId];\n            }\n        });\n    }, [setFavorites]);\n    \n    if (loading) return <div>加载中...</div>;\n    if (error) return <div>错误: {error}</div>;\n    \n    return (\n        <div className=\"user-list\">\n            <input\n                ref={inputRef}\n                type=\"text\"\n                value={searchTerm}\n                onChange={(e) => setSearchTerm(e.target.value)}\n                placeholder=\"搜索用户...\"\n            />\n            \n            <div className=\"pagination\">\n                <button onClick={() => setPage(p => Math.max(1, p - 1))}>\n                    上一页\n                </button>\n                <span>第 {page} 页</span>\n                <button onClick={() => setPage(p => p + 1)}>\n                    下一页\n                </button>\n            </div>\n            \n            <ul>\n                {filteredUsers.map(user => (\n                    <li key={user.id}>\n                        <span>{user.name}</span>\n                        <button \n                            onClick={() => toggleFavorite(user.id)}\n                            className={favorites.includes(user.id) ? 'favorited' : ''}\n                        >\n                            {favorites.includes(user.id) ? '★' : '☆'}\n                        </button>\n                    </li>\n                ))}\n            </ul>\n        </div>\n    );\n}\n\nexport default UserList;\n```\n\n```css\n/* 对应的CSS样式 */\n.user-list {\n    max-width: 800px;\n    margin: 0 auto;\n    padding: 20px;\n}\n\n.user-list input {\n    width: 100%;\n    padding: 10px;\n    margin-bottom: 20px;\n    border: 1px solid #ddd;\n    border-radius: 4px;\n}\n\n.user-list ul {\n    list-style: none;\n    padding: 0;\n}\n\n.user-list li {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    padding: 10px;\n    border-bottom: 1px solid #eee;\n}\n\n.user-list li:hover {\n    background-color: #f5f5f5;\n}\n\n.user-list button {\n    background: none;\n    border: none;\n    cursor: pointer;\n    font-size: 20px;\n    color: #ccc;\n}\n\n.user-list button.favorited {\n    color: #ffd700;\n}\n\n.pagination {\n    display: flex;\n    justify-content: center;\n    align-items: center;\n    gap: 20px;\n    margin-bottom: 20px;\n}\n\n.pagination button {\n    padding: 5px 15px;\n    border: 1px solid #007bff;\n    background: white;\n    color: #007bff;\n    border-radius: 4px;\n    cursor: pointer;\n}\n\n.pagination button:hover {\n    background: #007bff;\n    color: white;\n}\n```\n\n**学习资源：**\n- React官方：<a href=\"https://reactjs.org/docs/hooks-intro.html\">Hooks介绍</a>\n- 自定义Hooks：<a href=\"https://usehooks.com\">useHooks集合</a>\n- 最佳实践：<a href=\"https://kentcdodds.com/blog/usememo-and-usecallback\">useMemo和useCallback指南</a>"
                };
                Random random = new Random();
                String responseText = responses[random.nextInt(responses.length)];
                textContent.put("text", responseText);
                textMessage.put("content", textContent);
                
                textMessage.put("isUser", false);
                textMessage.put("timestamp", System.currentTimeMillis());
                textMessage.put("liked", false);
                textMessage.put("isThinking", false);
                
                // 序列化为JSON字符串并发送
                String textResponse = objectMapper.writeValueAsString(textMessage);
                session.sendMessage(new org.springframework.web.socket.TextMessage(textResponse));
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.error("WebSocket传输错误：{}", exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                log.info("WebSocket连接已关闭，状态：{}", closeStatus);
            }
            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, buildFullPath("/ws/aia/chat")).setAllowedOrigins("*");
    }
}
