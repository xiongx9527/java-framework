package cn.afterturn.easypoi.word.entity;

import java.util.Arrays;
import java.util.List;

/**
 * Word 图表数据替换模型，用于 Put 到 Map 中，ParseWord07 自动识别并替换对应图表的嵌入数据。
 * 图表匹配规则：模板中图表的标题文本需与 Map 中的 key 一致。
 */
public class ChartData {

    //private String title;
    private String[] categories;
    private List<Series> series;

    public ChartData() {}

    public ChartData(String[] categories, List<Series> series) {
        this.categories = categories;
        this.series = series;
    }

    //public String getTitle() { return title; }
    //public void setTitle(String title) { this.title = title; }
    public String[] getCategories() { return categories; }
    public void setCategories(String[] categories) { this.categories = categories; }
    public List<Series> getSeries() { return series; }
    public void setSeries(List<Series> series) { this.series = series; }

    public static class Series {
        private String name;
        private Number[] values;

        public Series() {}

        public Series(String name, Number[] values) {
            this.name = name;
            this.values = values;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Number[] getValues() { return values; }
        public void setValues(Number[] values) { this.values = values; }
    }
}
