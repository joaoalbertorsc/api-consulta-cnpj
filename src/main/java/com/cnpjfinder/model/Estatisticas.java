package com.cnpjfinder.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Estatisticas {
    private Long totalEmpresas;
    private Long totalConsultas;
    private List<Empresa> empresasMaisConsultadas;
}
