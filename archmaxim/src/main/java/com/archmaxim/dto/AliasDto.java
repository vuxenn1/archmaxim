package com.archmaxim.dto;

import com.archmaxim.entity.CourseAlias;

public record AliasDto(Long id, String alias) {

    public static AliasDto from(CourseAlias alias) {
        return new AliasDto(alias.getId(), alias.getAlias());
    }
}
