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

    // Esta é a ÚNICA associação da classe
    @ManyToOne 
    @JoinColumn(name = "estudante_id", nullable = false)
    private EstudanteModel estudante;

    // Este é um campo de dados simples, NÃO use @ManyToOne aqui
    @Column(name = "data_presenca") 
    private LocalDateTime dataPresenca = LocalDateTime.now();
}