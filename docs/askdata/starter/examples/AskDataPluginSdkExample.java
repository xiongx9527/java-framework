import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AskDataPluginSdkExample {
    private AskDataPluginSdkExample() {
    }

    static final class Context {
        final String projectId;
        final String operatorId;

        Context(String projectId, String operatorId) {
            this.projectId = projectId;
            this.operatorId = operatorId;
        }
    }

    interface ModelPlugin {
        String complete(Context context, String modelKey, String prompt);
    }

    interface VectorPlugin {
        List<Double> embed(Context context, String embeddingKey, String text);
    }

    interface SqlPlugin {
        String rewrite(Context context, String sql, Map<String, Object> parameters);
    }

    interface AgentPlugin {
        String run(Context context, String node, String input);
    }

    interface StorePlugin {
        void put(Context context, String key, String value);
        String get(Context context, String key);
    }

    interface ObservationPlugin {
        void record(Context context, String event, Map<String, String> labels);
    }

    static final class DemoPlugins implements ModelPlugin, VectorPlugin, SqlPlugin,
            AgentPlugin, StorePlugin, ObservationPlugin {
        private final Map<String, String> state = new LinkedHashMap<String, String>();

        @Override
        public String complete(Context context, String modelKey, String prompt) {
            return modelKey + ":" + prompt;
        }

        @Override
        public List<Double> embed(Context context, String embeddingKey, String text) {
            return Collections.singletonList((double) (embeddingKey.length() + text.length()));
        }

        @Override
        public String rewrite(Context context, String sql, Map<String, Object> parameters) {
            if (sql.indexOf(';') >= 0 || !sql.trim().toLowerCase().startsWith("select")) {
                throw new IllegalArgumentException("readonly single statement required");
            }
            return sql + " /* named-parameters=" + parameters.size() + " */";
        }

        @Override
        public String run(Context context, String node, String input) {
            return node + ":" + input;
        }

        @Override
        public void put(Context context, String key, String value) {
            state.put(context.projectId + ":" + context.operatorId + ":" + key, value);
        }

        @Override
        public String get(Context context, String key) {
            return state.get(context.projectId + ":" + context.operatorId + ":" + key);
        }

        @Override
        public void record(Context context, String event, Map<String, String> labels) {
            if (labels.containsKey("token") || labels.containsKey("password")) {
                throw new IllegalArgumentException("sensitive labels are forbidden");
            }
        }
    }

    public static void main(String[] args) {
        Context context = new Context("project-a", "operator-a");
        DemoPlugins plugins = new DemoPlugins();
        plugins.complete(context, "ollama.chat.default", "count rows");
        plugins.embed(context, "ollama.embedding.default", "orders");
        plugins.rewrite(context, "select * from orders", Collections.<String, Object>emptyMap());
        plugins.run(context, "sql-node", "orders");
        plugins.put(context, "checkpoint-1", "ready");
        plugins.record(context, "question.completed", Collections.singletonMap("status", "ok"));
        if (!"ready".equals(plugins.get(context, "checkpoint-1"))) {
            throw new IllegalStateException("context-isolated store failed");
        }
        System.out.println("PLUGIN_SDK_EXAMPLE_OK");
    }
}
