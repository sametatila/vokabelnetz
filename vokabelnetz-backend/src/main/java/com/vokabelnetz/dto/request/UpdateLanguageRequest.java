package com.vokabelnetz.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLanguageRequest {
    private String uiLanguage;
    private String sourceLanguage;
}
