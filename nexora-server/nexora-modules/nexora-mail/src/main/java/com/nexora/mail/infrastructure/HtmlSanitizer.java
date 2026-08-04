package com.nexora.mail.infrastructure;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTML 邮件内容清洗工具。
 * 去除 XSS 危险标签/属性，处理 cid: 内联图片，加固外部链接。
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
        // utility class
    }

    /**
     * 清洗 HTML 邮件内容。
     *
     * @param html         原始 HTML
     * @param inlineImages cid → data URI 映射
     * @return 清洗后的 body HTML
     */
    public static String sanitizeHtml(String html, Map<String, String> inlineImages) {
        Document source = Jsoup.parse(html);
        List<Element> headAssets = new ArrayList<>(source.head().select("style,link[rel=stylesheet]"));
        for (int index = headAssets.size() - 1; index >= 0; index--) {
            Element asset = headAssets.get(index);
            asset.remove();
            source.body().prependChild(asset);
        }

        source.select("script,iframe,object,embed,input,button,textarea,select,meta,base").remove();
        source.select("form").unwrap();
        source.select("link:not([rel=stylesheet])").remove();
        for (Element element : source.getAllElements()) {
            List<Attribute> attributes = new ArrayList<>(element.attributes().asList());
            for (Attribute attribute : attributes) {
                String key = attribute.getKey().toLowerCase(Locale.ROOT);
                String value = attribute.getValue().trim();
                if (key.startsWith("on") || isDangerousAttribute(key, value)) {
                    element.removeAttr(attribute.getKey());
                }
            }
        }
        for (Element style : source.select("style")) {
            String css = style.data().toLowerCase(Locale.ROOT);
            if (css.contains("expression(") || css.contains("javascript:")) {
                style.remove();
            }
        }
        for (Element link : source.select("a[href]")) {
            link.attr("target", "_blank");
            link.attr("rel", "noopener noreferrer");
        }
        for (Element image : source.select("img[src]")) {
            String src = image.attr("src").trim();
            String normalizedSrc = src.toLowerCase(Locale.ROOT);
            if (normalizedSrc.startsWith("cid:")) {
                String data = inlineImages.get(src.substring(4));
                if (data == null) {
                    image.removeAttr("src");
                } else {
                    image.attr("src", data);
                }
            } else if (src.startsWith("//")) {
                image.attr("src", "https:" + src);
                image.attr("referrerpolicy", "no-referrer");
            } else if (normalizedSrc.startsWith("http://") || normalizedSrc.startsWith("https://")) {
                image.attr("referrerpolicy", "no-referrer");
            }
        }
        source.outputSettings().prettyPrint(false);
        return source.body().html();
    }

    static boolean isDangerousAttribute(String key, String value) {
        String normalized = value.replaceAll("[\\x00-\\x20]+", "").toLowerCase(Locale.ROOT);
        if ("style".equals(key)) {
            return normalized.contains("expression(") || normalized.contains("javascript:");
        }
        if (!List.of("href", "src", "background", "action", "formaction", "xlink:href").contains(key)) {
            return false;
        }
        return normalized.startsWith("javascript:")
                || normalized.startsWith("vbscript:")
                || normalized.startsWith("data:text/html");
    }
}
