package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.Aluno;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    boolean existsByPessoaId(Long pessoaId);
}
