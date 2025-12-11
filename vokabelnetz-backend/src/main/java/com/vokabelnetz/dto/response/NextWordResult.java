package com.vokabelnetz.dto.response;

import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.entity.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of next word selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextWordResult {

    private Word word;
    private UserWordProgress progress;
    private boolean isReview;
    private int dueCount;
}
