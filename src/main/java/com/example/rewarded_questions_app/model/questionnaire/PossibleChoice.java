package com.example.rewarded_questions_app.model.questionnaire;

import com.example.rewarded_questions_app.model.AbstractEntity;
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
        name = "possible_choices"
//        indexes = {
//                @Index(name = "idx_properties_owner", columnList = "owner_id"),
//                @Index(name = "idx_properties_status", columnList = "status"),
//                @Index(name = "idx_properties_type", columnList = "type")
//        }
)
public class PossibleChoice extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String text;

    @Column(name = "choice_order")
    private Long order;
}
