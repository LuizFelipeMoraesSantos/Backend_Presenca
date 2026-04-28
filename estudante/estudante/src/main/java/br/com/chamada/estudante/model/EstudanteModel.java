package br.com.chamada.estudante.model;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "tag")
public class EstudanteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do estudante é obrigatório")
    @Size(max = 200, message = "O nome do estudante deve ter no máximo 200 caracteres")
    private String nome;

    @NotBlank(message = "O UID da tag RFID é obrigatório")
    private String uid;


    @Column(nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
