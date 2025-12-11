package com.vokabelnetz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageSettingsResponse {
    private String uiLanguage;
    private String sourceLanguage;
    private String targetLanguage;
}
