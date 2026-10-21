package com.archmaxim.search;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseAlias;
import com.archmaxim.entity.FileRecord;
import com.archmaxim.entity.FileType;
import com.archmaxim.entity.Lecturer;
import com.archmaxim.entity.LecturerTitle;
import com.archmaxim.entity.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the text that gets embedded for a file. Types, seasons and titles are
 * written in English and Turkish so queries in either language land close to it.
 */
public final class SearchableText {

    private static final Pattern FILENAME_SEPARATORS = Pattern.compile("[_\\-.]+");

    private SearchableText() {
    }

    public static String of(FileRecord file) {
        Course course = file.getCourse();
        Lecturer lecturer = file.getUploadedBy();

        List<String> parts = new ArrayList<>();
        add(parts, course.getName());
        add(parts, course.getCode());
        add(parts, course.getDescription());
        course.getAliases().stream().map(CourseAlias::getAlias).forEach(alias -> add(parts, alias));
        add(parts, course.getDepartment().getName());
        add(parts, typeTerms(file.getType()));
        add(parts, seasonTerms(file.getSeason()));
        add(parts, file.getYear() == null ? null : file.getYear().toString());
        add(parts, titleTerms(lecturer.getTitle()));
        add(parts, lecturer.getFullName());
        add(parts, filenameWords(file.getOriginalName()));
        add(parts, file.getDescription());
        return String.join(" ", parts);
    }

    static String filenameWords(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return FILENAME_SEPARATORS.matcher(base).replaceAll(" ").trim();
    }

    private static String typeTerms(FileType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case MIDTERM -> "midterm exam vize ara sınav";
            case FINAL -> "final exam final sınavı";
            case QUIZ -> "quiz kısa sınav küçük sınav";
            case HOMEWORK -> "homework assignment ödev";
            case NOTE -> "lecture notes ders notu notlar";
            case SYLLABUS -> "syllabus izlence müfredat ders planı";
            case LAB -> "lab laboratory laboratuvar";
            case PROJECT -> "project proje";
            case OTHER -> "other diğer";
        };
    }

    private static String seasonTerms(Season season) {
        if (season == null) {
            return null;
        }
        return switch (season) {
            case FALL -> "fall autumn semester güz dönemi";
            case SPRING -> "spring semester bahar dönemi";
            case SUMMER -> "summer school yaz okulu";
        };
    }

    private static String titleTerms(LecturerTitle title) {
        if (title == null) {
            return null;
        }
        return switch (title) {
            case PROFESSOR -> "Prof. Dr.";
            case ASSOCIATE_PROFESSOR -> "Doç. Dr.";
            case ASSISTANT_PROFESSOR -> "Dr. Öğr. Üyesi";
            case LECTURER, INSTRUCTOR -> "Öğr. Gör.";
            case RESEARCH_ASSISTANT -> "Arş. Gör.";
        };
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }
}
