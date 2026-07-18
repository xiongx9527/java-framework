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
- 已将当前分支合入 `dev`（合并前工作区干净）并对接已归档的 binlog-compatible stream 稳定 API：
  `BinlogEventReaderStreamReader` 通过 `BinlogEventReader` 读取 `NebulaBinlogEvent`，转换为 replication 层事件并推进兼容 position。
- 已补充 adapter 级 CDC/replication-style 自动化测试 `BinlogEventReaderStreamReaderTest`，验证事件读取、类型映射、payload 透传和 position 推进。
- 3.1 仍未勾选：当前合入后的代码中仍未发现可运行的 MySQL gateway 网络入口，无法用真实 CDC/复制客户端完成端到端事件读取验收；不得用 adapter 测试伪造真实客户端验收。
