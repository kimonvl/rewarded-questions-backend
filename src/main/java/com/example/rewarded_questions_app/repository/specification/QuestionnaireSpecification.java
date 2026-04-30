package com.example.rewarded_questions_app.repository.specification;

import com.example.rewarded_questions_app.dto.request.QuestionnaireFilters;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import org.springframework.data.jpa.domain.Specification;

public class QuestionnaireSpecification {

    public static Specification<Questionnaire> build(QuestionnaireFilters filters) {
        if (filters == null) {
            return isNotDeleted();
        }

        return Specification.allOf(
                hasTitle(filters.getTitle()),
                hasBusinessName(filters.getBusinessName()),
                isNotDeleted()
        );
    }

    private static Specification<Questionnaire> hasTitle(String title) {
        return (root, query, cb) -> isBlank(title) ? cb.conjunction() :
                cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
    }

    private static Specification<Questionnaire> hasBusinessName(String businessName) {
        return (root, query, cb) -> isBlank(businessName) ? cb.conjunction() :
                cb.like(cb.lower(root.get("user").get("organization")), "%" + businessName.trim().toLowerCase() + "%");
    }

    private static Specification<Questionnaire> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
