package br.com.chamada.estudante.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.chamada.estudante.model.EstudanteModel;


@Repository
public interface TagRepository extends JpaRepository<EstudanteModel, Long> {

    Optional<EstudanteModel> findByUid(String taguid);
}


