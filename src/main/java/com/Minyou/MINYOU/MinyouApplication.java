package com.Minyou.MINYOU;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableJpaAuditing
public class MinyouApplication {

	public static void main(String[] args) {
		loadEnvFile();
		SpringApplication.run(MinyouApplication.class, args);
	}

	private static void loadEnvFile() {
		Path envFile = findEnvFile();
		
		if (envFile != null && Files.exists(envFile)) {
			System.out.println("Loading .env file from: " + envFile.toAbsolutePath());
			
			try {
				// dotenv-java를 사용하여 .env 파일 로드
				Dotenv dotenv = Dotenv.configure()
						.directory(envFile.getParent().toAbsolutePath().toString())
						.filename(".env")
						.ignoreIfMissing()
						.load();
				
				// .env 파일의 모든 변수를 시스템 프로퍼티로 설정
				dotenv.entries().forEach(entry -> {
					String key = entry.getKey();
					String value = entry.getValue();
					
					// 이미 설정된 환경 변수나 시스템 프로퍼티가 있으면 덮어쓰지 않음
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
						System.out.println("Loaded environment variable: " + key + " = " + (key.contains("PASSWORD") ? "***" : value));
					} else {
						System.out.println("Skipped " + key + " (already set)");
					}
				});
				
				System.out.println("Successfully loaded .env file with " + dotenv.entries().size() + " variables");
			} catch (Exception e) {
				System.err.println("Failed to load .env file: " + e.getMessage());
				e.printStackTrace();
				
				// dotenv-java가 실패하면 직접 파일 읽기 시도
				System.out.println("Trying to load .env file manually...");
				loadEnvFileManually(envFile);
			}
		} else {
			System.out.println(".env file not found. Using default values or system environment variables.");
			System.out.println("Searched in:");
			System.out.println("  - " + Paths.get(System.getProperty("user.dir")).resolve(".env"));
			if (System.getProperty("user.dir").contains("target")) {
				System.out.println("  - " + Paths.get(System.getProperty("user.dir")).getParent().resolve(".env"));
			}
		}
	}
	
	private static void loadEnvFileManually(Path envFile) {
		try (var reader = Files.newBufferedReader(envFile)) {
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				// 주석이나 빈 줄 건너뛰기
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				
				// KEY=VALUE 형식 파싱
				int equalsIndex = line.indexOf('=');
				if (equalsIndex > 0) {
					String key = line.substring(0, equalsIndex).trim();
					String value = line.substring(equalsIndex + 1).trim();
					
					// 따옴표 제거
					if ((value.startsWith("\"") && value.endsWith("\"")) ||
						(value.startsWith("'") && value.endsWith("'"))) {
						value = value.substring(1, value.length() - 1);
					}
					
					// 환경 변수로 설정 (이미 설정된 경우 덮어쓰지 않음)
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
						System.out.println("Loaded environment variable: " + key + " = " + (key.contains("PASSWORD") ? "***" : value));
						count++;
					} else {
						System.out.println("Skipped " + key + " (already set)");
					}
				}
			}
			System.out.println("Successfully loaded .env file manually with " + count + " variables");
		} catch (Exception e) {
			System.err.println("Failed to load .env file manually: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static Path findEnvFile() {
		// 현재 작업 디렉토리
		Path currentDir = Paths.get(System.getProperty("user.dir"));
		System.out.println("Current working directory: " + currentDir.toAbsolutePath());
		
		// 1. 현재 디렉토리에서 .env 파일 찾기
		Path envFile = currentDir.resolve(".env");
		if (Files.exists(envFile)) {
			return envFile;
		}
		
		// 2. 프로젝트 루트 디렉토리 찾기 (target 폴더가 있으면 그 상위)
		if (currentDir.toString().contains("target")) {
			Path projectRoot = currentDir.getParent();
			envFile = projectRoot.resolve(".env");
			if (Files.exists(envFile)) {
				return envFile;
			}
		}
		
		// 3. 프로젝트 루트 찾기 (pom.xml이 있는 디렉토리)
		Path searchDir = currentDir;
		while (searchDir != null && Files.exists(searchDir)) {
			Path pomFile = searchDir.resolve("pom.xml");
			if (Files.exists(pomFile)) {
				envFile = searchDir.resolve(".env");
				if (Files.exists(envFile)) {
					return envFile;
				}
				break;
			}
			searchDir = searchDir.getParent();
		}
		
		// 4. src/main/resources에서도 찾기
		Path resourcesEnv = currentDir.resolve("src/main/resources/.env");
		if (Files.exists(resourcesEnv)) {
			return resourcesEnv;
		}
		
		return null;
	}
}
