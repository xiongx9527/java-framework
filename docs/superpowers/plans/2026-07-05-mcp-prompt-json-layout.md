---
archived-with: 2026-07-05-fix-mcp-prompt-json-layout
status: final
---
# MCP Prompt JSON Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修正 MCP Prompt 参数/消息布局为 JSON 组件，并补齐结构化 MCP Prompt 数据契约。

**Architecture:** 前端使用 JSON 编辑组件管理 `arguments` 与 `messages`；后端 provider 保持 JSON 字符串兼容并验证嵌套渲染。

**Tech Stack:** Vue 3, MagicAPI console plugin, Java 8, JUnit 5, Mockito/Maven.

## Global Constraints

- 不改 MCP JSON-RPC 方法名。
- 不引入新前端依赖。
- 保持 `argumentsJson` / `messagesJson` 兼容。

---

### Task 1: Provider contract tests

- [x] Add nested structured message rendering test
- [x] Add required argument parsing assertion
- [x] Run targeted provider tests

### Task 2: Console JSON layout

- [x] Replace prompt textarea controls with JSON component/editor layout
- [x] Update default prompt data to structured MCP content
- [x] Update i18n help text if needed

### Task 3: Docs and verification

- [x] Update MCP README prompt JSON examples
- [x] Run targeted MCP prompt tests
- [x] Update OpenSpec tasks and run build guard
