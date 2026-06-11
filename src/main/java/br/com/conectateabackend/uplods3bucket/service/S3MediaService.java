package br.com.conectateabackend.uplods3bucket.service;

import br.com.conectateabackend.uplods3bucket.service.model.S3FileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class S3MediaService {

    private final S3Client s3Client;

    private static final String BUCKET =
            "amzn-meu-primeiro-bucket-495680546949-us-east-2-an";

    private static final String REGION = "US_EAST_2";

    public S3MediaService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Lista os objetos do bucket com paginação
     */
    public List<S3FileDto> listarArquivos() {
        List<S3FileDto> arquivos = new ArrayList<>();

        String continuationToken = null;

        do {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(BUCKET)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            response.contents().forEach(obj -> {
                String url = gerarUrlPublica(obj.key());

                arquivos.add(new S3FileDto(
                        obj.key(),
                        url,
                        obj.size()
                ));
            });

            continuationToken = response.nextContinuationToken();

        } while (continuationToken != null);

        return arquivos;
    }

    /**
     * Faz upload de um arquivo para o S3
     */
    public S3FileDto uploadArquivo(MultipartFile file, String pasta) {
        try {
            String nomeArquivo = System.currentTimeMillis() + "-" + file.getOriginalFilename();

            String key = pasta == null || pasta.isBlank()
                    ? nomeArquivo
                    : pasta + "/" + nomeArquivo;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(Map.of(
                            "original-filename", file.getOriginalFilename() == null ? "sem-nome" : file.getOriginalFilename()
                    ))
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new S3FileDto(
                    key,
                    gerarUrlPublica(key),
                    file.getSize()
            );

        } catch (IOException e) {
            throw new RuntimeException("Erro ao fazer upload para o S3: " + e.getMessage(), e);
        }
    }

    /**
     * Busca metadata de um arquivo no S3
     */
    public Map<String, String> buscarMetadata(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        HeadObjectResponse response = s3Client.headObject(request);

        return response.metadata();
    }

    /**
     * Lê o conteúdo de um arquivo texto do bucket
     */
    public String lerArquivo(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        try (
                ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s3Object, StandardCharsets.UTF_8)
                )
        ) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler arquivo do S3: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta um arquivo do S3
     */
    public void deletarArquivo(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * Gera URL pública básica do objeto
     */
    private String gerarUrlPublica(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");

        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                BUCKET,
                REGION,
                encodedKey
        );
    }
}