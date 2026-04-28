package br.com.chamada.estudante.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import br.com.chamada.estudante.model.PresencaModel;

public interface PresencaRepository extends JpaRepository<PresencaModel, Long> {
}
