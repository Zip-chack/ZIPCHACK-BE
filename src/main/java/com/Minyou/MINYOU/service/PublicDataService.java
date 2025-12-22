package com.Minyou.MINYOU.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PublicDataService {
    private static final String PUBLIC_DATA_BASE_URL = "http://apis.data.go.kr";
    private final WebClient webClient;
    private final KakaoMapService kakaoMapService;
    
    @Value("${KDATA_KEY:}")
    private String apiKey;

    public PublicDataService(KakaoMapService kakaoMapService) {
        this.kakaoMapService = kakaoMapService;
        this.webClient = WebClient.builder()
                .baseUrl(PUBLIC_DATA_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 아파트 전월세 실거래가 조회
     */
    public List<Map<String, Object>> getApartmentRentData(String lawdCd, String dealYmd) {
        // @Value 주입이 안되었을 경우를 대비해 직접 로드 시도
        if (apiKey == null || apiKey.isEmpty()) {
            try {
                Dotenv dotenv = Dotenv.load();
                apiKey = dotenv.get("KDATA_KEY");
            } catch (Exception e) {
                log.warn("Dotenv load failed, falling back to empty key");
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("공공데이터 API 키(KDATA_KEY)가 설정되지 않았습니다.");
            throw new RuntimeException("API 인증키가 없습니다. 서버 설정을 확인해주세요.");
        }

        try {
            String path = "/1613000/RTMSOBJSvc/getRTMSDataSvcAptRent";
            // 인증키가 이미 인코딩된 경우를 위해 URI 직접 생성
            String url = String.format(
                "%s%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s&numOfRows=1000&pageNo=1",
                PUBLIC_DATA_BASE_URL, path, apiKey, lawdCd, dealYmd
            );
            
            URI uri = new URI(url);
            log.info("공공데이터 호출 (날짜: {}): {}", dealYmd, PUBLIC_DATA_BASE_URL + path + "?LAWD_CD=" + lawdCd);

            String response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("API 서버 응답 오류: {}", errorBody);
                                return Mono.error(new RuntimeException("API 서버 응답 오류"));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            return parseApartmentRentXml(response);
        } catch (Exception e) {
            log.error("데이터 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("실거래가 데이터를 가져오지 못했습니다.");
        }
    }

    private List<Map<String, Object>> parseApartmentRentXml(String xmlResponse) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (xmlResponse == null || !xmlResponse.contains("<item>")) return results;

        Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
        Matcher itemMatcher = itemPattern.matcher(xmlResponse);

        while (itemMatcher.find()) {
            String itemXml = itemMatcher.group(1);
            Map<String, Object> result = new HashMap<>();
            
            result.put("apartmentName", extractTagValue(itemXml, "아파트"));
            result.put("buildYear", extractTagValue(itemXml, "건축년도"));
            result.put("roadAddress", extractTagValue(itemXml, "도로명주소"));
            result.put("jibunAddress", extractTagValue(itemXml, "지번주소"));
            result.put("floor", extractTagValue(itemXml, "층"));
            result.put("area", extractTagValue(itemXml, "전용면적"));
            result.put("deposit", extractTagValue(itemXml, "보증금").replace(",", "").trim());
            result.put("monthlyRent", extractTagValue(itemXml, "월세금액").replace(",", "").trim());
            result.put("dealDate", extractTagValue(itemXml, "년"));
            result.put("dealMonth", extractTagValue(itemXml, "월"));
            result.put("dealDay", extractTagValue(itemXml, "일"));
            
            results.add(result);
        }
        return results;
    }

    private String extractTagValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">\\s*(.*?)\\s*</" + tagName + ">");
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    public List<Map<String, Object>> getRecentApartmentRentData(String lawdCd) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return getApartmentRentData(lawdCd, currentMonth);
    }
}