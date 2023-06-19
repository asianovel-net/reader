package net.asianovel.reader.model.translation.openai

import java.util.Date;

class OpenAIResponse {
    var id: String? = null
    var `object`: String? = null
    var model: String? = null
    var choices: List<Choice>? = null
    var usage: Usage? = null
}