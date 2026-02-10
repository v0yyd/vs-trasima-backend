package dhbw.trasima.trasima_aufgabe08.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public final class TrasimaHttpServer {

    public static void main(String[] args) throws Exception {
        int httpPort = intArg(args, "--httpPort", 8080);
        String grpcHost = stringArg(args, "--grpcHost", "localhost");
        int grpcPort = intArg(args, "--grpcPort", 50051);

        Server server = new Server(httpPort);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/http/trasima");

        ServletHolder servlet = new ServletHolder(new V2ListServlet());
        servlet.setInitParameter("grpcHost", grpcHost);
        servlet.setInitParameter("grpcPort", Integer.toString(grpcPort));
        context.addServlet(servlet, "/*");

        server.setHandler(context);

        server.start();
        System.out.println("Jetty HTTP Server: http://localhost:" + httpPort + "/http/trasima");
        System.out.println("gRPC Target: " + grpcHost + ":" + grpcPort);
        server.join();
    }

    private static int intArg(String[] args, String key, int defaultValue) {
        String value = stringArg(args, key, null);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static String stringArg(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}

