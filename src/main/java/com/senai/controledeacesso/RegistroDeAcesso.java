package com.senai.controledeacesso;

public class RegistroDeAcesso {
    String horario;
    int idAcesso;

    public RegistroDeAcesso(String horario, int idAcesso) {
        this.horario = horario;
        this.idAcesso = idAcesso;
    }

    @Override
    public String toString() {
        return horario + "\t\t" + idAcesso;
    }
}
