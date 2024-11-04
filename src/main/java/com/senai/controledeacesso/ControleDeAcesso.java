import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;

public class ControleDeAcesso {

    static Scanner scanner = new Scanner(System.in);
    static File arquivo = new File("src\\bancoDeDados.txt");
    static String[] cabecalho = {"ID", "IdAcesso", "Nome", "Telefone", "Email"};
    static String[][] matrizCadastro;
    public static String ultimaConsulta = "";
    static boolean modoCadastrarIdAcesso = false;
    static String brokerUrl = "tcp://localhost:1883";  // Exemplo de
    static String topico = "IoTKIT1/UID";


    public static void main(String[] args) {
        carregarDadosDoArquivo();
        ConexaoMQTT conexaoMQTT = new ConexaoMQTT(brokerUrl);
        // Iniciar uma nova thread para escutar o tópico continuamente
        new Thread(() -> {
            conexaoMQTT.assinarTopico(topico, mensagem -> {
                // Chama o método para processar a mensagem recebida
                if (!modoCadastrarIdAcesso)
                    buscarEAtualizarMatriz(mensagem);
                else {
                    cadastrarNovoIdAcesso(mensagem);
                    modoCadastrarIdAcesso=false;
                }
                textoMenuPrincipal();
            });
        }).start();

        ServidorWeb.iniciarServidorWeb(); // Inicia o servidor HTTP
        menuPrincipal();
    }

    private static void textoMenuPrincipal(){
        System.out.println("-------------------------------------------------");
            System.out.println("Escolha uma opção:");
            System.out.println("\t1- Exibir cadastro completo");
            System.out.println("\t2- Inserir novo cadastro");
            System.out.println("\t3- Atualizar cadastro por id");
            System.out.println("\t4- Deletar um cadastro por id");
            System.out.println("\t5- Associar TAG ou cartão de acesso ao usuário");
            System.out.println("\t6- Sair");
            System.out.println("-------------------------------------------------");
    }

    private static void menuPrincipal() {
        int opcao;
        do {
            textoMenuPrincipal();
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    exibirCadastro();
                    break;
                case 2:
                    inserirCadastro();
                    break;
                case 3:
                    atualizarCadastro();
                    break;
                case 4:
                    deletarCadastro();
                    break;
                case 5:
                    modoCadastrarIdAcesso = true;
                    System.out.println("Aguardando nova tag ou cartão para associar ao usuário");
                    while (modoCadastrarIdAcesso){
                        try {
                            Thread.sleep(1000);  // Dorme por 1 segundo
                        } catch (InterruptedException e) {
                            // Se interrompido, sai do loop
                            break;
                        }
                    }
                    break;
                case 6:
                    System.out.println("Fim do programa!");
                    scanner.close();
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        } while (opcao != 6);
    }

    // Função que busca e atualiza a tabela com o ID recebido
    private static void buscarEAtualizarMatriz(String idAcesso) {
        boolean encontrado = false; // Variável para verificar se o usuário foi encontrado

        // Loop para percorrer a matriz e buscar o idAcesso
        for (int i = 1; i < matrizCadastro.length; i++) { // Começa de 1 para ignorar o cabeçalho
            String idAcessoNaMatriz = matrizCadastro[i][1]; // A coluna do idAcesso é a segunda coluna (índice 1)

            // Verifica se o idAcesso da matriz corresponde ao idAcesso recebido
            if (idAcessoNaMatriz.equals(idAcesso)) {
                String nomeUsuario = matrizCadastro[i][2]; // Assume que o nome do usuário está na coluna 3
                String horario = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                ultimaConsulta = nomeUsuario + " - " + horario;
                System.out.println("Usuário encontrado: " + ultimaConsulta);
                encontrado = true; // Marca que o usuário foi encontrado
                break; // Sai do loop, pois já encontrou o usuário
            }
        }
        // Se não encontrou o usuário, imprime uma mensagem
        if (!encontrado) {
            System.out.println("Usuário com idAcesso " + idAcesso + " não encontrado.");
        }
    }

    private static void cadastrarNovoIdAcesso(String novoIdAcesso) {
        boolean encontrado = false; // Variável para verificar se o usuário foi encontrado
        // Exibe a lista de usuários para o administrador escolher
        System.out.println("Selecione um usuário para associar o idAcesso:");
        for (int i = 1; i < matrizCadastro.length; i++) { // Começa de 1 para ignorar o cabeçalho
            System.out.println(matrizCadastro[i][0] + " - " + matrizCadastro[i][2]); // Exibe idUsuario e nomeUsuario
        }

        // Pede ao administrador que escolha o ID do usuário
        System.out.print("Digite o ID do usuário para associar ao novo idAcesso: ");
        String idUsuarioEscolhido = scanner.nextLine();

        // Verifica se o ID do usuário existe na matriz
        for (int i = 1; i < matrizCadastro.length; i++) {
            if (matrizCadastro[i][0].equals(idUsuarioEscolhido)) { // Coluna 0 é o idUsuario
                matrizCadastro[i][1] = novoIdAcesso; // Atualiza a coluna 1 com o novo idAcesso
                System.out.println("idAcesso " + novoIdAcesso + " associado ao usuário " + matrizCadastro[i][2]);
                encontrado=true;
                break;
            }
        }

        // Se não encontrou o usuário, imprime uma mensagem
        if (!encontrado) {
            System.out.println("Usuário com idAcesso " + idUsuarioEscolhido + " não encontrado.");
        }
    }

