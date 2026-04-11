package com.example.rewarded_questions_app.model.submission;

import com.example.rewarded_questions_app.model.AbstractEntity;
import com.example.rewarded_questions_app.model.questionnaire.Questionnaire;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "submissions",
        indexes = {
                @Index(name = "idx_questionnaire_id", columnList = "questionnaire_id")
        }
)
public class Submission extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @OneToMany(mappedBy = "submission")
    private Set<Answer> answers;
}
