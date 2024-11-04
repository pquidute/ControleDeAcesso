package com.senai.controledeacesso;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServidorWeb {
    private static final String RAIZ_WEBAPP = "C:\\Users\\rafae\\Documents\\Projetos Java\\ControleDeAcesso\\src\\main\\webapp";

    // Função que inicializa o servidor HTTP
    public static void iniciarServidorWeb() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new HomeHandler());
            server.createContext("/atualizacao", new AtualizacaoHandler());
            server.createContext("/cadastro", new CadastroHandler());
            server.setExecutor(null); // Utiliza o executor padrão
            server.start();
            System.out.println("Servidor HTTP iniciado na porta 8000");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handler para servir arquivos HTML, CSS e JS do diretório especificado
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String caminhoRequisitado = exchange.getRequestURI().getPath();
            File arquivo;

            // Define o arquivo a ser servido com base na rota
            if ("/".equals(caminhoRequisitado) || "/index.html".equals(caminhoRequisitado)) {
                arquivo = new File(RAIZ_WEBAPP + "/index.html"); // Página principal
            } else {
                arquivo = new File(RAIZ_WEBAPP + caminhoRequisitado);
            }

            // Verifica se o arquivo existe e é legível
            if (arquivo.exists() && arquivo.isFile()) {
                String mimeType = Files.probeContentType(Paths.get(arquivo.getAbsolutePath()));
                exchange.getResponseHeaders().set("Content-Type", mimeType);

                // Envia o conteúdo do arquivo na resposta
                byte[] bytesResposta = Files.readAllBytes(arquivo.toPath());
                exchange.sendResponseHeaders(200, bytesResposta.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytesResposta);
                os.close();
            } else {
                // Se o arquivo não for encontrado, retorna um erro 404
                String resposta = "Erro 404: Arquivo não encontrado";
                exchange.sendResponseHeaders(404, resposta.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resposta.getBytes());
                os.close();
            }
        }
    }

    // Handler para a rota "/atualizacao", onde o AJAX faz o fetch
    static class AtualizacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder resposta = new StringBuilder();

            // Construindo o HTML com os dados da matriz
            if (ControleDeAcesso.registrosDeAcesso.length == 0) {
                resposta.append("<tr><td colspan='2'>Nenhuma atualização ainda.</td></tr>");
            } else {
                for (String[] registro : ControleDeAcesso.registrosDeAcesso) {
                    resposta.append("<tr>")
                            .append("<td>").append(registro[0]).append("</td>") // ID
                            .append("<td>").append(registro[1]).append("</td>") // Nome
                            .append("</tr>");
                }
            }

            byte[] bytesResposta = resposta.toString().getBytes();
            exchange.sendResponseHeaders(200, bytesResposta.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytesResposta);
            os.close();
        }
    }

    // Handler para lidar com requisições POST de cadastro
    static class CadastroHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }

                // Log para visualização
                System.out.println("Dados recebidos: " + requestBody);

                // Processar JSON e retornar resposta
                String responseMessage = "Cadastro recebido com sucesso!";
                exchange.sendResponseHeaders(200, responseMessage.length());
                OutputStream os = exchange.getResponseBody();
                os.write(responseMessage.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Método não permitido
            }
        }
    }
}
