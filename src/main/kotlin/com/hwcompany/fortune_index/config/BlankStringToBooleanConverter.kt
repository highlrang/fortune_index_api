package com.hwcompany.fortune_index.config

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesBinding
class BlankStringToBooleanConverter : Converter<String, Boolean?> {
    override fun convert(source: String): Boolean? {
        val value = source.trim()
        if (value.isEmpty()) {
            return null
        }

        return value.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("불리언 값이 올바르지 않습니다: $source")
    }
}
