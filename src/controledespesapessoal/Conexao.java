package controledespesapessoal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

    // Configurações de acesso ao Aiven Cloud MySQL
    private static final String HOST = "mysql-2832c419-cleiton-79f7.b.aivencloud.com";
    private static final String PORTA = "15858";
    private static final String BANCO = "defaultdb";
    private static final String USUARIO = "avnadmin";
    // Cole aqui a sua senha do painel da Aiven:
    private static final String SENHA = "AVNS_Di07zr4fxNqmdIvNy2Y";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORTA + "/" + BANCO 
            + "?sslMode=REQUIRED&verifyServerCertificate=false&allowPublicKeyRetrieval=true";

    public static Connection getConexao() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    public static void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS despesas_pessoais ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "descricao VARCHAR(255) NOT NULL, "
                + "tipo ENUM('receita', 'despesa', 'cofre') NOT NULL, "
                + "categoria ENUM('fixa', 'variavel') NOT NULL, "
                + "valor DECIMAL(10, 2) NOT NULL, "
                + "data_vencimento DATE NOT NULL, "
                + "data_pagamento DATE NULL, "
                + "data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        try (Connection conn = getConexao();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
            System.out.println(">> CONEXAO COM AIVEN BEM-SUCEDIDA! <<");
            System.out.println(">> TABELA 'despesas_pessoais' VERIFICADA/CRIADA COM SUCESSO! <<");

        } catch (SQLException e) {
            System.err.println("Erro ao conectar ou criar tabela no Aiven: " + e.getMessage());
        }
    }
}