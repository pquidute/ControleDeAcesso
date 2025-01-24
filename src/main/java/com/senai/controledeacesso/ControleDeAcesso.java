package com.senai.controledeacesso;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ControleDeAcesso {
    // Caminho para a pasta ControleDeAcesso no diretório do usuário
    private static final File pastaControleDeAcesso = new File(System.getProperty("user.home"), "ControleDeAcesso");

    // Caminho para o arquivo bancoDeDados.txt e para a pasta imagens
    private static final File databaseCadastros = new File(pastaControleDeAcesso, "bancoDeDados.txt");
    private static final File databaseRegistrosDeAcesso = new File(pastaControleDeAcesso, "bancoDeDados.txt");
    public static final File pastaImagens = new File(pastaControleDeAcesso, "imagens");

    static ArrayList<Cadastro> arrayCadastros;
    static ArrayList<RegistroDeAcesso> arrayRegistrosDeAcesso;
    static String cabecalho = "ID\t\tIdAcesso\t\tNome\t\tTelefone\t\tEmail\t\tImagem";
    static String cabecalhoRegistrosDeAcesso = "Horário\t\tID de Acesso";


    static volatile boolean modoCadastrarIdAcesso = false;
    static int idUsuarioRecebidoPorHTTP = 0;
    static String dispositivoRecebidoPorHTTP = "Disp1";

    static String brokerUrl = "tcp://localhost:1883";
    static String topico = "IoTKIT1/UID";

    static CLienteMQTT conexaoMQTT;
    static ServidorHTTPS servidorHTTPS;
    static Scanner scanner = new Scanner(System.in);
    static ExecutorService executorIdentificarAcessos = Executors.newFixedThreadPool(4);
    static ExecutorService executorCadastroIdAcesso = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        verificarEstruturaDeDiretorios();
        carregarDadosDoArquivo();
        conexaoMQTT = new CLienteMQTT(brokerUrl, topico, ControleDeAcesso::processarMensagemMQTTRecebida);
        servidorHTTPS = new ServidorHTTPS(); // Inicia o servidor HTTPS
        menuPrincipal();

        // Finaliza o todos os processos abertos ao sair do programa
        scanner.close();
        executorIdentificarAcessos.shutdown();
        executorCadastroIdAcesso.shutdown();
        conexaoMQTT.desconectar();
        servidorHTTPS.pararServidorHTTPS();
    }

    private static void menuPrincipal() {
        int opcao;
        do {
            String menu = """
                    _________________________________________________________
                    |   Escolha uma opção:                                  |
                    |       1- Exibir cadastro completo                     |
                    |       2- Inserir novo cadastro                        |
                    |       3- Atualizar cadastro por id                    |
                    |       4- Deletar um cadastro por id                   |
                    |       5- Listar registros de acesso                   |
                    |       6- Limpar registros de acesso                   |
                    |       7- Associar TAG ou cartão de acesso ao usuário  |
                    |       8- Sair                                         |
                    _________________________________________________________
                    """;
            System.out.println(menu);
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    exibirCadastro();
                    break;
                case 2:
                    cadastrarUsuario();
                    break;
                case 3:
                    atualizarUsuario();
                    break;
                case 4:
                    deletarUsuario();
                    break;
                case 5:
                    exibirRegistrosDeAcesso();
                    break;
                case 6:
                    deletarRegistrosDeAcesso();
                    break;
                case 7:
                    aguardarCadastroDeIdAcesso();
                    break;
                case 8:
                    System.out.println("Fim do programa!");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }

        } while (opcao != 6);
    }

    private static void aguardarCadastroDeIdAcesso() {
        modoCadastrarIdAcesso = true;
        System.out.println("Aguardando nova tag ou cartão para associar ao usuário");
        // Usar Future para aguardar até que o cadastro de ID seja concluído
        Future<?> future = executorCadastroIdAcesso.submit(() -> {
            while (modoCadastrarIdAcesso) {
                // Loop em execução enquanto o modoCadastrarIdAcesso estiver ativo
                try {
                    Thread.sleep(100); // Evita uso excessivo de CPU
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        try {
            future.get(); // Espera até que o cadastro termine
        } catch (Exception e) {
            System.err.println("Erro ao aguardar cadastro: " + e.getMessage());
        }
    }

    private static void processarMensagemMQTTRecebida(String mensagem) {
        if (!modoCadastrarIdAcesso) {
            executorIdentificarAcessos.submit(() -> criarNovoRegistroDeAcesso(Integer.parseInt(mensagem))); // Processa em thread separada
        } else {
            cadastrarNovoIdAcesso(Integer.parseInt(mensagem)); // Processa em thread separada
            modoCadastrarIdAcesso = false;
            idUsuarioRecebidoPorHTTP = 0;
        }
    }

    // Função que busca e atualiza a tabela com o ID recebido
    private static void criarNovoRegistroDeAcesso(int idAcessoRecebido) {
        boolean usuarioEncontrado = false; // Variável para verificar se o usuário foi encontrado

        for (int i = 0; i < arrayRegistrosDeAcesso.size(); i++) {
            if (arrayCadastros.get(i).idAcesso == idAcessoRecebido){
                usuarioEncontrado = true;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String horario = LocalDateTime.now().format(formatter);
                new RegistroDeAcesso(horario, idAcessoRecebido);
            }else System.out.println("ID de acesso '" + idAcessoRecebido + "' não encontrado!");
        }
    }

    private static void cadastrarNovoIdAcesso(int novoIdAcesso) {
        boolean encontrado = false; // Variável para verificar se o usuário foi encontrado
        int idUsuarioEscolhido = idUsuarioRecebidoPorHTTP;
        String dispositivoEscolhido = dispositivoRecebidoPorHTTP;

        if (idUsuarioRecebidoPorHTTP == 0) {
            // Exibe a lista de usuários para o administrador escolher
            System.out.println("ID\t\tNOME");
            for (int i = 0; i < arrayCadastros.size(); i++) {
                System.out.println(arrayCadastros.indexOf(i) + " - " + arrayCadastros.get(i).nome);
            }
            // Pede ao administrador que escolha o ID do usuário
            System.out.print("Digite o ID do usuário para associar ao novo idAcesso: ");
            idUsuarioEscolhido = scanner.nextInt();
            conexaoMQTT.publicarMensagem(topico, dispositivoEscolhido);
        }
        modoCadastrarIdAcesso = true;
        // Verifica se o ID do usuário existe na ArrayList
        for (int i = 0; i < arrayCadastros.size(); i++) {
            if (arrayCadastros.indexOf(i) == idUsuarioEscolhido){
                arrayCadastros.get(i).idAcesso = novoIdAcesso;
                System.out.println("ID de acesso " + novoIdAcesso + " associado ao usuário '" + arrayCadastros.get(i).nome + "'.");
                encontrado = true;
                salvarDadosNoArquivo(1);
                break;
            }
        }
        // Se não encontrou o usuário, imprime uma mensagem
        if (!encontrado) {
            System.out.println("Usuário com id" + idUsuarioEscolhido + " não encontrado.");
        }
    }

    // Funções de CRUD para usuários
    private static void exibirCadastro() {
        if (arrayCadastros.isEmpty()){
            System.out.println("Não há cadastros no sistema!");
        }else{
            System.out.println(cabecalho);
            for (int i = 0; i < arrayCadastros.size(); i++) {
                System.out.println(arrayCadastros.indexOf(i) + arrayCadastros.get(i).toString());
            }
        }
    }

    private static void cadastrarUsuario() {
        System.out.print("Digite a quantidade de usuarios que deseja cadastrar:");
        int qtdUsuarios = scanner.nextInt();
        scanner.nextLine();

        System.out.println("\nPreencha os dados a seguir:");
        for (int i = 0; i < qtdUsuarios; i++) {
            if (qtdUsuarios > 1){
                int usuario = i+1;
                System.out.println("-----------------------USUÁRIO " + usuario + "-----------------------");
                System.out.print("Nome: ");
                String nome = scanner.nextLine();
                System.out.print("Email: ");
                String email = scanner.nextLine();
                System.out.println("Telefone: ");
                int telefone = scanner.nextInt();
                Cadastro cadastro = new Cadastro(nome, telefone, email);
                cadastro.imagem = "-";
                System.out.println("-----------------------Cadastrado com sucesso------------------------\n");
            }
        }
        salvarDadosNoArquivo(1);
    }

    private static void atualizarUsuario() {
        exibirCadastro();
        System.out.println("Escolha um id para atualizar o cadastro:");
        int idUsuario = scanner.nextInt();
        scanner.nextLine();
        for (int i = 0; i < arrayCadastros.size(); i++) {
            if (arrayCadastros.indexOf(i) == idUsuario){
                System.out.println("\nAtualize os dados a seguir:");
                System.out.print("Nome: ");
                String nome = scanner.nextLine();
                System.out.print("Email: ");
                String email = scanner.nextLine();
                System.out.println("Telefone: ");
                int telefone = scanner.nextInt();
                arrayCadastros.get(i).nome = nome;
                arrayCadastros.get(i).telefone = telefone;
                arrayCadastros.get(i).email = email;
                System.out.println("---------Atualizado com sucesso-----------");
            }else {
                System.out.println("ID não encontrado!");
                return;
            }
        }
        exibirCadastro();
        salvarDadosNoArquivo(1);
    }

    public static void deletarUsuario() {
        int idUsuario = idUsuarioRecebidoPorHTTP;
        if (idUsuarioRecebidoPorHTTP == 0) {
            exibirCadastro();
            System.out.println("Escolha um id para deletar o cadastro:");
            idUsuario = scanner.nextInt();
            scanner.nextLine();
        }
        for (int i = 0; i < arrayCadastros.size(); i++) {
            if (arrayCadastros.indexOf(i) == idUsuario){
                arrayCadastros.remove(i);
                System.out.println("-----------------------Deletado com sucesso------------------------\n");
                exibirCadastro();
            }else {
                System.out.println("Usuário não encontrado!");
                return;
            }
        }
        salvarDadosNoArquivo(1);
        idUsuarioRecebidoPorHTTP = 0;
    }

    // Funções de CRUD para registros de acesso
    private static void exibirRegistrosDeAcesso() {
        if (arrayRegistrosDeAcesso.isEmpty()){
            System.out.println("Não há registros de acesso no sistema!");
        }else{
            System.out.println(cabecalhoRegistrosDeAcesso);
            for (int i = 0; i < arrayRegistrosDeAcesso.size(); i++) {
                System.out.println(arrayCadastros.get(i).toString());
            }
        }
    }

    public static void deletarRegistrosDeAcesso() {
        System.out.println("Você irá deletar todos os registros de acesso do sistema. Tem certeza que deseja fazer isso?\n1.Sim\n2.Não");
        int menu = scanner.nextInt();
        switch (menu){
            case 1:
                arrayRegistrosDeAcesso.clear();
                System.out.println("-----------------Registros de acesso deletados com sucesso!-----------------");
                salvarDadosNoArquivo(2);
                break;
            case 2:
                System.out.println("Operação de limpeza de dados cancelada.");
                break;
        }
    }

    // Funções para persistência de dados
    private static void carregarDadosDoArquivo() {
        try (BufferedReader reader = new BufferedReader(new FileReader(databaseCadastros))) {
        String linha;
        while ((linha = reader.readLine()) != null) {
            try {
                String[] parts = linha.trim().split(",", 4);

                int idAcesso = parts[0].equals("-") ? 0 : Integer.parseInt(parts[0]);
                String nome = parts[1];
                int telefone = Integer.parseInt(parts[2]);
                String email = parts[3];
                String imagem = parts.length > 4 ? parts[4] : "";

                Cadastro cadastro = new Cadastro(nome, telefone, email);
                cadastro.idAcesso = idAcesso;
                cadastro.imagem = imagem;

                arrayCadastros.add(cadastro);
            } catch (Exception e) {
                System.err.println("Error parsing line: " + linha + " - " + e.getMessage());
            }
        }
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
        try (BufferedReader reader = new BufferedReader(new FileReader(databaseRegistrosDeAcesso))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                try {
                    String[] parts = linha.trim().split(",", 2);

                    String horario = parts[0];
                    int idAcesso = Integer.parseInt(parts[1]);

                    RegistroDeAcesso registro = new RegistroDeAcesso(horario, idAcesso);

                    arrayRegistrosDeAcesso.add(registro);
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + linha + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void salvarDadosNoArquivo(int tipoDeDado) //parametro que indica à função o tipo de dado que será salvo (1-Cadastros; 2-Registros; 3-Ambos)
    {
        switch (tipoDeDado){
            case 1:         //Case 1 salva dados no banco de dados de cadastros
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseCadastros))) {
                    writer.write(cabecalho);
                    for (Cadastro cadastro : arrayCadastros) {
                        writer.write(cadastro.toString() + "\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 2:         //Case 2 salva dados no banco de dados de registros de acesso
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseRegistrosDeAcesso))) {
                    writer.write(cabecalhoRegistrosDeAcesso);
                    for (RegistroDeAcesso registro : arrayRegistrosDeAcesso) {
                        writer.write(registro.toString() + "\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 3:         //Case 3 salva dados em ambos os bancos de dados
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseCadastros))) {
                    writer.write(cabecalho);
                    for (Cadastro cadastro : arrayCadastros) {
                        writer.write(cadastro.toString() + "\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseRegistrosDeAcesso))) {
                    writer.write(cabecalhoRegistrosDeAcesso);
                    for (RegistroDeAcesso registro : arrayRegistrosDeAcesso) {
                        writer.write(registro.toString() + "\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private static void verificarEstruturaDeDiretorios() {
        // Verifica se a pasta ControleDeAcesso existe, caso contrário, cria
        if (!pastaControleDeAcesso.exists()) {
            if (pastaControleDeAcesso.mkdir()) {
                System.out.println("Pasta ControleDeAcesso criada com sucesso.");
            } else {
                System.out.println("Falha ao criar a pasta ControleDeAcesso.");
            }
        }

        // Verifica se o arquivo databaseCadastros.txt existe, caso contrário, cria
        if (!databaseCadastros.exists()) {
            try {
                if (databaseCadastros.createNewFile()) {
                    System.out.println("Arquivo 'databaseCadastros.txt' criado com sucesso.");
                } else {
                    System.out.println("Falha ao criar o arquivo 'databaseCadastros.txt'");
                }
            } catch (IOException e) {
                System.out.println("Erro ao criar arquivo 'databaseCadastros.txt': " + e.getMessage());
            }
        }
        if (!databaseRegistrosDeAcesso.exists()){
            try{
                if (databaseRegistrosDeAcesso.createNewFile()){
                    System.out.println("Arquivo 'databaseRegistrosDeAcesso.txt' criado com sucesso.");
                } else {
                    System.out.println("Falha ao criar o arquivo 'databaseRegistrosDeAcesso.txt'");
                }
            }catch (IOException e){
                System.out.println("Erro ao criar arquivo 'databaseRegistrosDeAcesso.txt': " + e.getMessage());
            }
        }

        // Verifica se a pasta imagens existe, caso contrário, cria
        if (!pastaImagens.exists()) {
            if (pastaImagens.mkdir()) {
                System.out.println("Pasta imagens criada com sucesso.");
            } else {
                System.out.println("Falha ao criar a pasta imagens.");
            }
        }
    }
}
