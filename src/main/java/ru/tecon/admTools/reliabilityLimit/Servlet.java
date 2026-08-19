package ru.tecon.admTools.reliabilityLimit;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.tecon.admTools.specificModel.ejb.CheckUserSB;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/reliabilityLimit")
public class Servlet extends HttpServlet {

    @Inject
    private Logger logger;

    @EJB
    private CheckUserSB bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> parameterMap = req.getParameterMap();
        if (parameterMap.containsKey("sessionId") && parameterMap.containsKey("formId") && parameterMap.containsKey("objectId")) {
            try {
                int ignore = Integer.parseInt(req.getParameter("formId"));

                if (bean.checkSession(req.getParameter("sessionId"))) {
                    req.getRequestDispatcher("/view/reliabilityLimit.xhtml").forward(req, resp);
                } else {
                    // Авторизуйтесь в системе
                    logger.log(Level.WARNING, "authorization error");
                    req.getRequestDispatcher("/error.html").forward(req, resp);
                }
            } catch (NumberFormatException ex) {
                // Не корректный параметр formID
                logger.log(Level.WARNING, "invalid parameter \"formId\"");
                req.getRequestDispatcher("/error.html").forward(req, resp);
            }
        } else {
            // Не хватает параметров
            logger.log(Level.WARNING, "missing parameters");
            req.getRequestDispatcher("/error.html").forward(req, resp);
        }
    }
}