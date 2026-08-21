package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TinyflowLocalRetrievalOptions {

	private static final int DEFAULT_CANDIDATE_MULTIPLIER = 5;
	private static final int MAX_CANDIDATE_COUNT = 200;
	private static final Set<String> SUPPORTED_KEYS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"retrievalMode", "candidateMultiplier", "maxCandidateCount", "metadataLogic",
			"fusionMode", "vectorWeight", "keywordWeight")));

	private final Mode mode;
	private final int candidateMultiplier;
	private final int maxCandidateCount;
	private final String metadataLogic;
	private final FusionMode fusionMode;
	private final double vectorWeight;
	private final double keywordWeight;

	private TinyflowLocalRetrievalOptions(Mode mode, int candidateMultiplier, int maxCandidateCount,
			String metadataLogic, FusionMode fusionMode, double vectorWeight, double keywordWeight) {
		this.mode = mode;
		this.candidateMultiplier = candidateMultiplier;
		this.maxCandidateCount = maxCandidateCount;
		this.metadataLogic = metadataLogic;
		this.fusionMode = fusionMode;
		this.vectorWeight = vectorWeight;
		this.keywordWeight = keywordWeight;
	}

	public static TinyflowLocalRetrievalOptions from(Map<String, Object> values) {
		Map<String, Object> options = values == null ? Collections.<String, Object>emptyMap() : values;
		for (String key : options.keySet()) {
			if (!SUPPORTED_KEYS.contains(key)) {
				throw new IllegalArgumentException("Unknown retrieval option: " + key);
			}
		}
		Mode mode = enumOption(options, "retrievalMode", Mode.VECTOR, Mode.class);
		int multiplier = integerOption(options, "candidateMultiplier", DEFAULT_CANDIDATE_MULTIPLIER,
				1, Integer.MAX_VALUE);
		int maximum = integerOption(options, "maxCandidateCount", MAX_CANDIDATE_COUNT,
				1, MAX_CANDIDATE_COUNT);
		String logic = options.containsKey("metadataLogic")
				? logicOption(options.get("metadataLogic")) : "and";
		FusionMode fusion = enumOption(options, "fusionMode", FusionMode.RRF, FusionMode.class);
		BigDecimal vector = decimalOption(options, "vectorWeight", new BigDecimal("0.5"));
		BigDecimal keyword = decimalOption(options, "keywordWeight", new BigDecimal("0.5"));
		validateWeight("vectorWeight", vector);
		validateWeight("keywordWeight", keyword);
		if (vector.add(keyword).compareTo(BigDecimal.ONE) != 0) {
			throw new IllegalArgumentException("vectorWeight and keywordWeight must sum to 1");
		}
		return new TinyflowLocalRetrievalOptions(mode, multiplier, maximum, logic, fusion,
				vector.doubleValue(), keyword.doubleValue());
	}

	public Mode getMode() { return mode; }
	public int getCandidateMultiplier() { return candidateMultiplier; }
	public int getMaxCandidateCount() { return maxCandidateCount; }
	public String getMetadataLogic() { return metadataLogic; }
	public FusionMode getFusionMode() { return fusionMode; }
	public double getVectorWeight() { return vectorWeight; }
	public double getKeywordWeight() { return keywordWeight; }

	public int candidateCount(int topK) {
		long multiplied = (long) topK * (long) candidateMultiplier;
		return (int) Math.min(multiplied, maxCandidateCount);
	}

	private static int integerOption(Map<String, Object> options, String key,
			int defaultValue, int minimum, int maximum) {
		if (!options.containsKey(key)) {
			return defaultValue;
		}
		BigDecimal decimal = decimal(options.get(key), key);
		final int value;
		try {
			value = decimal.intValueExact();
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(key + " must be an exact integer", exception);
		}
		if (value < minimum || value > maximum) {
			throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
		}
		return value;
	}

	private static BigDecimal decimalOption(
			Map<String, Object> options, String key, BigDecimal defaultValue) {
		return options.containsKey(key) ? decimal(options.get(key), key) : defaultValue;
	}

	private static BigDecimal decimal(Object value, String key) {
		if (value == null) {
			throw new IllegalArgumentException(key + " must be a finite number");
		}
		try {
			return new BigDecimal(String.valueOf(value).trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(key + " must be a finite number", exception);
		}
	}

	private static void validateWeight(String key, BigDecimal value) {
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException(key + " must be between 0 and 1");
		}
	}

	private static String logicOption(Object value) {
		String logic = value == null ? "null" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
		if (!"and".equals(logic) && !"or".equals(logic)) {
			throw new IllegalArgumentException("Unsupported metadata logic: " + logic);
		}
		return logic;
	}

	private static <E extends Enum<E>> E enumOption(Map<String, Object> options, String key,
			E defaultValue, Class<E> type) {
		if (!options.containsKey(key)) {
			return defaultValue;
		}
		String value = String.valueOf(options.get(key)).trim().toUpperCase(Locale.ROOT);
		try {
			return Enum.valueOf(type, value);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unsupported " + key + ": " + value, exception);
		}
	}

	public enum Mode {
		VECTOR, KEYWORD, HYBRID
	}

	public enum FusionMode {
		RRF, WEIGHTED
	}
}
