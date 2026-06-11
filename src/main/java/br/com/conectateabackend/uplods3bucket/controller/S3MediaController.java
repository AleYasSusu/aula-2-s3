package br.com.conectateabackend.uplods3bucket.controller;

import br.com.conectateabackend.uplods3bucket.service.S3MediaService;
import br.com.conectateabackend.uplods3bucket.service.model.S3FileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
public class S3MediaController {

    private final S3MediaService s3MediaService;

    public S3MediaController(S3MediaService s3MediaService) {
        this.s3MediaService = s3MediaService;
    }

    @GetMapping("/arquivos")
    public ResponseEntity<List<S3FileDto>> listarArquivos() {
        return ResponseEntity.ok(s3MediaService.listarArquivos());
    }

    @PostMapping("/upload")
    public ResponseEntity<S3FileDto> uploadArquivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pasta", required = false) String pasta
    ) {
        return ResponseEntity.ok(s3MediaService.uploadArquivo(file, pasta));
    }

    @GetMapping("/metadata")
    public ResponseEntity<Map<String, String>> buscarMetadata(@RequestParam String key) {
        return ResponseEntity.ok(s3MediaService.buscarMetadata(key));
    }

    @GetMapping("/ler")
    public ResponseEntity<String> lerArquivo(@RequestParam String key) {
        return ResponseEntity.ok(s3MediaService.lerArquivo(key));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletarArquivo(@RequestParam String key) {
        s3MediaService.deletarArquivo(key);
        return ResponseEntity.noContent().build();
    }
}