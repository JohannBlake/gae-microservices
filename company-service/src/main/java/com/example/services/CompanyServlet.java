package com.example.services;

import com.google.appengine.api.utils.SystemProperty;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

public class CompanyServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String EMPLOYEES_DOMAIN = "employees-dot-myapp-12345.appspot.com";
        final String EMPLOYEES_PATH = "/employees/v1";
        String serviceUrl;

        if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
            // Production
            serviceUrl = "https://" + EMPLOYEES_DOMAIN;
        } else {
            // Local development server
            // which is: SystemProperty.Environment.Value.Development
            serviceUrl = "http://" + EMPLOYEES_DOMAIN + ":8080";
        }

        URL url = new URL(serviceUrl + EMPLOYEES_PATH);
        URLConnection conn = url.openConnection();

        conn.setDoOutput(true);
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        PrintWriter out = resp.getWriter();
        out.println("Company service called");

        while ((line = reader.readLine()) != null) {
            out.println("Response from Employees service: " + line);
        }
        reader.close();
    }
}
