package com.abhout.pocket_ledger_be.extraction;

import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.extraction.provider", havingValue = "gemini")
public class GeminiReceiptExtractor implements ReceiptExtractor {
    private static final String PROMPT_TEMPLATE = """
        You extract transaction data from a single financial document (a receipt,
        invoice, or photographed bill). This is for an Indian personal-finance app —
        amounts are Indian rupees, dates may appear in day-first format.

        Rules:
        - Extract only what is clearly legible. Never guess or infer a value you
          cannot ground in the document.
        - If any of date, merchant, amount, or type cannot be confidently read,
          leave that field null rather than guessing.
        - amount is always a positive number (the magnitude), never signed.
        - type is "expense" for money paid out, "income" for money received.
        - category should only be set if it clearly matches one of these known
          categories, otherwise leave it null: %s
        - date must be ISO 8601 (yyyy-MM-dd) if extracted.

        Extract the transaction from this document.
    """;
    private final Client client;
    private final ObjectMapper objectMapper;
    @Value("${app.extraction.gemini-model:gemini-2.5-flash}")
    private String modelId;

    public GeminiReceiptExtractor(ObjectMapper objectMapper) {
        this.client = new Client();
        this.objectMapper = objectMapper;
    }
    private Schema nullableString(String description) {
        return Schema.builder().type(Type.Known.STRING)
                .nullable(true).description(description).build();
    }
    @Override
    public ReceiptExtraction extract(
            byte[] content,
            String mimeType,
            List<String> knownCategories
    ) {
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(
                        ImmutableMap.of(
                                "date", nullableString(
                                        "Transaction date, ISO 8601 (yyyy-MM-dd)"),
                                "merchant", nullableString("Merchant or Payee name"),
                                "amount", Schema.builder().type(Type.Known.NUMBER).nullable(true)
                                        .description("transaction amount, always positive").build(),
                                "type", nullableString("\"expense\" or \"income\""),
                                "category", nullableString("Best match from the known categories, or null")
                        )
                ).build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(schema)
                .candidateCount(1)
                .build();

        Content requestContent = Content.fromParts(
                Part.fromText(PROMPT_TEMPLATE.formatted(String.join(", ", knownCategories))),
                Part.fromBytes(content, mimeType)
        );

        GenerateContentResponse res = client.models.generateContent(modelId, requestContent, config);

        boolean promptBlocked = res.promptFeedback().flatMap(
                GenerateContentResponsePromptFeedback::blockReason).isPresent();
        boolean candidateRefused = res.candidates().flatMap(
                list -> list.stream().findFirst()
        ).flatMap(Candidate::finishReason).map(
                reason -> reason.knownEnum() == FinishReason.Known.SAFETY)
                .orElse(false);
        if (promptBlocked || candidateRefused) {
            throw new ReceiptExtractionRefusedException();
        }

        String text = res.text();
        if(text == null || text.isBlank()){
            throw new ReceiptExtractionRefusedException();
        }

        try {
            return  objectMapper.readValue(text, ReceiptExtraction.class);
        } catch ( JacksonException e){
            throw new ReceiptExtractionRefusedException();
        }
    }
}
