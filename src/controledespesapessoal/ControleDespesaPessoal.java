package controledespesapessoal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ControleDespesaPessoal {

    private static final int PORTA = 8080;
    private static final DespesaDAO dao = new DespesaDAO();
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        try {
            Conexao.criarTabelaSeNaoExistir();

            HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);
            servidor.createContext("/", new PaginaInicialHandler());
            servidor.createContext("/cadastrar", new CadastroHandler());
            servidor.createContext("/editar", new EditarHandler());
            servidor.createContext("/excluir", new ExcluirHandler());
            servidor.createContext("/pagar", new PagarHandler());

            servidor.setExecutor(null);
            servidor.start();

            System.out.println("=================================================");
            System.out.println("SISTEMA ONLINE! ACESSE: http://localhost:" + PORTA);
            System.out.println("=================================================");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }

    // PÁGINA INICIAL COM SUPORTE AOS FILTROS
    static class PaginaInicialHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            try {
                String html = new String(Files.readAllBytes(Paths.get("index.html")), StandardCharsets.UTF_8);
                List<Despesa> lista = dao.listarTodas();

                double totalReceita = 0.0;
                double totalDespesa = 0.0;
                double totalPago = 0.0;
                double totalPendente = 0.0;
                StringBuilder linhas = new StringBuilder();

                for (Despesa d : lista) {
                    String tipo = d.getTipo() != null ? d.getTipo().toLowerCase() : "despesa";
                    String categoria = d.getCategoria() != null ? d.getCategoria().toLowerCase() : "fixa";
                    boolean isPago = (d.getDataPagamento() != null);

                    if ("receita".equals(tipo)) {
                        totalReceita += d.getValor();
                    } else {
                        totalDespesa += d.getValor();
                        if (isPago) {
                            totalPago += d.getValor();
                        } else {
                            totalPendente += d.getValor();
                        }
                    }

                    String classeLinha = tipo + "-row";
                    if (isPago && !"receita".equals(tipo)) {
                        classeLinha = "pago-row";
                    }

                    // Extrai mês e ano para alimentar o filtro do HTML
                    String mesStr = String.format("%02d", d.getDataVencimento().getMonthValue());
                    String anoStr = String.valueOf(d.getDataVencimento().getYear());

                    linhas.append("<tr class=\"").append(classeLinha).append("\" ")
                          .append("data-pago=\"").append(isPago).append("\" ")
                          .append("data-tipo=\"").append(tipo).append("\" ")
                          .append("data-valor=\"").append(d.getValor()).append("\" ")
                          .append("data-mes=\"").append(mesStr).append("\" ")
                          .append("data-ano=\"").append(anoStr).append("\">");

                    linhas.append("<td>").append(d.getDescricao()).append("</td>");

                    // Badge Tipo
                    linhas.append("<td><span class=\"badge-tipo ").append(tipo).append("\">")
                          .append(tipo.substring(0, 1).toUpperCase()).append(tipo.substring(1))
                          .append("</span></td>");

                    // Badge Categoria
                    linhas.append("<td><span class=\"badge-cat ").append(categoria).append("\">")
                          .append(categoria.substring(0, 1).toUpperCase()).append(categoria.substring(1))
                          .append("</span></td>");

                    // Valor formatado
                    String classeValor = "receita".equals(tipo) ? "valor-positivo" : "valor-negativo";
                    linhas.append("<td class=\"").append(classeValor).append("\">")
                          .append(MOEDA.format(d.getValor())).append("</td>");

                    // Vencimento
                    linhas.append("<td>").append(d.getDataVencimento().format(DATA_FMT)).append("</td>");

                    // Pagamento interativo na tabela
                    linhas.append("<td>");
                    if (isPago) {
                        linhas.append("<form action=\"/pagar\" method=\"POST\" style=\"display:inline-flex; align-items:center;\">")
                              .append("<input type=\"hidden\" name=\"id\" value=\"").append(d.getId()).append("\">")
                              .append("<input type=\"hidden\" name=\"remover\" value=\"true\">")
                              .append("<span style=\"color:#10b981; font-weight:600; font-size:12px; margin-right:6px;\">")
                              .append("<i class=\"fas fa-check\"></i> ").append(d.getDataPagamento().format(DATA_FMT)).append("</span>")
                              .append("<button type=\"submit\" title=\"Desfazer pagamento\" style=\"background:none; border:none; color:#f43f5e; cursor:pointer; font-size:11px;\"><i class=\"fas fa-times\"></i></button>")
                              .append("</form>");
                    } else {
                        linhas.append("<form action=\"/pagar\" method=\"POST\" style=\"display:inline-flex; align-items:center;\">")
                              .append("<input type=\"hidden\" name=\"id\" value=\"").append(d.getId()).append("\">")
                              .append("<input type=\"date\" name=\"dataPagamento\" class=\"input-inline-date\" value=\"").append(LocalDate.now()).append("\">")
                              .append("<button type=\"submit\" class=\"btn-inline-check\" title=\"Confirmar Pagamento\"><i class=\"fas fa-check\"></i> Pagar</button>")
                              .append("</form>");
                    }
                    linhas.append("</td>");

                    // Ações (Editar / Excluir)
                    String dtPagStr = d.getDataPagamento() != null ? d.getDataPagamento().toString() : "";
                    linhas.append("<td style=\"text-align: center; white-space: nowrap;\">");
                    linhas.append("<button class=\"btn-acao btn-edit\" onclick=\"abrirEditar(")
                          .append(d.getId()).append(", '")
                          .append(d.getDescricao().replace("'", "\\'")).append("', '")
                          .append(tipo).append("', '")
                          .append(categoria).append("', ")
                          .append(d.getValor()).append(", '")
                          .append(d.getDataVencimento()).append("', '")
                          .append(dtPagStr).append("')\" title=\"Editar\"><i class=\"fas fa-pen\"></i></button> ");

                    linhas.append("<form action=\"/excluir\" method=\"POST\" style=\"display:inline;\" onsubmit=\"return confirm('Deseja realmente excluir este lançamento?');\">")
                          .append("<input type=\"hidden\" name=\"id\" value=\"").append(d.getId()).append("\">")
                          .append("<button type=\"submit\" class=\"btn-acao btn-del\" title=\"Excluir\"><i class=\"fas fa-trash-alt\"></i></button>")
                          .append("</form>");
                    linhas.append("</td>");

                    linhas.append("</tr>");
                }

                double saldo = totalReceita - totalDespesa;
                html = html.replace("{{TOTAL_RECEITA}}", MOEDA.format(totalReceita))
                           .replace("{{TOTAL_DESPESA}}", MOEDA.format(totalDespesa))
                           .replace("{{SALDO}}", MOEDA.format(saldo))
                           .replace("{{TOTAL_PAGO}}", MOEDA.format(totalPago))
                           .replace("{{TOTAL_PENDENTE}}", MOEDA.format(totalPendente))
                           .replace("{{LINHAS_TABELA}}", linhas.toString());

                byte[] resposta = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, resposta.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resposta);
                    os.flush();
                }

            } catch (IOException e) {
                // Fechamento de conexão ignorado
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // CADASTRO COM SUPORTE A PARCELAS (AUTOMATIZAÇÃO)
    static class CadastroHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> dados = extrairParams(exchange);
                try {
                    String descBase = URLDecoder.decode(dados.getOrDefault("descricao", ""), StandardCharsets.UTF_8);
                    String tipo = dados.get("tipo");
                    String categoria = dados.get("categoria");
                    double valor = Double.parseDouble(dados.getOrDefault("valor", "0").replace(",", "."));
                    LocalDate dataVencBase = LocalDate.parse(dados.get("dataVencimento"));

                    String parcelasStr = dados.getOrDefault("parcelas", "1");
                    int totalParcelas = 1;
                    try {
                        totalParcelas = Integer.parseInt(parcelasStr);
                        if (totalParcelas < 1) totalParcelas = 1;
                    } catch (NumberFormatException e) {
                        totalParcelas = 1;
                    }

                    String dataPag = dados.get("dataPagamento");
                    LocalDate dataPagamento = (dataPag != null && !dataPag.trim().isEmpty()) 
                            ? LocalDate.parse(dataPag) 
                            : null;

                    if (totalParcelas == 1) {
                        Despesa d = new Despesa(descBase, tipo, categoria, valor, dataVencBase, dataPagamento);
                        dao.inserir(d);
                    } else {
                        for (int i = 1; i <= totalParcelas; i++) {
                            String descParcelada = String.format("%s (%02d/%02d)", descBase, i, totalParcelas);
                            LocalDate vencimentoParcela = dataVencBase.plusMonths(i - 1);
                            LocalDate pagParcela = (i == 1) ? dataPagamento : null;

                            Despesa d = new Despesa(descParcelada, tipo, categoria, valor, vencimentoParcela, pagParcela);
                            dao.inserir(d);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Erro ao cadastrar: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            redirecionar(exchange, "/");
        }
    }

    // EDIÇÃO
    static class EditarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> dados = extrairParams(exchange);
                try {
                    Despesa d = new Despesa();
                    d.setId(Integer.parseInt(dados.get("id")));
                    d.setDescricao(URLDecoder.decode(dados.getOrDefault("descricao", ""), StandardCharsets.UTF_8));
                    d.setTipo(dados.get("tipo"));
                    d.setCategoria(dados.get("categoria"));
                    d.setValor(Double.parseDouble(dados.getOrDefault("valor", "0").replace(",", ".")));
                    d.setDataVencimento(LocalDate.parse(dados.get("dataVencimento")));

                    String dataPag = dados.get("dataPagamento");
                    if (dataPag != null && !dataPag.trim().isEmpty()) {
                        d.setDataPagamento(LocalDate.parse(dataPag));
                    }
                    dao.atualizar(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            redirecionar(exchange, "/");
        }
    }

    // CONFIRMAR OU DESFAZER PAGAMENTO NA TABELA
    static class PagarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> dados = extrairParams(exchange);
                try {
                    int id = Integer.parseInt(dados.get("id"));
                    if ("true".equals(dados.get("remover"))) {
                        dao.atualizarPagamento(id, null);
                    } else {
                        String data = dados.get("dataPagamento");
                        Date dt = (data != null && !data.isEmpty()) ? Date.valueOf(data) : Date.valueOf(LocalDate.now());
                        dao.atualizarPagamento(id, dt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            redirecionar(exchange, "/");
        }
    }

    // EXCLUSÃO
    static class ExcluirHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> dados = extrairParams(exchange);
                try {
                    int id = Integer.parseInt(dados.get("id"));
                    dao.excluir(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            redirecionar(exchange, "/");
        }
    }

    // UTILITÁRIOS
    private static Map<String, String> extrairParams(HttpExchange exchange) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String linha;
        while ((linha = br.readLine()) != null) {
            sb.append(linha);
        }

        Map<String, String> mapa = new HashMap<>();
        for (String par : sb.toString().split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) {
                mapa.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                mapa.put(kv[0], "");
            }
        }
        return mapa;
    }

    private static void redirecionar(HttpExchange exchange, String rota) throws IOException {
        try {
            exchange.getResponseHeaders().set("Location", rota);
            exchange.sendResponseHeaders(303, -1);
        } catch (IOException e) {
            // Conexão encerrada pelo cliente
        }
    }
}
