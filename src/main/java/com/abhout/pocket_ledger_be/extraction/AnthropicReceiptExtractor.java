package com.abhout.pocket_ledger_be.extraction;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.extraction.provider", havingValue = "anthropic")
public class AnthropicReceiptExtractor implements ReceiptExtractor {
    private static final String SYSTEM_PROMPT_TEMPLATE = """  
      You extract transaction data from a single financial document (a receipt, invoice, or photographed bill). This is for an Indian personal-finance app —                                 
      amounts are Indian rupees, dates may appear in day-first format.                                             
                                                                
      Rules:                                            
      - Extract only what is clearly legible. Never guess or infer a value you cannot ground in the document.                  
      - If any of date, merchant, amount, or type cannot be confidently read,                                          
        leave that field null rather than guessing.     
      - amount is always a positive number (the magnitude), never signed.                                     
      - type is "expense" for money paid out, "income" for money received.                                           
      - category should only be set if it clearly matches one of these known                                    
        categories, otherwise leave it null: %s         
      - date must be ISO 8601 (yyyy-MM-dd) if extracted.
    """;

    private final AnthropicClient client;

    public AnthropicReceiptExtractor() {
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    public ReceiptExtraction extract(
            byte[] fileBytes,
            String mimeType,
            List<String> knownCategories
    ){
        String systemPrompt =
                SYSTEM_PROMPT_TEMPLATE.formatted(String.join(", ", knownCategories));
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        ContentBlockParam fileBlock = "application/pdf".equals(mimeType)
                ? ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                .source(Base64PdfSource.builder().data(base64Data).build()).build())
                : ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder().mediaType(Base64ImageSource.MediaType.of(mimeType))
                        .data(base64Data).build()).build());

        StructuredMessageCreateParams<ReceiptExtraction> params = MessageCreateParams.builder()
                .model("claude-opus-5")
                .maxTokens(2048L)
                .outputConfig(ReceiptExtraction.class)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(systemPrompt)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build())
                ).addUserMessageOfBlockParams(List.of(
                        fileBlock,
                        ContentBlockParam.ofText(TextBlockParam.builder()
                                .text("Extract the transaction from this document.").build())))
                .build();

        var response = client.messages().create(params); // StructuredMessage<ReceiptExtraction>, not Message

        if (response.stopReason().isPresent()
                && response.stopReason().get() == StopReason.REFUSAL) {
            throw new ReceiptExtractionRefusedException();
        }

        return response.content().stream()
                .flatMap(cb -> cb.text().stream())
                .findFirst()
                .orElseThrow(ReceiptExtractionRefusedException::new).text();
    }
}
