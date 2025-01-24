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
            return "\t\t" + "\t\t" + nome + "\t\t" + telefone + "\t\t" + email + "\t\t" + imagem;
        } else {
            return "\t\t" + idAcesso + "\t\t" + nome + "\t\t" + telefone + "\t\t" + email + "\t\t" + imagem;
        }
    }
}
