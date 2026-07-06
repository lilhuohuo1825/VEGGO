package com.veggo.app.data.mapper;

import com.veggo.app.data.remote.dto.ConsultationDto;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.model.ConsultationReply;

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
    private static final String ADMIN_DISPLAY_NAME = "VEGGO Admin";
    private static final String[] ISO_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
    };

    private ConsultationMapper() {}

    public static Consultation fromDto(ConsultationDto.QuestionDto dto) {
        List<String> likeIds = new ArrayList<>();
        if (dto.getHelpfulLikes() != null) {
            for (ConsultationDto.HelpfulLikeDto like : dto.getHelpfulLikes()) {
                if (like.getCustomerId() != null && !like.getCustomerId().isEmpty()) {
                    likeIds.add(like.getCustomerId());
                }
            }
        }
        int helpfulCount = dto.getHelpfulCount() != null ? dto.getHelpfulCount() : likeIds.size();

        String answeredBy = dto.getAnsweredBy();
        if (dto.getAnswer() != null && !dto.getAnswer().trim().isEmpty()) {
            answeredBy = ADMIN_DISPLAY_NAME;
        }

        return new Consultation(
                dto.getId(),
                dto.getQuestion(),
                dto.getCustomerName(),
                dto.getCustomerId(),
                dto.getCustomerAvatarUrl(),
                dto.getAnswer(),
                dto.getStatus(),
                formatDate(dto.getCreatedAt()),
                formatDate(dto.getAnsweredAt()),
                answeredBy,
                helpfulCount,
                likeIds,
                mapReplies(dto.getReplies())
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

    private static List<ConsultationReply> mapReplies(List<ConsultationDto.ReplyDto> replies) {
        List<ConsultationReply> mapped = new ArrayList<>();
        if (replies == null) {
            return mapped;
        }
        for (ConsultationDto.ReplyDto replyDto : replies) {
            mapped.add(new ConsultationReply(
                    replyDto.getId(),
                    replyDto.getCustomerId(),
                    replyDto.isAdmin() ? ADMIN_DISPLAY_NAME : replyDto.getCustomerName(),
                    replyDto.getCustomerAvatarUrl(),
                    replyDto.getContent(),
                    replyDto.isAdmin(),
                    formatDate(replyDto.getCreatedAt())
            ));
        }
        return mapped;
    }

    private static Date parseCreatedAt(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        for (String pattern : ISO_PATTERNS) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat(pattern, Locale.US);
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                return isoFormat.parse(isoDate);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static String formatDate(String isoDate) {
        Date date = parseCreatedAt(isoDate);
        if (date == null) {
            return isoDate != null ? isoDate : "";
        }
        return com.veggo.app.core.utils.DateFormatter.format(date);
    }
}
