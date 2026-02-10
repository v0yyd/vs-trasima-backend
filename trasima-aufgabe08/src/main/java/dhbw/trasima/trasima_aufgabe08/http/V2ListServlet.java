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

        try (PrintWriter out = resp.getWriter()) {
            V2List list = blockingStub.fetchAll(Empty.getDefaultInstance());

            out.println("<!doctype html>");
            out.println("<html lang=\"de\"><head><meta charset=\"utf-8\"><title>Trasima V2 Liste</title></head><body>");
            out.println("<h1>V2 Liste</h1>");

            if (list.getStatesCount() == 0) {
                out.println("<p>Keine V2-Daten vorhanden.</p>");
            } else {
                out.println("<ul>");
                list.getStatesList().stream()
                        .sorted(Comparator.comparingInt(V2State::getId))
                        .forEach(state -> out.println("<li>V2 " + state.getId()
                                + ": x=" + state.getX()
                                + ", y=" + state.getY()
                                + ", speed=" + state.getSpeed()
                                + "</li>"));
                out.println("</ul>");
            }

            out.println("</body></html>");
        } catch (StatusRuntimeException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Fehler beim Abruf via gRPC (FetchAll): " + e.getStatus());
            }
        }
    }
}

