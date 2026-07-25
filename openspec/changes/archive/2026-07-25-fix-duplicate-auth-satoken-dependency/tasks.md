## 1. BOM 重复声明修复

- [x] 1.1 删除 `fw-plus-dependencies` 中重复的 `fw-plus-auth-satoken` dependencyManagement 块，保留唯一版本管理项。

## 2. 验证

- [x] 2.1 执行 `fw-plus-dependencies` Maven 构建，确认构建成功且不再输出重复依赖声明警告。
