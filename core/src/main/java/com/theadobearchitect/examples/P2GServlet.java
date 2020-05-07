package com.theadobearchitect.examples;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

@Component(service=Servlet.class,
        property={
                "name=" + "The Adobe Architect Examples - P2GServlet",
                Constants.SERVICE_DESCRIPTION + "= P2GServlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "sling/servlet/default",
                "sling.servlet.selectors="+ "p2g",
                "sling.servlet.extensions="+ "html"
        })
/**
 * Convert Post To Get, we can whitelist and black list paths later on
 */
public class P2GServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    final Logger LOG = LoggerFactory.getLogger(P2GServlet.class);

    @Reference
    private transient RequestResponseFactory requestResponseFactory;

    @Reference
    private transient SlingRequestProcessor slingRequestProcessor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        //LOG.info(request.getResource().getPath());
        boolean doNotContinue = false;
        /**
         * Don't Continue if we find :formstart as a parameter in post
         * :formstart is used by Forms Framework, I don't want to disturb any OOB functionality
         * Also if .error.html is invovked we want to skip processing that too as its for client error handling
         */
        if (!StringUtils.isEmpty(request.getParameter(":formstart")) || (!StringUtils.isEmpty(request.getRequestPathInfo().getSelectorString()) && request.getRequestPathInfo().getSelectorString().equalsIgnoreCase("error"))) {
            doNotContinue = true;
        }
        //DECIDE on this, this should not even happen, lets restrict Servlet with resourceType
        if (doNotContinue) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOG.warn("{},This should not have happened. Path either contains a selector .html or the form post contains variable :formstart",request.getResource() != null ? request.getResource().getPath(): "PATH NOT FOUND");
            return ;
        }
        String extension = request.getRequestPathInfo().getExtension();
        //LOG.info("Extension:{}",extension);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest httpServletRequest = requestResponseFactory.createRequest("GET",request.getResource().getPath() + "."+ (StringUtils.isEmpty(extension) ? "html" : extension),request.getParameterMap());
        WCMMode.DISABLED.toRequest(httpServletRequest);
        HttpServletResponse httpServletResponse = requestResponseFactory.createResponse(out);
        httpServletRequest.setAttribute("convertingPOSTtoGet","1");
        if (request.getParameterNames() != null) {
            Enumeration<String> enmParamNames = request.getParameterNames();
            while (enmParamNames.hasMoreElements()) {
                String paramName = enmParamNames.nextElement();
                if (request.getParameter(paramName) != null) {
                    httpServletRequest.setAttribute(paramName,request.getParameter(paramName));
                }
            }
        }
        slingRequestProcessor.processRequest(httpServletRequest,httpServletResponse,request.getResourceResolver());
        String html = out.toString();
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(html);
    }
}