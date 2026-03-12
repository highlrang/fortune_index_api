package com.hwcompany.fortune_index.auth

import com.fasterxml.jackson.databind.ObjectMapper

object JsonTestUtils {
    private val objectMapper = ObjectMapper()

    fun readJson(json: String, path: String): String {
        val fields = path.removePrefix("$.").split(".")
        val node = fields.fold(objectMapper.readTree(json)) { current, field ->
            current.get(field)
        }
        return node.asText()
    }
}
