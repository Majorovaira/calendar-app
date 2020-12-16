package by.innowise.calendarapp.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomJwtAuthenticationFilter extends OncePerRequestFilter {


    private final UserDetailsService userDetailsService;

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public CustomJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwtToken = extractJwtFromRequest(httpServletRequest);
            if (StringUtils.hasText(jwtToken) && jwtTokenProvider.validateToken(jwtToken)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(jwtTokenProvider.getUsernameFromToken(jwtToken));
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(), userDetails.getPassword());

                SecurityContextHolder.getContext().setAuthentication(token);
            }
        }
        catch(ExpiredJwtException e) {
            String isRefreshToken = httpServletRequest.getHeader("isRefreshToken");
            String requestUrl = httpServletRequest.getRequestURL().toString();
            if(isRefreshToken != null && isRefreshToken.equals(true) && requestUrl.contains("refreshtoken")) {
                allowForRefreshToken(e, httpServletRequest);
            }
            else {
                httpServletRequest.setAttribute("exception ", e);
            }

        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);


    }

    private void allowForRefreshToken(ExpiredJwtException e, HttpServletRequest httpServletRequest) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                null, null);
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        httpServletRequest.setAttribute("claims", e.getClaims());

    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }
}
