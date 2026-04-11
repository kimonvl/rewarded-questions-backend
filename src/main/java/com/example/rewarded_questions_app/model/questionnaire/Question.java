package com.example.rewarded_questions_app.model.questionnaire;

import com.example.rewarded_questions_app.model.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "questions",
        indexes = {
                @Index(name = "idx_questions_questionnaires", columnList = "questionnaire_id"),
        }
)
public class Question extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @Column(nullable = false)
    private String text;

    private Boolean isFreeText;

    // TODO: check possible interaction with flyway
    @Column(columnDefinition = "BIGINT DEFAULT 1")
    private Long selectMin = 1L;

    @Column(nullable = true)
    private Long selectMax;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PossibleChoice> possibleChoices = new HashSet<>();

    public Set<PossibleChoice> getAllPossibleChoices() {
        return Collections.unmodifiableSet(possibleChoices);
    }

    public void addPossibleChoice(PossibleChoice possibleChoice) {
        if (possibleChoices == null) possibleChoices = new HashSet<>();
        possibleChoices.add(possibleChoice);
        possibleChoice.setQuestion(this);
    }

    public void removePossibleChoice(PossibleChoice possibleChoice) {
        if (possibleChoices == null) return;
        possibleChoices.remove(possibleChoice);
        possibleChoice.setQuestion(null);
    }

    // TODO: add active and inactive Questions for a questionnaire
}
