package br.com.chamada.estudante.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "presencas")
public class PresencaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Muitos registros de presença para um estudante
    @JoinColumn(name = "estudante_id")
    private EstudanteModel estudante;

    private LocalDateTime dataPresenca = LocalDateTime.now();
}
