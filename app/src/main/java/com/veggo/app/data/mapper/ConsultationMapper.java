package com.veggo.app.data.mapper;

import com.veggo.app.data.remote.dto.ConsultationDto;
import com.veggo.app.domain.model.Consultation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ConsultationMapper {
    private ConsultationMapper() {}

    public static Consultation fromDto(ConsultationDto.QuestionDto dto) {
        return new Consultation(
                dto.getId(),
                dto.getQuestion(),
                dto.getCustomerName(),
                dto.getCustomerId(),
                dto.getAnswer(),
                dto.getStatus(),
                formatDate(dto.getCreatedAt()),
                formatDate(dto.getAnsweredAt()),
                dto.getAnsweredBy()
        );
    }

    public static List<Consultation> fromDto(ConsultationDto dto) {
        List<Consultation> result = new ArrayList<>();
        if (dto != null && dto.getQuestions() != null) {
            List<ConsultationDto.QuestionDto> sorted = new ArrayList<>(dto.getQuestions());
            Collections.sort(sorted, Comparator.comparing(
                    question -> parseCreatedAt(question.getCreatedAt()),
                    Comparator.nullsLast(Date::compareTo)
            ));
            for (ConsultationDto.QuestionDto questionDto : sorted) {
                result.add(fromDto(questionDto));
            }
        }
        return result;
    }

    private static Date parseCreatedAt(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.parse(isoDate);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private static String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "";
        }
        try {
            if (isoDate.contains("T")) {
                SimpleDateFormat isoFormat = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFormat.parse(isoDate);
                return com.veggo.app.core.utils.DateFormatter.format(date);
            }
        } catch (Exception ignored) {
        }
        return isoDate;
    }
}
