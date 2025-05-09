package victor.training.sourcing;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Component
public class PrintRequestsAsCurlFilter extends HttpFilter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest((HttpServletRequest) request);
    String body = wrappedRequest.getReader().lines().collect(Collectors.joining(""));
    Object jsonBody = new Gson().fromJson(body, Object.class);
    System.out.println(requestToCurl(wrappedRequest, jsonBody));

    super.doFilter(wrappedRequest, response, chain);
  }

  private static String requestToCurl(HttpServletRequest request,
                                      Object jsonBody) {

    StringBuilder result = new StringBuilder();

    result.append("curl --location --request ");

    // output method
    result.append(request.getMethod()).append(" ");

    // output url
    result.append("\"")
        .append(request.getRequestURL().toString())
        .append("\"");

    // output headers
    for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
      String headerName = (String) headerNames.nextElement();
      result
          .append(" -H \"")
          .append(headerName).append(": ")
          .append(request.getHeader(headerName))
          .append("\"");
    }

    // output parameters
    for (Enumeration<String> parameterNames = request.getParameterNames(); parameterNames.hasMoreElements(); ) {
      String parameterName = (String) parameterNames.nextElement();
      result.append(" -d \"")
          .append(parameterName)
          .append("=")
          .append(request.getParameter(parameterName))
          .append("\"");
    }

    // output body
    if (RequestMethod.POST.name().equalsIgnoreCase(request.getMethod()) && jsonBody != null) {
      String compactJson = new Gson().toJson(jsonBody);
      result.append(" -d '").append(compactJson).append("'");
    }

    return result.toString();
  }

  public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
      super(request);
      cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
      return new ServletInputStream() {
        @Override public int read() { return byteArrayInputStream.read(); }
        @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(ReadListener readListener) {}
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream()));
    }
  }
}
