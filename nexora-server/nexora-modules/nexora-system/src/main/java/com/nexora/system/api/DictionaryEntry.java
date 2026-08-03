package com.nexora.system.api;

/**
 * 可供其他领域读取的字典条目。
 */
public record DictionaryEntry(String label, String value, boolean defaultEntry) {
}
