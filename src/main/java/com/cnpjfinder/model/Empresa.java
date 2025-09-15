package com.cnpjfinder.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
public class Empresa {
    @Id
    private String cnpj;

    private String razaoSocial;
    private String nomeFantasia;
    private String descricaoSituacaoCadastral;
    private String descricaoPorte;
    private String descricaoNaturezaJuridica;
    private String cnaeFiscalDescricao;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cep;
    private String uf;
    private String municipio;

    @Temporal(TemporalType.DATE)
    private Date dataAbertura;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataUltimaConsulta;

    private Integer quantidadeConsultas = 0;
}
