package com.cnpjfinder.client;

import com.cnpjfinder.exception.CnpjApiException;
import com.cnpjfinder.exception.CnpjNotFoundException;
import com.cnpjfinder.model.Empresa;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class ReceitaWsClient {

    private static final String API_NAME = "ReceitaWS";
    private static final String RECEITA_WS_URL = "https://receitaws.com.br/v1/cnpj/{cnpj}";

    private final RestTemplate restTemplate;

    public ReceitaWsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<Empresa> consultarCnpj(String cnpj) {
        try {
            // A API da ReceitaWS tem um limite de 3 requisições por minuto para o plano gratuito.
            Thread.sleep(21000);

            ResponseEntity<Empresa> response = restTemplate.getForEntity(
                    RECEITA_WS_URL, Empresa.class, cnpj);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            throw new CnpjNotFoundException(cnpj);

        } catch (HttpClientErrorException.NotFound e) {
            throw new CnpjNotFoundException(cnpj);
        } catch (HttpClientErrorException e) {
            throw new CnpjApiException("Erro ao consultar o CNPJ na " + API_NAME + ". Status: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new CnpjApiException("Falha na comunicação com a " + API_NAME, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CnpjApiException("A consulta à " + API_NAME + " foi interrompida.", e);
        } catch (Exception e) {
            throw new CnpjApiException("Ocorreu um erro inesperado ao consultar a " + API_NAME, e);
        }
    }
}
