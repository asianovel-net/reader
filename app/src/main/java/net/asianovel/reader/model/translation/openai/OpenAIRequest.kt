package net.asianovel.reader.model.translation.openai


class OpenAIRequest {
    var model: String? = null
    var temperature: Int? = null
    var max_tokens: Int? = null
    var top_p: Int? = null
    var frequency_penalty: Int? = null
    var presence_penalty: Int? = null
    var stop: List<String>? = null
    var messages: List<Message>? = null

    inner class Message (role:String,content:String){
        var role: String = role
        var content: String = content
    }
}

