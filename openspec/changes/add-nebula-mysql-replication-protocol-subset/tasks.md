## 1. 协议子集设计

- [x] 1.1 定义支持的复制命令、状态机和错误码。
- [x] 1.2 定义 position 与 binlog-compatible stream 的映射。

## 2. 协议实现

- [x] 2.1 实现基础复制握手/注册上下文。
- [x] 2.2 实现 COM_BINLOG_DUMP 或等价读取命令。
- [x] 2.3 实现不支持复制能力的明确错误。

## 3. 兼容验证

- [ ] 3.1 使用至少一个 CDC/复制客户端验证事件读取。
- [x] 3.2 补充 position、断连续读和不支持命令测试。

阻塞说明：
- 3.1 需要前置 MySQL gateway 网络入口和 binlog-compatible stream 最终 API 落地后，才能用真实 CDC/复制客户端做端到端事件读取验收。
- 当前实现只定义 replication 层的 `BinlogStreamReader` / `BinlogStreamCursor` 适配端口，未硬编码 `add-nebula-mysql-binlog-compatible-stream` 分支的未稳定实现类。
