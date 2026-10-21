package com.archmaxim.dto;

import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Season;

public record FileSearchRequest(String query, int limit, Integer year, Season season,
                                FileType type, Long departmentId, boolean mine) {
}
