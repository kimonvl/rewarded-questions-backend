package com.example.rewarded_questions_app.model.questionnaire;

import com.example.rewarded_questions_app.model.AbstractEntity;
import com.example.rewarded_questions_app.model.user.User;
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
        name = "questionnaires",
        indexes = {
                @Index(name = "idx_questionnaires_user", columnList = "user_id"),
        }
)
public class Questionnaire extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 200)
    private String description;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(mappedBy = "questionnaire", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Question> questions = new HashSet<>();

    public Set<Question> getAllQuestions() {
        return Collections.unmodifiableSet(questions);
    }

    public void addQuestion(Question question) {
        if (questions == null) questions = new HashSet<>();
        questions.add(question);
        question.setQuestionnaire(this);
    }

    public void removeQuestion(Question question) {
        if (questions == null) return;
        questions.remove(question);
        question.setQuestionnaire(null);
    }
}