    // Funções de CRUD

    private static void exibirCadastro() {
        String imprimirMatriz = "";
        for (int linhas = 0; linhas < matrizCadastro.length; linhas++) {
            for (int colunas = 0; colunas < cabecalho.length; colunas++) {
                imprimirMatriz += matrizCadastro[linhas][colunas];

                int qtdCaracteres = matrizCadastro[linhas][colunas].length();
                if (qtdCaracteres < 32 && colunas != 0) {
                    int qtdTabulacoes = qtdCaracteres / 4;
                    while (qtdTabulacoes < 7) {
                        imprimirMatriz += "\t";
                        qtdTabulacoes++;
                    }
                }
                imprimirMatriz += "\t";
            }
            imprimirMatriz += "\n";
        }

        System.out.println(imprimirMatriz);
        System.out.println("-------------------------------------------------------");
    }

    private static void inserirCadastro() {
        System.out.print("Digite a quantidade de usuarios que deseja cadastrar:");
        int qtdUsuarios = scanner.nextInt();
        scanner.nextLine();

        String[][] novaMatriz = new String[matrizCadastro.length + qtdUsuarios][cabecalho.length];

        for (int i = 0; i < matrizCadastro.length; i++) {
            novaMatriz[i] = Arrays.copyOf(matrizCadastro[i], matrizCadastro[i].length);
        }

        System.out.println("\nPreencha os dados a seguir:");
        for (int i = matrizCadastro.length; i < novaMatriz.length; i++) {
            System.out.println(cabecalho[0] + "- " + i);
            novaMatriz[i][0] = String.valueOf(i);

            for (int dados = 2; dados < cabecalho.length; dados++) {
                System.out.print(cabecalho[dados] + ": ");
                novaMatriz[i][dados] = scanner.nextLine();
            }
            System.out.println("-----------------------Inserido com sucesso------------------------");
        }
        matrizCadastro = novaMatriz;
        salvaDadosNoArquivo();
    }

    private static void atualizarCadastro() {
        exibirCadastro();
        System.out.println("Escolha um id para atualizar o cadastro:");
        int idUsuario = scanner.nextInt();
        scanner.nextLine();
        System.out.println("\nAtualize os dados a seguir:");

        System.out.println(cabecalho[0] + "- " + idUsuario);
        for (int dados = 2; dados < cabecalho.length; dados++) {
            System.out.print(cabecalho[dados] + ": ");
            matrizCadastro[idUsuario][dados] = scanner.nextLine();
        }

        System.out.println("---------Atualizado com sucesso-----------");
        exibirCadastro();
        salvaDadosNoArquivo();
    }

    private static void deletarCadastro() {
        String[][] novaMatriz = new String[matrizCadastro.length - 1][cabecalho.length];

        exibirCadastro();
        System.out.println("Escolha um id para deletar o cadastro:");
        int idUsuario = scanner.nextInt();
        scanner.nextLine();

        for (int i = 0, j = 0; i < matrizCadastro.length; i++) {
            if (i == idUsuario) {
                continue;
            }
            novaMatriz[j++] = matrizCadastro[i];
        }

        matrizCadastro = novaMatriz;
        salvaDadosNoArquivo();
        System.out.println("-----------------------Deletado com sucesso------------------------");
    }

    // Funções para persistência de dados

    private static void carregarDadosDoArquivo() {
        try {
            if (!arquivo.exists()) {
                arquivo.createNewFile();
                matrizCadastro = new String[0][cabecalho.length];
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(arquivo));
            String linha;
            StringBuilder conteudo = new StringBuilder();
            while ((linha = reader.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
            reader.close();

            String[] linhas = conteudo.toString().split("\n");
            matrizCadastro = new String[linhas.length][cabecalho.length];
            matrizCadastro[0] = cabecalho;

            for (int i = 1; i < linhas.length; i++) {
                matrizCadastro[i] = linhas[i].split(",");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void salvaDadosNoArquivo() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo));
            for (String[] linha : matrizCadastro) {
                writer.write(String.join(",", linha) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
