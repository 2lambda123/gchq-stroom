package stroom.proxy.app.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.util.BuildInfoUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

public class ProxyStatusServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyStatusServlet.class);

    @Override
    public void init() throws ServletException {
        LOGGER.info("Initialising Status Servlet");
        super.init();
        LOGGER.info("Initialised Status Servlet");
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying Status Servlet");
        super.destroy();
        LOGGER.info("Destroyed Status Servlet");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        final Writer writer = response.getWriter();
        writer.write("INFO,HTTP,OK");
        writer.write("\nINFO,STROOM_PROXY,Build version ");
        writer.write(BuildInfoUtil.getBuildVersion());
        writer.write("\nINFO,STROOM_PROXY,Build date ");
        writer.write(BuildInfoUtil.getBuildDate());
        writer.write("\nINFO,STROOM_PROXY,Up date ");
        writer.write(BuildInfoUtil.getUpDate());
        writer.close();
    }
}
