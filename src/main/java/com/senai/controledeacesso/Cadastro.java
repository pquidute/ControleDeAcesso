package com.senai.controledeacesso;

public class Cadastro {
    int idAcesso;
    String nome;
    int telefone;
    String email;
    String imagem;

    public Cadastro(String nome, int telefone, String email) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
    }

    @Override
    public String toString() {
        if (this.idAcesso == 0) {
            //Se o usuário não possuir id de acesso o campo 'idAcesso' printa "-"
            return "," + "," + nome + "," + telefone + "," + email + "," + imagem;
        } else {
            return "," + idAcesso + "," + nome + "," + telefone + "," + email + "," + imagem;
        }
    }
}
