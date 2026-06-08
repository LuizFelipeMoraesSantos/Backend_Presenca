package br.com.chamada.estudante.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "presencas")
public class PresencaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Associação com a tabela de estudantes através do ID
    @ManyToOne 
    @JoinColumn(name = "estudante_id", nullable = false)
    private EstudanteModel estudante;

    // Guarda data e hora exata do registro para controle de duplicatas diárias
    @Column(name = "data_presenca", nullable = false) 
    private LocalDateTime dataPresenca = LocalDateTime.now();
}