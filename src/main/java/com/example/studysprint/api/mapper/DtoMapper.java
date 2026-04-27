package com.example.studysprint.api.mapper;

import com.example.studysprint.api.dto.ChapitreDto;
import com.example.studysprint.api.dto.MatiereDto;
import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.modules.matieres.models.Matiere;

public class DtoMapper {
    public static MatiereDto toDto(Matiere m) {
        MatiereDto dto = new MatiereDto();
        dto.id = m.getId();
        dto.name = m.getName();
        dto.code = m.getCode();
        dto.description = m.getDescription();
        return dto;
    }

    public static ChapitreDto toDto(Chapitre c) {
        ChapitreDto dto = new ChapitreDto();
        dto.id = c.getId();
        dto.title = c.getTitle();
        dto.orderNo = c.getOrderNo();
        dto.summary = c.getSummary();
        dto.content = c.getContent();
        dto.attachmentUrl = c.getAttachmentUrl();
        dto.aiSummary = c.getAiSummary();
        dto.aiKeyPoint = c.getAiKeyPoint();
        dto.aiTags = c.getAiTags();
        dto.subjectId = c.getSubjectId();
        return dto;
    }
}