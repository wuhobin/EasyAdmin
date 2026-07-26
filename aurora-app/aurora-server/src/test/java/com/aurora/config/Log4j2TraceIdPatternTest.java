package com.aurora.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2TraceIdPatternTest {

    @Test
    void infoFileAcceptsInfoAndWarnButRejectsError() throws Exception {
        Element filter = findAppender(loadDocument(), "InfoFile")
                .getElementsByTagName("LevelRangeFilter")
                .item(0) instanceof Element element ? element : null;
        assertThat(filter).isNotNull();
        Level minLevel = Level.getLevel(filter.getAttribute("minLevel"));
        Level maxLevel = Level.getLevel(filter.getAttribute("maxLevel"));

        assertThat(Level.INFO.isInRange(minLevel, maxLevel)).isTrue();
        assertThat(Level.WARN.isInRange(minLevel, maxLevel)).isTrue();
        assertThat(Level.ERROR.isInRange(minLevel, maxLevel)).isFalse();
    }

    @Test
    void errorFileAcceptsErrorAndFatalButRejectsWarn() throws Exception {
        Element filter = findAppender(loadDocument(), "ErrorFile")
                .getElementsByTagName("LevelRangeFilter")
                .item(0) instanceof Element element ? element : null;
        assertThat(filter).isNotNull();
        Level minLevel = Level.getLevel(filter.getAttribute("minLevel"));
        Level maxLevel = Level.getLevel(filter.getAttribute("maxLevel"));

        assertThat(Level.ERROR.isInRange(minLevel, maxLevel)).isTrue();
        assertThat(Level.FATAL.isInRange(minLevel, maxLevel)).isTrue();
        assertThat(Level.WARN.isInRange(minLevel, maxLevel)).isFalse();
    }

    @Test
    void rollingFilesUseDailyRolloverAndSevenDayRetention() throws Exception {
        Document document = loadDocument();
        assertThat(findProperty(document, "LOG_RETENTION"))
                .isEqualTo("${env:LOG_RETENTION:-7d}");

        assertRolloverSettings(findAppender(document, "InfoFile"), "info.*.log.gz");
        assertRolloverSettings(findAppender(document, "ErrorFile"), "error.*.log.gz");
    }

    @Test
    void rendersTraceIdFromMdc() throws Exception {
        String pattern = loadPattern();
        SortedArrayStringMap contextData = new SortedArrayStringMap();
        contextData.putValue("traceId", "trace-123");
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("test.logger")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("test message"))
                .setContextData(contextData)
                .build();

        String logLine = PatternLayout.newBuilder()
                .withPattern(pattern)
                .build()
                .toSerializable(event);

        assertThat(logLine).contains("[trace-123]");
    }

    private static String loadPattern() throws Exception {
        return findProperty(loadDocument(), "PATTERN");
    }

    private static String findProperty(Document document, String name) {
        NodeList properties = document.getElementsByTagName("Property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (name.equals(property.getAttribute("name"))) {
                return property.getTextContent();
            }
        }
        throw new IllegalStateException(name + " property not found");
    }

    private static Element findAppender(Document document, String name) {
        NodeList appenders = document.getElementsByTagName("RollingFile");
        for (int i = 0; i < appenders.getLength(); i++) {
            Element appender = (Element) appenders.item(i);
            if (name.equals(appender.getAttribute("name"))) {
                return appender;
            }
        }
        throw new IllegalStateException(name + " appender not found");
    }

    private static void assertRolloverSettings(Element appender, String fileGlob) {
        Element timePolicy = (Element) appender.getElementsByTagName("TimeBasedTriggeringPolicy").item(0);
        assertThat(timePolicy.getAttribute("interval")).isEqualTo("1");
        assertThat(timePolicy.getAttribute("modulate")).isEqualTo("true");

        Element delete = (Element) appender.getElementsByTagName("Delete").item(0);
        Element fileName = (Element) delete.getElementsByTagName("IfFileName").item(0);
        Element lastModified = (Element) fileName.getElementsByTagName("IfLastModified").item(0);
        assertThat(fileName.getAttribute("glob")).isEqualTo(fileGlob);
        assertThat(lastModified.getAttribute("age")).isEqualTo("${LOG_RETENTION}");
    }

    private static Document loadDocument() throws Exception {
        try (InputStream input = Log4j2TraceIdPatternTest.class.getResourceAsStream("/log4j2-spring.xml")) {
            assertThat(input).isNotNull();
            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(input);
        }
    }
}
