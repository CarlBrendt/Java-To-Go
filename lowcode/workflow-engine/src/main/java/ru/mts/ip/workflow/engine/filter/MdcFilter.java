package ru.mts.ip.workflow.engine.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mts.ip.workflow.engine.utility.HeaderUtils;

import java.io.IOException;

@Component
public class MdcFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
      var mdcHeaders = HeaderUtils.extractMdc(request);
      mdcHeaders.forEach(MDC::put);
      filterChain.doFilter(request, response);
  }
}
