package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

/**
 * 会话摘要记录：摘要正文 + 自上次全量重摘以来的增量次数（用于 {@code rebuild-every}）
 * + 已并入摘要的最后一条窗口外历史的身份锚点（写入水位，避免同一批历史被反复精炼）。
 *
 * <p>水位用「身份锚点」而非条数：窗口滑动/驱逐时条数可能不变而内容整体前移，
 * 只有锚点才能正确定位新增轮次。</p>
 */
public final class SummaryRecord {

	/** 空摘要记录：refineCount = 0、无正文、无锚点 */
	public static final SummaryRecord EMPTY = new SummaryRecord("", 0, null);

	private final String summary;

	private final int refineCount;

	/** 已并入摘要的最后一条窗口外历史的身份（{@link ChatMessageIdentities#of} 产出）；无则 null */
	private final String lastCovered;

	public SummaryRecord(String summary, int refineCount) {
		this(summary, refineCount, null);
	}

	public SummaryRecord(String summary, int refineCount, String lastCovered) {
		this.summary = summary == null ? "" : summary;
		this.refineCount = refineCount < 0 ? 0 : refineCount;
		this.lastCovered = lastCovered;
	}

	public String getSummary() {
		return summary;
	}

	public int getRefineCount() {
		return refineCount;
	}

	/** 已并入摘要的最后一条窗口外历史的身份锚点；无则 null */
	public String getLastCovered() {
		return lastCovered;
	}

	public boolean isEmpty() {
		return summary.trim().isEmpty();
	}

	/** 增量精炼：计数 +1，水位保持原值 */
	public SummaryRecord refined(String newSummary) {
		return refined(newSummary, lastCovered);
	}

	/** 增量精炼：计数 +1，并推进水位锚点到 {@code lastCovered} */
	public SummaryRecord refined(String newSummary, String lastCovered) {
		return new SummaryRecord(newSummary, refineCount + 1, lastCovered);
	}

	/** 全量重摘：计数归零，水位保持原值 */
	public SummaryRecord rebuilt(String newSummary) {
		return rebuilt(newSummary, lastCovered);
	}

	/** 全量重摘：计数归零，并推进水位锚点到 {@code lastCovered} */
	public SummaryRecord rebuilt(String newSummary, String lastCovered) {
		return new SummaryRecord(newSummary, 0, lastCovered);
	}

	@Override
	public String toString() {
		return "SummaryRecord{refineCount=" + refineCount + ", lastCovered=" + lastCovered
				+ ", summary=" + summary + "}";
	}

}
