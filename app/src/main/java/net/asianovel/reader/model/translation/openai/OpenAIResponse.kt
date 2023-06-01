package net.asianovel.reader.model.translation.openai

import java.util.Date;

class OpenAIResponse {
    var id: String? = null
    var `object`: String? = null
    var creation: Date? = null
    var model: String? = null
    var choicesList: List<Choices>? = null
    var usage: Usage? = null
}