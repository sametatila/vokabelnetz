package com.vokabelnetz.dto.request;

import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    private SessionType sessionType;
    private CefrLevel cefrLevel;
    private Integer wordCount;
}
