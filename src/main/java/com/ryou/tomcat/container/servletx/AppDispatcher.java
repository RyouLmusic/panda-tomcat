package com.ryou.tomcat.container.servletx;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class AppDispatcher implements RequestDispatcher {

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        
    }
}
