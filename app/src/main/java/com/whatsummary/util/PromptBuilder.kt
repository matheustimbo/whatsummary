package com.whatsummary.util

import com.whatsummary.data.db.entity.CapturedMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PromptBuilder {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Gemma 3 1B has a 2048-token context window (input + output combined).
    // We budget ~1400 tokens for the messages to leave ~600 for the template
    // overhead (instructions + headers) and ~600 for the generated summary.
    // Rough heuristic: 1 token ≈ 4 chars of Portuguese text.
    private const val MAX_INPUT_CHARS = 1400 * 4

    fun buildSummaryPrompt(
        groupName: String,
        date: String,
        messages: List<CapturedMessage>
    ): String {
        val formattedMessages = formatMessages(messages)
        val count = messages.size

        return """
Você é um assistente que resume conversas de grupos do WhatsApp.

Regras obrigatórias:
- Responda em português brasileiro.
- Seja conciso (no máximo 150 palavras).
- NÃO use asteriscos, markdown, negrito ou crases. Apenas texto simples.
- Ignore cumprimentos, figurinhas e mensagens triviais, a não ser que dominem a conversa.
- Inclua somente as seções que tiverem conteúdo real. Omita as demais.
- NÃO numere as seções. Use exatamente os títulos abaixo com seus emojis.

Seções disponíveis:
📋 Assuntos discutidos:
✅ Decisões tomadas:
❓ Perguntas sem resposta:
🔗 Links e mídias:
📌 Menções importantes:

Se houver menos de 5 mensagens úteis, responda apenas com o tema geral em uma frase, sem usar as seções.

Grupo: $groupName
Data: $date
Total de mensagens: $count

Mensagens:
$formattedMessages

Resumo:
        """.trimIndent()
    }

    private fun formatMessages(messages: List<CapturedMessage>): String {
        val zone = ZoneId.systemDefault()
        val sb = StringBuilder()
        var charsUsed = 0
        var omitted = 0

        // Preserve the chronological order (oldest → newest) but prefer to keep
        // the most recent messages if we have to truncate — summaries are more
        // useful when anchored to recent activity.
        val kept = mutableListOf<String>()
        val iterator = messages.asReversed().iterator() // newest first

        while (iterator.hasNext()) {
            val message = iterator.next()
            val time = Instant.ofEpochMilli(message.timestamp)
                .atZone(zone)
                .format(timeFormatter)

            val line = when (message.messageType) {
                "text" -> "[$time] ${message.author}: ${message.text}"
                "image" -> "[$time] ${message.author}: 📷 Foto"
                "video" -> "[$time] ${message.author}: 📹 Vídeo"
                "audio" -> "[$time] ${message.author}: 🎤 Áudio"
                "document" -> "[$time] ${message.author}: 📄 Documento"
                "sticker" -> "[$time] ${message.author}: Figurinha"
                "gif" -> "[$time] ${message.author}: GIF"
                "location" -> "[$time] ${message.author}: 📍 Localização"
                "contact" -> "[$time] ${message.author}: Cartão de contato"
                else -> "[$time] ${message.author}: ${message.text}"
            }

            if (charsUsed + line.length + 1 > MAX_INPUT_CHARS) {
                omitted = messages.size - kept.size
                break
            }
            kept.add(line)
            charsUsed += line.length + 1
        }

        // kept is newest-first; flip back to oldest-first for the prompt.
        for (line in kept.asReversed()) {
            sb.appendLine(line)
        }
        if (omitted > 0) {
            sb.insert(0, "[... $omitted mensagens mais antigas omitidas por limite de contexto ...]\n")
        }

        return sb.toString().trimEnd()
    }
}
