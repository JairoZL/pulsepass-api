# Escribe los archivos de ArtistController. Ejecutar desde la raiz de pulsepass-api
$utf8 = New-Object System.Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force src\main\java\com\pulsepass\controller, src\test\java\com\pulsepass\controller | Out-Null
$codigo = @'
package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Único punto donde las excepciones se convierten en respuestas HTTP (CTRL-007, PRD §14 y §15)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: un request con @Valid no cumplió las anotaciones de Bean Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // putIfAbsent: si un campo falla dos validaciones, se conserva el primer mensaje
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    // 400: el cuerpo no se pudo leer como JSON (mal formado, enum inexistente, fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", Map.of());
    }

    // 404: el Service no encontró el recurso
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // 409: conflicto de unicidad
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 409: regla de negocio incumplida
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 400: un parámetro de la URL (path o query) no se pudo convertir al tipo esperado (ej. id=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", Map.of());
    }

    // 400: falta un @RequestParam obligatorio
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", Map.of());
    }

    // 404: la URL no corresponde a ningún endpoint (ej. /api/artistas). Sin este handler caería en el de 500
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found: /" + ex.getResourcePath(), Map.of());
    }

    // 500: cualquier otra excepción. Mensaje genérico: no se expone ex.getMessage() (NFR-CTRL-007)
    // La excepción completa (con stack trace) solo va al log del servidor, nunca al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) 'src\main\java\com\pulsepass\exception\GlobalExceptionHandler.java'), $codigo + "`n", $utf8)
Write-Host 'Escrito: src\main\java\com\pulsepass\exception\GlobalExceptionHandler.java' (Get-Item 'src\main\java\com\pulsepass\exception\GlobalExceptionHandler.java').Length 'bytes'
$codigo = @'
package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Contrato HTTP: PRD Controladores 8.3 (FR-CTRL-ART-001, FR-CTRL-ART-002, FR-CTRL-ART-003)
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    // Depende solo del Service (CTRL-002, NFR-CTRL-004)
    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // FR-CTRL-ART-003. Ruta literal: tiene prioridad sobre /{id}
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        return ResponseEntity.ok(artistService.findActiveArtists());
    }

    // FR-CTRL-ART-002. stageName viaja como query param: /api/artists/by-stage-name?stageName=Solar Beat
    // @RequestParam es obligatorio por defecto: si falta, Spring lanza MissingServletRequestParameterException (400)
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    // FR-CTRL-ART-001. Spring convierte el texto de la URL a Long; si no puede (ej. "abc"),
    // lanza MethodArgumentTypeMismatchException (400) antes de entrar al metodo
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.findById(id));
    }
}
'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) 'src\main\java\com\pulsepass\controller\ArtistController.java'), $codigo + "`n", $utf8)
Write-Host 'Escrito: src\main\java\com\pulsepass\controller\ArtistController.java' (Get-Item 'src\main\java\com\pulsepass\controller\ArtistController.java').Length 'bytes'
$codigo = @'
package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PRD Controladores 22.3. Solo capa web, ArtistService es un mock (NFR-CTRL-002, QT-CTRL-002)
@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    // Datos de V2__insert_initial_artists.sql y PRD Controladores 23.3
    private final ArtistResponse solarBeat = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
    private final ArtistResponse neonWaves = new ArtistResponse(2L, "Neon Waves", "Mexico", "Synthwave", true);

    @Test
    void findById_artistaExistente_retorna200() throws Exception { // TEST-CTRL-ART-001
        when(artistService.findById(1L)).thenReturn(solarBeat);

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.genre").value("Electronic"));

        verify(artistService).findById(1L);
    }

    @Test
    void findById_artistaInexistente_retorna404() throws Exception { // TEST-CTRL-ART-002
        when(artistService.findById(99L)).thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));

        verify(artistService).findById(99L);
    }

    @Test
    void findByStageName_retorna200() throws Exception { // TEST-CTRL-ART-003
        when(artistService.findByStageName("Solar Beat")).thenReturn(solarBeat);

        // .param() arma el query string y codifica el espacio de "Solar Beat"
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value("Colombia"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void findActiveArtists_retorna200ConLista() throws Exception { // TEST-CTRL-ART-004
        when(artistService.findActiveArtists()).thenReturn(List.of(solarBeat, neonWaves));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }

    // --- Casos extra: errores de la capa HTTP que nunca deben llegar al Service ---

    @Test
    void findById_idNoNumerico_retorna400SinLlamarAlService() throws Exception { // QT-CTRL-007
        mockMvc.perform(get("/api/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        verifyNoInteractions(artistService);
    }

    @Test
    void findByStageName_sinParametro_retorna400SinLlamarAlService() throws Exception { // QT-CTRL-007
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'stageName'"));

        verifyNoInteractions(artistService);
    }

    @Test
    void rutaInexistente_retorna404ConErrorResponse() throws Exception { // NFR-CTRL-003
        mockMvc.perform(get("/api/artistas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) 'src\test\java\com\pulsepass\controller\ArtistControllerTest.java'), $codigo + "`n", $utf8)
Write-Host 'Escrito: src\test\java\com\pulsepass\controller\ArtistControllerTest.java' (Get-Item 'src\test\java\com\pulsepass\controller\ArtistControllerTest.java').Length 'bytes'
