document.getElementById("cadastroForm").addEventListener("submit", function(event) {
    event.preventDefault(); // Previne o comportamento padrão do formulário
    const nome = document.getElementById("nome").value;

    // Adiciona o novo usuário à tabela
    const tabela = document.getElementById("tabela");
    const novaLinha = tabela.insertRow(tabela.rows.length - 1); // Insere antes da última linha (a de "Nenhum usuário cadastrado ainda.")
    const novaCelula = novaLinha.insertCell(0);
    novaCelula.textContent = nome;

    // Limpa o campo de entrada
    document.getElementById("nome").value = "";
});
