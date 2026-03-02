package com.nexora.bank.Service;

import com.nexora.bank.Utils.AIResponseFormatter;
import com.nexora.bank.Utils.AIResponseFormatter.FormattedAnalysis;

import java.util.concurrent.CompletableFuture;

public class AIAnalysisService {

    public FormattedAnalysis getFormattedAnalysis(String rawGeminiResponse) {
        return AIResponseFormatter.parseGeminiResponse(rawGeminiResponse);
    }

    public String getPlainTextAnalysis(String rawGeminiResponse) {
        FormattedAnalysis analysis = AIResponseFormatter.parseGeminiResponse(rawGeminiResponse);
        return AIResponseFormatter.toPlainText(analysis);
    }

    public String stripMarkdown(String text) {
        return AIResponseFormatter.stripMarkdown(text);
    }

    public CompletableFuture<FormattedAnalysis> getFormattedAnalysisAsync(String rawGeminiResponse) {
        return CompletableFuture.supplyAsync(() ->
            AIResponseFormatter.parseGeminiResponse(rawGeminiResponse)
        );
    }
}
