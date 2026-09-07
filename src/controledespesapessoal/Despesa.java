package controledespesapessoal;

import java.time.LocalDate;

public class Despesa {
    private int id;
    private String descricao;
    private String tipo;        // "receita", "despesa", "cofre"
    private String categoria;   // "fixa", "variavel"
    private double valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento; // Pode ser null se estiver pendente

    public Despesa() {}

    public Despesa(String descricao, String tipo, String categoria, double valor, LocalDate dataVencimento, LocalDate dataPagamento) {
        this.descricao = descricao;
        this.tipo = tipo;
        this.categoria = categoria;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
}