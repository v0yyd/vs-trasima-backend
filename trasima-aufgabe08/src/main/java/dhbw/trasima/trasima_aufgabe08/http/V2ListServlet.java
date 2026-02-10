package dhbw.trasima.trasima_aufgabe08.http;

import dhbw.trasima.trasima_aufgabe07.Empty;
import dhbw.trasima.trasima_aufgabe07.TrasimaServiceGrpc;
import dhbw.trasima.trasima_aufgabe07.V2List;
import dhbw.trasima.trasima_aufgabe07.V2State;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

public final class V2ListServlet extends HttpServlet {

    private ManagedChannel channel;
    private TrasimaServiceGrpc.TrasimaServiceBlockingStub blockingStub;
    private String grpcTarget;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String grpcHost = config.getInitParameter("grpcHost");
        if (grpcHost == null || grpcHost.isBlank()) {
            grpcHost = "localhost";
        }
        int grpcPort = 50051;
        String grpcPortRaw = config.getInitParameter("grpcPort");
        if (grpcPortRaw != null && !grpcPortRaw.isBlank()) {
            grpcPort = Integer.parseInt(grpcPortRaw);
        }

        this.grpcTarget = grpcHost + ":" + grpcPort;
        this.channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext().build();
        this.blockingStub = TrasimaServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void destroy() {
        if (channel != null) {
            channel.shutdownNow();
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");

        final V2List list;
        try {
            list = blockingStub.fetchAll(Empty.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html lang=\"de\"><head><meta charset=\"utf-8\"><title>Trasima V2 Liste</title></head><body>");
                out.println("<h1>Fehler</h1>");
                out.println("<p>Fehler beim Abruf via gRPC (FetchAll) von <code>" + grpcTarget + "</code>: <code>" + e.getStatus() + "</code></p>");
                out.println("</body></html>");
            }
            return;
        }

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang=\"de\">");
            out.println("<head>");
            out.println("<meta charset=\"utf-8\">");
            out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            out.println("<title>Trasima – V2 Übersicht</title>");
            out.println("<style>");
            out.println("  :root{--bg:#ffffff;--panel:#ffffff;--text:#111827;--muted:#6b7280;--line:#e5e7eb;--accent:#2563eb;}");
            out.println("  *{box-sizing:border-box} body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:var(--bg);color:var(--text);}");
            out.println("  .wrap{max-width:980px;margin:40px auto;padding:0 16px} .card{background:var(--panel);border:1px solid var(--line);border-radius:14px;box-shadow:0 12px 40px rgba(17,24,39,.10);overflow:hidden}");
            out.println("  header{display:flex;align-items:baseline;justify-content:space-between;gap:12px;padding:18px 18px 10px}");
            out.println("  h1{margin:0;font-size:20px;letter-spacing:.2px} .meta{color:var(--muted);font-size:13px}");
            out.println("  .actions{padding:0 18px 14px} a{color:var(--accent);text-decoration:none} a:hover{text-decoration:underline}");
            out.println("  table{width:100%;border-collapse:collapse} th,td{padding:12px 14px;border-top:1px solid var(--line);text-align:left;font-variant-numeric:tabular-nums}");
            out.println("  th{color:var(--muted);font-weight:600;background:#f9fafb} tr:nth-child(even) td{background:#fcfdff}");
            out.println("  .empty{padding:18px;border-top:1px solid var(--line);color:var(--muted)} code{background:rgba(37,99,235,.08);padding:2px 6px;border-radius:6px}");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class=\"wrap\">");
            out.println("<div class=\"card\">");
            out.println("<header>");
            out.println("<h1>V2 Übersicht</h1>");
            out.println("<div class=\"meta\">gRPC: <code>" + grpcTarget + "</code> · Einträge: <code>" + list.getStatesCount() + "</code></div>");
            out.println("</header>");
            out.println("<div class=\"actions\"><a href=\"\">Neu laden</a></div>");

            if (list.getStatesCount() == 0) {
                out.println("<div class=\"empty\">Keine V2-Daten vorhanden (Simulation/Publisher läuft evtl. noch nicht).</div>");
            } else {
                out.println("<table>");
                out.println("<thead><tr><th>ID</th><th>X</th><th>Y</th><th>Speed</th></tr></thead>");
                out.println("<tbody>");
                list.getStatesList().stream()
                        .sorted(Comparator.comparingInt(V2State::getId))
                        .forEach(state -> out.println("<tr><td>" + state.getId()
                                + "</td><td>" + state.getX()
                                + "</td><td>" + state.getY()
                                + "</td><td>" + state.getSpeed()
                                + "</td></tr>"));
                out.println("</tbody>");
                out.println("</table>");
            }

            out.println("</div>");
            out.println("</div>");
            out.println("</body></html>");
        }
    }
}
