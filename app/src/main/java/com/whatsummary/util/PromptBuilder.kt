package com.whatsummary.util

import com.whatsummary.data.db.entity.CapturedMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PromptBuilder {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun buildSummaryPrompt(
        groupName: String,
        date: String,
        messages: List<CapturedMessage>
    ): String {
        val formattedMessages = formatMessages(messages)
        val count = messages.size

        return """
Você é um assistente que resume conversas de grupo do WhatsApp.

Regras:
1. Resuma em português brasileiro, de forma concisa e objetiva.
2. Estruture o resumo nas seguintes seções (inclua apenas as que tiverem conteúdo):
   - 📋 Principais assuntos discutidos
   - ✅ Decisões tomadas
   - ❓ Perguntas que ficaram sem resposta
   - 🔗 Links e mídias compartilhados (mencione o contexto)
   - 📌 Menções importantes (se alguém foi diretamente chamado para algo)
3. Não inclua fofocas ou conversas triviais (bom dia, figurinhas, etc.), a menos que dominem a conversa — nesse caso, mencione brevemente.
4. Use no máximo 300 palavras por grupo.
5. Se houver poucas mensagens (menos de 5), diga apenas o tema geral em uma frase.

Grupo: $groupName
Data: $date
Total de mensagens: $count

Mensagens:
$formattedMessages
        """.trimIndent()
    }

    private fun formatMessages(messages: List<CapturedMessage>): String {
        val zone = ZoneId.systemDefault()
        val sb = StringBuilder()
        var tokenEstimate = 0
        val maxTokens = 3500

        for (message in messages) {
            val time = Instant.ofEpochMilli(message.timestamp)
                .atZone(zone)
                .format(timeFormatter)

            val line = when (message.messageType) {
                "text" -> "[$time] ${message.author}: ${message.text}"
                "image" -> "[$time] ${message.author}: \uD83D\uDCF7 Foto"
                "video" -> "[$time] ${message.author}: \uD83D\uDCF9 Vídeo"
                "audio" -> "[$time] ${message.author}: \uD83C\uDFA4 Áudio (conteúdo não disponível)"
                "document" -> "[$time] ${message.author}: \uD83D\uDCC4 Documento"
                "sticker" -> "[$time] ${message.author}: Figurinha"
                "gif" -> "[$time] ${message.author}: GIF"
                "location" -> "[$time] ${message.author}: \uD83D\uDCCD Localização"
                "contact" -> "[$time] ${message.author}: Cartão de contato"
                else -> "[$time] ${message.author}: ${message.text}"
            }

            // Rough token estimate: ~4 chars per token
            val lineTokens = line.length / 4
            if (tokenEstimate + lineTokens > maxTokens) {
                val remaining = messages.size - messages.indexOf(message)
                sb.appendLine("[... $remaining mensagens anteriores omitidas ...]")
                break
            }

            sb.appendLine(line)
            tokenEstimate += lineTokens
        }

        return sb.toString().trimEnd()
    }
}
