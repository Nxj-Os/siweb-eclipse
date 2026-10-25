package pe.com.eclipsesuites.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorDTO {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private Map<String, String> detalles;
    private String path;
}
