package stroom.query.impl;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractHtmlBuilder<B extends AbstractHtmlBuilder<?>>  {

    private final StringBuilder sb = new StringBuilder();

    public B append(final String text) {
        sb.append(text);
        return self();
    }

    public B startElem(final String element) {
        sb.append("<");
        sb.append(element);
        sb.append(">");
        return self();
    }

    public B startElem(final String element, final String className) {
        sb.append("<");
        sb.append(element);
        sb.append(" ");
        className(className);
        sb.append(">");
        return self();
    }

    public B endElem(final String element) {
        sb.append("</");
        sb.append(element);
        sb.append(">");
        return self();
    }

    public B emptyElem(final String element) {
        sb.append("<");
        sb.append(element);
        sb.append(" />");
        return self();
    }

    public B emptyElem(final String element, final String className) {
        sb.append("<");
        sb.append(element);
        sb.append(" ");
        className(className);
        sb.append(" />");
        return self();
    }

    private B className(final String className) {
        sb.append("class=\"");
        sb.append(className);
        sb.append("\"");
        return self();
    }

    public B appendLink(final String url, final String title) {
        Objects.requireNonNull(url);
        sb.append("<a href=\"");
        append(url);
        sb.append("\" target=\"_blank\">");
        if (title != null && !title.isEmpty()) {
            append(title);
        }
        sb.append("</a>");
        return self();
    }

    public void elem(final String name, final String className, final Consumer<B> consumer) {
        startElem(name, className);
        consumer.accept(self());
        endElem(name);
    }

    public void elem(final String name, final Consumer<B> consumer) {
        startElem(name);
        consumer.accept(self());
        endElem(name);
    }

    public String toString() {
        return sb.toString();
    }

    public abstract B self();

    public static HtmlBuilderImpl builder() {
        return new HtmlBuilderImpl();
    }

    private static class HtmlBuilderImpl extends AbstractHtmlBuilder<HtmlBuilderImpl> {

        @Override
        public HtmlBuilderImpl self() {
            return this;
        }
    }
}
