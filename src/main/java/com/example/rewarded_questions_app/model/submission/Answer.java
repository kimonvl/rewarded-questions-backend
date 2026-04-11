package com.example.rewarded_questions_app.model.submission;

import com.example.rewarded_questions_app.model.AbstractEntity;
import com.example.rewarded_questions_app.model.questionnaire.PossibleChoice;
import com.example.rewarded_questions_app.model.questionnaire.Question;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_submission_question_choice",
                        columnNames = {"submission_id", "question_id", "possible_choice_id"}
                )
        },
        indexes = {
                @Index(name = "idx_submission_id", columnList = "submission_id")
        }
)
public class Answer extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    private String freeText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "possible_choice_id")
    private PossibleChoice selectedChoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
}
