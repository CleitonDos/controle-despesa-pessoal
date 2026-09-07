package controledespesapessoal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DespesaDAO {

    public void inserir(Despesa d) throws SQLException {
        String sql = "INSERT INTO despesas_pessoais (descricao, tipo, categoria, valor, data_vencimento, data_pagamento) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getDescricao());
            ps.setString(2, d.getTipo());
            ps.setString(3, d.getCategoria());
            ps.setDouble(4, d.getValor());
            ps.setDate(5, Date.valueOf(d.getDataVencimento()));

            if (d.getDataPagamento() != null) {
                ps.setDate(6, Date.valueOf(d.getDataPagamento()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }

            ps.executeUpdate();
        }
    }

    public void atualizar(Despesa d) throws SQLException {
        String sql = "UPDATE despesas_pessoais SET descricao=?, tipo=?, categoria=?, valor=?, data_vencimento=?, data_pagamento=? WHERE id=?";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getDescricao());
            ps.setString(2, d.getTipo());
            ps.setString(3, d.getCategoria());
            ps.setDouble(4, d.getValor());
            ps.setDate(5, Date.valueOf(d.getDataVencimento()));

            if (d.getDataPagamento() != null) {
                ps.setDate(6, Date.valueOf(d.getDataPagamento()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            ps.setInt(7, d.getId());

            ps.executeUpdate();
        }
    }

    public void atualizarPagamento(int id, Date dataPagamento) throws SQLException {
        String sql = "UPDATE despesas_pessoais SET data_pagamento = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dataPagamento != null) {
                ps.setDate(1, dataPagamento);
            } else {
                ps.setNull(1, java.sql.Types.DATE);
            }
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM despesas_pessoais WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Despesa> listarTodas() throws SQLException {
        List<Despesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM despesas_pessoais ORDER BY data_vencimento ASC";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Despesa d = new Despesa();
                d.setId(rs.getInt("id"));
                d.setDescricao(rs.getString("descricao"));
                d.setTipo(rs.getString("tipo"));
                d.setCategoria(rs.getString("categoria"));
                d.setValor(rs.getDouble("valor"));
                d.setDataVencimento(rs.getDate("data_vencimento").toLocalDate());

                Date dtPag = rs.getDate("data_pagamento");
                if (dtPag != null) {
                    d.setDataPagamento(dtPag.toLocalDate());
                }

                lista.add(d);
            }
        }
        return lista;
    }
}