package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.Colaborador;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    boolean existsByPessoaId(Long pessoaId);
}
