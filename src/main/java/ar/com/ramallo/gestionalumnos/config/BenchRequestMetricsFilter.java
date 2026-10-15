package ar.com.ramallo.gestionalumnos.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * Solo para el perfil {@code bench} (scripts/benchmark): registra, por request, cuántas sentencias SQL
 * ejecutó y cuánto tardó en el servidor. El script las lee de bench.log para informar el costo en consultas.
 *
 * <p>Usa el contador global de Hibernate, por lo que solo es exacto con requests de a una (el benchmark
 * es secuencial). Corre después de la cadena de Spring Security, así que no cuenta la consulta del usuario.
 * Requiere {@code hibernate.generate_statistics=true}, que ya activa el perfil.
 */
@Component
@Profile("bench")
public class BenchRequestMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BenchRequestMetricsFilter.class);

    private final Statistics statistics;

    public BenchRequestMetricsFilter(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long sentenciasAntes = statistics.getPrepareStatementCount();
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            double ms = (System.nanoTime() - inicio) / 1_000_000.0;
            long sentencias = statistics.getPrepareStatementCount() - sentenciasAntes;
            String ruta = request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            log.info("BENCH_SQL {} {} sql={} ms={}",
                    request.getMethod(), ruta, sentencias, String.format(Locale.ROOT, "%.1f", ms));
        }
    }
}
