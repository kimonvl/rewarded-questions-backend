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
        name = "answers"
//        indexes = {
//                @Index(name = "idx_properties_owner", columnList = "owner_id"),
//                @Index(name = "idx_properties_status", columnList = "status"),
//                @Index(name = "idx_properties_type", columnList = "type")
//        }
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

    @ManyToMany
    @JoinTable(
            name = "answer_possible_choice",
            joinColumns = @JoinColumn(name = "answer_id"),
            inverseJoinColumns = @JoinColumn(name = "choice_id")
    )
    private Set<PossibleChoice> selectedChoices = new HashSet<>();

    public Set<PossibleChoice> getAllSelectedChoices() {
        return Collections.unmodifiableSet(selectedChoices);
    }

    public void addSelectedChoice(PossibleChoice possibleChoice) {
        if (selectedChoices == null) selectedChoices = new HashSet<>();
        selectedChoices.add(possibleChoice);
    }

    public void removeSelectedChoice(PossibleChoice possibleChoice) {
        if (selectedChoices == null) return;
        selectedChoices.remove(possibleChoice);
    }
}
