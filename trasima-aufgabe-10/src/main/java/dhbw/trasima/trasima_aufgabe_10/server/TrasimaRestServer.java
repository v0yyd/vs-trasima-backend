package dhbw.trasima.trasima_aufgabe_10.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public final class TrasimaRestServer {

    public static void main(String[] args) throws Exception {
        int httpPort = intArg(args, "--httpPort", 8080);

        ResourceConfig config = new ResourceConfig();
        config.register(VehicleResource.class);
        config.register(JacksonFeature.class);

        Server server = new Server(httpPort);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        servlet.setInitOrder(0);
        context.addServlet(servlet, "/api/*");

        server.setHandler(context);
        server.start();
        System.out.println("REST Server: http://localhost:" + httpPort + "/api/trasima/vehicles");
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

