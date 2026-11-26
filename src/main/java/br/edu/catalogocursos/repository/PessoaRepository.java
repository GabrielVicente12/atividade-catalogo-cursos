package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.Pessoa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {}
