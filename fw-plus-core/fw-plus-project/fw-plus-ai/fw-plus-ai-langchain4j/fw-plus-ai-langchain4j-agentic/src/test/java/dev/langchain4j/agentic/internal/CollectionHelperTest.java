package dev.langchain4j.agentic.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Runtime contract for the internal immutable collection factories.
 */
public final class CollectionHelperTest {

    private CollectionHelperTest() {
    }

    public static void main(String[] args) {
        testListOfIsImmutableAndRejectsNulls();
        testListOfVarargsIsASnapshot();
        testListCopyOfIsAnImmutableSnapshot();
        testSetOfRejectsNullsAndDuplicates();
        testSetCopyOfDeduplicatesAndSnapshots();
        testMapOfRejectsNullsAndDuplicateKeys();
        testMapCopyOfIsAnImmutableSnapshot();
        testStreamToListAllowsNullsAndPreservesOrder();
        System.out.println("CollectionHelper contract passed");
    }

    private static void testListOfIsImmutableAndRejectsNulls() {
        assertList(Arrays.asList("a", "a"), CollectionHelper.listOf("a", "a"));
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.listOf("a").add("b");
            }
        });
        assertNullRejected(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.listOf("a", (String) null);
            }
        });
    }

    private static void testListOfVarargsIsASnapshot() {
        String[] values = new String[] {"a", "b"};
        List<String> result = CollectionHelper.listOf(values);
        values[0] = "changed";
        assertList(Arrays.asList("a", "b"), result);
    }

    private static void testListCopyOfIsAnImmutableSnapshot() {
        List<String> source = new java.util.ArrayList<String>(Arrays.asList("a", "b"));
        List<String> result = CollectionHelper.listCopyOf(source);
        source.set(0, "changed");
        assertList(Arrays.asList("a", "b"), result);
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.clear();
            }
        });
        assertNullRejected(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.listCopyOf(Arrays.asList("a", null));
            }
        });
    }

    private static void testSetOfRejectsNullsAndDuplicates() {
        Set<String> result = CollectionHelper.setOf("a", "b");
        assertEquals(Arrays.asList("a", "b"), Arrays.asList(result.toArray()), "Set.of order");
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.add("c");
            }
        });
        assertNullRejected(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.setOf("a", null);
            }
        });
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.setOf("a", "a");
            }
        });
    }

    private static void testSetCopyOfDeduplicatesAndSnapshots() {
        List<String> source = new java.util.ArrayList<String>(Arrays.asList("a", "a", "b"));
        Set<String> result = CollectionHelper.setCopyOf(source);
        source.set(0, "changed");
        assertEquals(2, result.size(), "Set.copyOf duplicate collapse");
        assertEquals(Arrays.asList("a", "b"), Arrays.asList(result.toArray()), "Set.copyOf order");
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.clear();
            }
        });
    }

    private static void testMapOfRejectsNullsAndDuplicateKeys() {
        Map<String, Integer> result = CollectionHelper.mapOf("a", Integer.valueOf(1), "b", Integer.valueOf(2));
        assertEquals(Arrays.asList("a", "b"), Arrays.asList(result.keySet().toArray()), "Map.of order");
        Map<String, Integer> larger = CollectionHelper.mapOf(
                "a", Integer.valueOf(1), "b", Integer.valueOf(2), "c", Integer.valueOf(3), "d", Integer.valueOf(4));
        assertEquals(Arrays.asList("a", "b", "c", "d"), Arrays.asList(larger.keySet().toArray()), "Map.of larger order");
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                larger.put("e", Integer.valueOf(5));
            }
        });
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.put("c", Integer.valueOf(3));
            }
        });
        assertNullRejected(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.mapOf("a", null);
            }
        });
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                CollectionHelper.mapOf("a", Integer.valueOf(1), "a", Integer.valueOf(2));
            }
        });
    }

    private static void testMapCopyOfIsAnImmutableSnapshot() {
        Map<String, Integer> source = new LinkedHashMap<String, Integer>();
        source.put("a", Integer.valueOf(1));
        Map<String, Integer> result = CollectionHelper.mapCopyOf(source);
        source.put("a", Integer.valueOf(2));
        assertEquals(Integer.valueOf(1), result.get("a"), "Map.copyOf snapshot");
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.clear();
            }
        });
        assertNullRejected(new Runnable() {
            @Override
            public void run() {
                Map<String, Integer> invalid = new LinkedHashMap<String, Integer>();
                invalid.put(null, Integer.valueOf(1));
                CollectionHelper.mapCopyOf(invalid);
            }
        });
    }

    private static void testStreamToListAllowsNullsAndPreservesOrder() {
        List<String> result = Stream.of("a", null, "b")
                .collect(CollectionHelper.<String>toUnmodifiableList());
        assertList(Arrays.asList("a", null, "b"), result);
        assertUnsupported(new Runnable() {
            @Override
            public void run() {
                result.add("c");
            }
        });
    }

    private static void assertList(List<String> expected, List<String> actual) {
        assertEquals(expected, actual, "list contents");
    }

    private static void assertUnsupported(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    private static void assertNullRejected(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    private static void assertIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
