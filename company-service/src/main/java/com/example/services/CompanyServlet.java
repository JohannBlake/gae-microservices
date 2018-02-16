package com.example.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class CompanyServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        URL url = new URL("http://employees-dot-myapp-12345.appspot.com:8080/employees/");
        URLConnection conn = url.openConnection();

        conn.setDoOutput(true);
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
    }
}
