package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

/**
 * NL2SQL REST 接口（样例工程组件，非库层自动装配）。
 */
@RestController
@RequestMapping("/nl2sql")
public class Nl2SqlController {

	private final Nl2SqlService nl2SqlService;

	public Nl2SqlController(Nl2SqlService nl2SqlService) {
		this.nl2SqlService = nl2SqlService;
	}

	/** 问答：生成 SQL 并返回执行结果 */
	@PostMapping("/ask")
	public Map<String, Object> ask(@RequestBody QuestionBody body) {
		return nl2SqlService.ask(body.getQuestion());
	}

	/** 只生成 SQL，不执行 */
	@PostMapping("/sql")
	public Map<String, Object> sql(@RequestBody QuestionBody body) {
		return Map.of("question", body.getQuestion(), "sql", nl2SqlService.generateSql(body.getQuestion()));
	}

	/** 直接执行一段只读 SQL */
	@PostMapping("/execute")
	public Map<String, Object> execute(@RequestBody ExecuteBody body) {
		List<Map<String, Object>> rows = nl2SqlService.executeSql(body.getSql());
		return Map.of("sql", body.getSql(), "rows", rows);
	}

	/** 训练表结构（全表，受 table.include/exclude 过滤） */
	@PostMapping("/train")
	public Map<String, Object> train() {
		return Map.of("trainedTables", nl2SqlService.train());
	}

	@Data
	public static class QuestionBody {
		private String question;
	}

	@Data
	public static class ExecuteBody {
		private String sql;
	}
}